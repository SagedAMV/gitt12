package com.saged.githubvault.core.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.webkit.*
import com.saged.githubvault.feature.auth.*
import com.saged.githubvault.feature.github.*
import kotlinx.serialization.json.*
import java.io.File

private const val ORIGIN="https://appassets.androidplatform.net"
private fun JsonObject.str(key:String)=this[key]?.jsonPrimitive?.content?:""

/** Only packaged, same-origin main-frame HTML can invoke the bridge. No web tokens or remote pages. */
@SuppressLint("SetJavaScriptEnabled")
@Composable fun WebVault(theme:String,onTheme:(String)->Unit,vm:VaultViewModel=viewModel(),github:GitHubViewModel=viewModel()) {
 val context=LocalContext.current
 val systemDark=androidx.compose.foundation.isSystemInDarkTheme()
 val dark=theme=="dark"||(theme=="system"&&systemDark)
 SideEffect {
  (context as? android.app.Activity)?.window?.let { window ->
   val color=if(dark)android.graphics.Color.rgb(19,23,34)else android.graphics.Color.rgb(246,247,251)
   @Suppress("DEPRECATION")
   window.statusBarColor=color
   @Suppress("DEPRECATION")
   window.navigationBarColor=color
   androidx.core.view.WindowCompat.getInsetsController(window,window.decorView).apply {
    isAppearanceLightStatusBars=!dark
    isAppearanceLightNavigationBars=!dark
   }
  }
 }
 val s by vm.state.collectAsStateWithLifecycle();val g by github.state.collectAsStateWithLifecycle()
 val files by vm.local.collectAsStateWithLifecycle();val jobs by vm.transfers.collectAsStateWithLifecycle()
 var browser by remember{mutableStateOf<WebView?>(null)}
 var ready by remember{mutableStateOf(false)}
 var pageLoaded by remember{mutableStateOf(false)}
 var painted by remember{mutableStateOf(false)}
 var problem by remember{mutableStateOf<String?>(null)}
 var attempt by remember{mutableIntStateOf(0)}
 val engine=remember { runCatching { WebViewCompat.getCurrentWebViewPackage(context)?.versionName?:"unknown" }.getOrDefault("unavailable") }
 LaunchedEffect(attempt) { delay(12000);if(!painted||!ready)problem=if(!pageLoaded)"W01"else if(!ready)"W02"else "W03" }
 var pending by remember{mutableStateOf<List<Uri>?>(null)}
 var uploadPublic by remember{mutableStateOf(true)}
 var uploadFolder by remember{mutableStateOf(false)}
 var uploadTarget by remember{mutableStateOf("")}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){if(it.isNotEmpty())pending=it}
 val folderPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()){if(it!=null)pending=listOf(it)}
 val dispatch: (String)->Unit = {raw ->
  try {
   require(raw.length<=16384)
   val d=Json.parseToJsonElement(raw).jsonObject
   fun v(k:String)=d.str(k)
   fun extra()=d["extra"]?.jsonObject?:buildJsonObject{}
   when(v("action")) {
    "ready"->ready=true
    "painted"->{painted=true;problem=null}
    "webError"->{problem="W10:${v("line").take(8)}"}
    "exit"->(context as? android.app.Activity)?.finish()
    "tab"->if(v("value") in listOf("github","local","transfers","settings","home","users","admin","audit"))vm.tab(v("value"))
    "back"->if(s.tab=="github")github.back()else if(s.tab=="home"&&s.repositoryId!=null){if(s.path.isEmpty())vm.home()else vm.browse(s.repositoryId!!,s.path.substringBeforeLast('/',""))}else vm.tab("github")
    "refresh"->if(s.tab=="github"){if(g.repo==null)github.repositories()else github.open(g.repo!!,g.branch,g.path)}else if(s.tab=="local")vm.syncLocal()else vm.tab(s.tab)
    "repo"->g.repositories.find{it.str("name")==v("name")}?.let{github.open(it.str("name"),it.str("default_branch"))}
    "file"->g.files.find{it.str("path")==v("path")}?.let{if(it.str("type")=="dir")github.open(g.repo!!,g.branch,it.str("path"))else github.preview(it)}
    "download"->g.files.find{it.str("path")==v("path")}?.let(github::download)
    "more"->github.repositories(true)
    "create"->github.createRepository(v("name"))
    "token"->github.token(v("value"))
    "removeToken"->github.removeToken()
    "dismiss"->{github.clearMessage();vm.message(null)}
    "upload","folderUpload"->{uploadPublic=s.tab=="github";uploadFolder=v("action")=="folderUpload";uploadTarget=if(uploadPublic)"${g.repo}/${g.branch}/${g.path}"else "${s.repositoryId}/${s.path}";if(uploadPublic&&!g.tokenSaved)vm.message("حط توكنك أول")else if(uploadFolder)folderPicker.launch(null)else picker.launch(arrayOf("*/*"))}
    "theme"->if(v("value") in listOf("system","light","dark"))onTheme(v("value"))
    "localOpen"->files.find{it.id==v("id")}?.let{f->val file=File(f.localPath);check(file.exists());val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file);val mime=MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())?:"application/octet-stream";context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).setDataAndType(uri,mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"افتح بواسطة"))}
    "localUpdate"->files.find{it.id==v("id")}?.let(vm::updateLocal)
    "localDelete"->files.find{it.id==v("id")}?.let(vm::deleteLocal)
    "retry"->jobs.find{it.id==v("id")}?.let(vm::retry)
    "cancel"->jobs.find{it.id==v("id")}?.let(vm::cancel)
    "login"->vm.login(v("name"),v("password"))
    "register"->vm.register(v("name"),v("password"))
    "logout"->vm.logout()
    "password"->vm.changePassword(v("current"),v("password"))
    "configure"->vm.configure(v("url"),v("key"))
    "vaultRepo"->s.repositories.find{it.str("id")==v("id")}?.let{vm.browse(it.str("id"))}
    "vaultFile"->s.items.find{it.str("path")==v("path")}?.let{if(it.str("type")=="dir")vm.browse(s.repositoryId!!,it.str("path"))else vm.toggle(it.str("path"))}
    "vaultDownload"->vm.download(listOf(v("path")),v("raw")=="true")
    "selectionDownload"->if(s.selected.isNotEmpty())vm.download(s.selected.toList())
    "item"->if(s.role=="ADMIN"&&v("op") in listOf("mkdir","move","delete"))vm.manageItem(v("op"),v("path"),v("destination"))
    "user"->if(s.role=="ADMIN"&&v("op") in listOf("create","review","status","reset","grant","revoke"))vm.userAction(v("op"),v("id"),extra())
    "repoAdmin"->if(s.role=="ADMIN"&&v("op") in listOf("link","unlink","status"))vm.repoAction(v("op"),v("id"),extra())
    "available"->if(s.role=="ADMIN")vm.available()
    "audit"->if(s.role=="ADMIN")vm.audit((v("offset").toIntOrNull()?:0).coerceAtLeast(0),v("query"),v("outcome"))
   }
  }catch(e:Exception){vm.message("ما ضبطت العملية")}
 }
 val currentDispatch by rememberUpdatedState(dispatch)
 val feedback=(s.message?:g.message)?.let(SimpleCopy::feedback)
 val state=buildJsonObject {
  put("reduceMotion",!android.animation.ValueAnimator.areAnimatorsEnabled());put("tab",s.tab);put("theme",theme);put("busy",s.busy||g.busy);put("signedIn",s.signedIn);put("username",s.username);put("role",s.role);put("mustChange",s.mustChangePassword)
  put("repo",g.repo?.let(::JsonPrimitive)?:JsonNull);put("branch",g.branch);put("path",g.path);put("repositories",JsonArray(g.repositories));put("files",JsonArray(g.files));put("hasMore",g.hasMore);put("tokenSaved",g.tokenSaved)
  put("preview",g.preview?.let(::JsonPrimitive)?:JsonNull);put("previewName",g.previewName)
  put("message",feedback?.let{buildJsonObject{put("title",it.title);put("hint",it.hint);put("reference",it.reference);put("kind",it.kind)}}?:JsonNull)
  put("local",JsonArray(files.map{buildJsonObject{put("id",it.id);put("name",it.relativePath);put("updated",it.updateAvailable);put("removed",it.removedRemotely)}}))
  put("jobs",JsonArray(jobs.map{buildJsonObject{put("id",it.id);put("name",it.label);put("kind",it.kind);put("status",it.status);put("progress",it.progress);put("error",it.error?.let(SimpleCopy::message)?:"")}}))
  put("vaultRepos",JsonArray(s.repositories));put("vaultItems",JsonArray(s.items));put("vaultRepo",s.repositoryId?.let(::JsonPrimitive)?:JsonNull);put("vaultPath",s.path);put("selected",JsonArray(s.selected.map(::JsonPrimitive)))
  put("users",JsonArray(s.users));put("managed",JsonArray(s.managed));put("available",JsonArray(s.available));put("access",JsonArray(s.access));put("logs",JsonArray(s.logs));put("logOffset",s.logOffset)
 }.toString()
 LaunchedEffect(state,ready,pageLoaded,browser){
  if(ready||pageLoaded)browser?.evaluateJavascript("(function(){try{if(typeof window.receiveState!=='function')return false;window.receiveState(${JsonPrimitive(state)});return true;}catch(e){return false;}})();"){value->
   if(value=="true"&&ready){painted=true;problem=null}
  }
 }
 BackHandler {
  browser?.evaluateJavascript("window.handleBack && window.handleBack();",null)
 }
 if(pending!=null)AlertDialog(onDismissRequest={pending=null},title={Text("نرفع الملفات؟")},text={Column{Text("الموجود ممكن يتبدّل");Text(if(uploadPublic)"حدّه 90MiB"else "حدّه 8MiB");Text(uploadTarget);Text("${pending!!.size} ملفات")}},confirmButton={TextButton(onClick={val target=if(uploadPublic)"${g.repo}/${g.branch}/${g.path}"else "${s.repositoryId}/${s.path}";if(target==uploadTarget){if(uploadPublic)github.upload(pending!!)else vm.upload(pending!!,uploadFolder)}else vm.message("المسار مش مضبوط");pending=null}){Text("ارفعها")}},dismissButton={TextButton(onClick={pending=null}){Text("خلّيه")}})
 Box(Modifier.fillMaxSize().systemBarsPadding().displayCutoutPadding().imePadding()) {
 key(attempt) {
 AndroidView(modifier=Modifier.fillMaxSize(),factory={ctx->
  try { WebView(ctx).apply {
   layoutParams=android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,android.view.ViewGroup.LayoutParams.MATCH_PARENT)
   browser=this
   setBackgroundColor(android.graphics.Color.rgb(246,247,251))
   settings.javaScriptEnabled=true;settings.domStorageEnabled=false;settings.allowFileAccess=false;settings.allowContentAccess=false;settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW;settings.javaScriptCanOpenWindowsAutomatically=false;settings.setSupportMultipleWindows(false)
   val loader=WebViewAssetLoader.Builder().addPathHandler("/assets/",WebViewAssetLoader.AssetsPathHandler(ctx)).build()
   webViewClient=object:WebViewClient(){
    override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest):WebResourceResponse = loader.shouldInterceptRequest(request.url)?:WebResourceResponse("text/plain","UTF-8",403,"Blocked",emptyMap(),"".byteInputStream())
    override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean = request.url.toString()!="$ORIGIN/assets/vault.html"
    override fun onPageStarted(view:WebView,url:String,favicon:android.graphics.Bitmap?){pageLoaded=false;painted=false}
    override fun onPageFinished(view:WebView,url:String){if(url=="$ORIGIN/assets/vault.html")pageLoaded=true}
    override fun onReceivedError(view:WebView,request:WebResourceRequest,error:WebResourceError){if(request.isForMainFrame)problem="W04:${error.errorCode}"}
    override fun onReceivedHttpError(view:WebView,request:WebResourceRequest,error:WebResourceResponse){if(request.isForMainFrame)problem="W05:${error.statusCode}"}
    override fun onRenderProcessGone(view:WebView,detail:RenderProcessGoneDetail):Boolean{ready=false;painted=false;problem="W06";return true}
   }
   webChromeClient=object:WebChromeClient(){
    override fun onConsoleMessage(message:ConsoleMessage):Boolean {
     if(message.messageLevel()==ConsoleMessage.MessageLevel.ERROR&&!painted)problem="W07:${message.lineNumber()}"
     return true
    }
   }
   if(WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)){
    WebViewCompat.addWebMessageListener(this,"VaultNative",setOf(ORIGIN)){_,message,source,isMainFrame,_->if(isMainFrame&&source.scheme=="https"&&source.host=="appassets.androidplatform.net"){val raw=message.data?:return@addWebMessageListener;post{currentDispatch(raw)}}}
    loadUrl("$ORIGIN/assets/vault.html")
   }else {problem="W08"}
  }} catch(e:Exception) {problem="W09";android.widget.TextView(ctx).apply{text="Vault"}}
 },onRelease={view->(view as? WebView)?.let{runCatching{it.stopLoading();it.destroy()}};if(browser===view)browser=null})
 }
 if(!painted||problem!=null){
  Surface(modifier=Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background){
   Column(Modifier.fillMaxSize().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
    Text("GitHub Vault",style=MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(20.dp))
    if(problem==null){CircularProgressIndicator();Spacer(Modifier.height(16.dp));Text("بنفتح ملفاتك")}
    else {Text("الواجهة ما فتحت",style=MaterialTheme.typography.titleLarge);Spacer(Modifier.height(10.dp));Text("رمز: $problem");Text("WebView: $engine",style=MaterialTheme.typography.bodySmall)}
    Spacer(Modifier.height(24.dp))
    Button(onClick={ready=false;pageLoaded=false;painted=false;problem=null;attempt++},modifier=Modifier.fillMaxWidth()){Text("جرّب ثاني")}
    if(problem!=null){Spacer(Modifier.height(12.dp));Text("صوّر هذي الشاشة",style=MaterialTheme.typography.bodySmall)}
   }
  }
 }
 }
}

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class,androidx.compose.animation.ExperimentalAnimationApi::class)
package com.saged.githubvault.core.ui

import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saged.githubvault.feature.auth.*
import com.saged.githubvault.core.database.*
import kotlinx.serialization.json.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

private data class Tab(val id:String,val label:String,val icon:ImageVector)
private val tabs=listOf(Tab("github","saged7",Icons.Rounded.FolderOpen),Tab("local","عندك",Icons.Rounded.OfflinePin),Tab("transfers","الشغل",Icons.Rounded.SwapVert),Tab("settings","حسابك",Icons.Rounded.PersonOutline))
@Composable fun VaultApp(vm:VaultViewModel=viewModel(),theme:String,onTheme:(String)->Unit) {
 val githubVm:com.saged.githubvault.feature.github.GitHubViewModel=viewModel()
 val s by vm.state.collectAsStateWithLifecycle()
 val local by vm.local.collectAsStateWithLifecycle()
 val transfers by vm.transfers.collectAsStateWithLifecycle()
 val context=LocalContext.current
 BackHandler(s.tab=="home" && s.repositoryId!=null) {if(s.path.isEmpty())vm.home() else vm.browse(s.repositoryId!!,s.path.substringBeforeLast('/',""))}
 val showShell=s.tab=="github" || s.signedIn || s.tab=="local" || s.tab=="transfers" || s.tab=="settings"
 Scaffold(
  containerColor=MaterialTheme.colorScheme.background,
  snackbarHost={MotionNotice(s.message,{vm.message(null)},Modifier.padding(16.dp))},
  topBar={if(showShell)DesignHeader(onAccount={vm.tab("settings")},modifier=Modifier.statusBarsPadding())},
  bottomBar={if(showShell)DesignNavigation(s.tab,{vm.tab(it)},transfers.any{it.status in listOf("QUEUED","RUNNING","RETRYING")},modifier=Modifier.navigationBarsPadding())}

 ) {padding ->
  Column(Modifier.fillMaxSize().padding(padding)) {
   AnimatedVisibility(s.busy){LinearProgressIndicator(Modifier.fillMaxWidth())}
   if(!showShell) AuthScreen(s,vm)
   else if(s.mustChangePassword && s.tab !in listOf("local","github")) PasswordScreen(s,vm,true)
   else {
    if(s.signedIn && s.role=="ADMIN" && s.tab!="github") Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
     listOf("home" to "ملفاتي","admin" to "إدارتك","users" to "الناس","audit" to "وش صار").forEach{(id,title)->MotionFilterChip(selected=s.tab==id,onClick={vm.tab(id)},label={Text(title)})}
    }
    AnimatedContent(targetState=s.tab,transitionSpec={ (fadeIn(tween(220))+slideInHorizontally{it/12}) togetherWith (fadeOut(tween(160))+slideOutHorizontally{-it/12}) },label="screen") {tab->
     when(tab){
      "github"->com.saged.githubvault.feature.github.GitHubScreen(githubVm)
      "home"->RepositoriesScreen(s,vm)
      "local"->LocalScreen(local,vm,s.signedIn)
      "transfers"->TransfersScreen(transfers,vm)
      "users"->UsersScreen(s,vm)
      "admin"->AdminScreen(s,vm)
      "audit"->AuditScreen(s,vm)
      else->if(s.signedIn)SettingsScreen(s,vm,theme,onTheme)else PublicAccountScreen(githubVm,theme,onTheme,{vm.tab("home")})
     }
    }
   }
  }
 }
}
@Composable private fun AuthScreen(s:VaultState,vm:VaultViewModel) {
 var register by rememberSaveable{mutableStateOf(false)}
 var name by rememberSaveable{mutableStateOf("")}
 var pass by remember{mutableStateOf("")}
 var confirm by remember{mutableStateOf("")}
 var visible by remember{mutableStateOf(false)}
 var connection by remember{mutableStateOf(false)}
 if(connection)FormDialog("اضبط الاتصال",listOf("Project URL: https://…supabase.co","المفتاح العام"),onDismiss={connection=false}){v->vm.configure(v[0],v[1]);connection=false}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
  Spacer(Modifier.height(28.dp))
  val transition=rememberInfiniteTransition(label="shield")
  val float by transition.animateFloat(-4f,4f,infiniteRepeatable(tween(2400,easing=FastOutSlowInEasing),RepeatMode.Reverse),label="float")
  Box(Modifier.offset(y=float.dp).size(76.dp).background(MaterialTheme.colorScheme.primaryContainer,RoundedCornerShape(24.dp)),contentAlignment=Alignment.Center){Icon(Icons.Rounded.Shield,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(40.dp))}
  Text("يا هلا فيك",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
  Text("ملفاتك وين ماتروح",color=MaterialTheme.colorScheme.onSurfaceVariant)
  AnimatedContent(register,label="auth-title"){r->Text(if(r)"أبغى حساب" else "يا هلا",style=MaterialTheme.typography.titleLarge)}
  OutlinedTextField(name,{name=it.lowercase().take(32)},Modifier.fillMaxWidth(),label={Text("اسمك")},leadingIcon={Icon(Icons.Rounded.PersonOutline,null)},singleLine=true,shape=RoundedCornerShape(16.dp),supportingText={if(register)Text("اسمك: 3–32 حرف")})
  OutlinedTextField(pass,{pass=it.take(128)},Modifier.fillMaxWidth(),label={Text("الباسورد")},leadingIcon={Icon(Icons.Rounded.LockOpen,null)},trailingIcon={MotionIconButton(onClick={visible=!visible}){Icon(if(visible)Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,"ورّي أو خبّي")}},singleLine=true,visualTransformation=if(visible)VisualTransformation.None else PasswordVisualTransformation(),shape=RoundedCornerShape(16.dp))
  AnimatedVisibility(register){OutlinedTextField(confirm,{confirm=it},Modifier.fillMaxWidth(),label={Text("عيد الباسورد")},visualTransformation=PasswordVisualTransformation(),singleLine=true,shape=RoundedCornerShape(16.dp),supportingText={Text("الباسورد: 12+ حرف")})}
  val valid=name.matches(Regex("[a-z0-9_]{3,32}"))&&pass.isNotEmpty()&&(!register||pass.length>=12&&pass==confirm)
  MotionButton(onClick={if(register)vm.register(name,pass) else vm.login(name,pass);pass="";confirm=""},enabled=valid&&!s.busy,modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(16.dp)) {Text(if(register)"اطلب حساب" else "ادخل");Spacer(Modifier.width(10.dp));Icon(Icons.AutoMirrored.Rounded.ArrowForward,null)}
  MotionTextButton(onClick={register=!register;pass="";confirm=""},modifier=Modifier.align(Alignment.CenterHorizontally)){Text(if(register)"عندي حساب" else "أبغى حساب")}
  MotionTextButton(onClick={connection=true},modifier=Modifier.align(Alignment.CenterHorizontally)){Icon(Icons.Rounded.SettingsEthernet,null);Spacer(Modifier.width(8.dp));Text("إعداد الاتصال")}
  HorizontalDivider()
  MotionTextButton(onClick={vm.tab("github")},modifier=Modifier.align(Alignment.CenterHorizontally)){Icon(Icons.Rounded.FolderOpen,null);Spacer(Modifier.width(8.dp));Text("افتح saged7")}
  MotionTextButton(onClick={vm.tab("local")},modifier=Modifier.align(Alignment.CenterHorizontally)){Icon(Icons.Rounded.OfflinePin,null);Spacer(Modifier.width(8.dp));Text("شوف ملفاتك")}
 }
}
@Composable private fun Hero(title:String,subtitle:String,icon:ImageVector) {com.saged.githubvault.feature.github.BrowserTitle(title,subtitle)}

@Composable private fun Empty(title:String,subtitle:String,icon:ImageVector=Icons.Rounded.FolderOpen) {
 Column(Modifier.fillMaxWidth().padding(vertical=48.dp,horizontal=20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)){
  Icon(icon,null,Modifier.size(52.dp),tint=MaterialTheme.colorScheme.outline)
  Text(title,style=MaterialTheme.typography.titleMedium);Text(subtitle,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium)
 }
}
@Composable private fun Search(value:String,onChange:(String)->Unit,label:String="دوّر") {OutlinedTextField(value,onChange,Modifier.fillMaxWidth(),placeholder={Text(label)},leadingIcon={Icon(Icons.Rounded.Search,null)},singleLine=true,shape=RoundedCornerShape(18.dp))}
@Composable private fun RepositoriesScreen(s:VaultState,vm:VaultViewModel) {
 var search by rememberSaveable{mutableStateOf("")}
 var dialog by remember{mutableStateOf("")}
 var target by remember{mutableStateOf<JsonObject?>(null)}
 var confirmUpload by remember{mutableStateOf<List<Uri>?>(null)}
 var folderUpload by remember{mutableStateOf(false)}
 val filePicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){if(it.isNotEmpty()){folderUpload=false;confirmUpload=it}}
 val folderPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()){it?.let{folderUpload=true;confirmUpload=listOf(it)}}
 val repo=s.repositories.find{it.text("id")==s.repositoryId}
 if(confirmUpload!=null) AlertDialog(onDismissRequest={confirmUpload=null},title={Text("نرفعها؟")},text={Text("الموجود ممكن يتبدّل")},confirmButton={MotionTextButton(onClick={vm.upload(confirmUpload!!,folderUpload);confirmUpload=null}){Text("ارفع")}},dismissButton={MotionTextButton(onClick={confirmUpload=null}){Text("خلّيه")}})
 if(dialog=="mkdir" || dialog=="move") FormDialog(if(dialog=="mkdir")"مجلد جديد" else "غيّر مكانه واسمه",listOf("المسار الجديد"),initial=if(dialog=="mkdir")listOf(if(s.path.isEmpty())"" else s.path+"/") else listOf(target?.text("path")?:""),onDismiss={dialog=""}){v->vm.manageItem(if(dialog=="mkdir")"mkdir" else "move",if(dialog=="mkdir")v[0] else target!!.text("path"),v[0]);dialog=""}
 if(dialog=="delete") Confirm("نمسحه من GitHub؟","نسختك بتبقى هنا",onDismiss={dialog=""}){vm.manageItem("delete",target!!.text("path"));dialog=""}
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=24.dp,top=16.dp)) {
  item{Hero(if(s.repositoryId==null)"أهلاً، ${s.username}" else repo?.text("name")?:"مستودع",if(s.repositoryId==null)"عندك ${s.repositories.size} مستودع" else "عندك ${s.items.size} عنصر",Icons.Rounded.FolderCopy)}
  item{Search(search,{search=it},if(s.repositoryId==null)"دوّر مستودع" else "دوّر هنا")}
  if(s.repositoryId==null){
   item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("مستودعاتك",style=MaterialTheme.typography.titleMedium);MotionIconButton(onClick=vm::home,enabled=!s.busy){Icon(Icons.Rounded.Refresh,"حدّث")}}}
   if(s.repositories.isEmpty()&&!s.busy)item{Empty("مافي مستودعات","اسأل المشرف")}
   items(s.repositories.filter{it.text("name").contains(search,true)},key={it.text("id")}){r->
    MotionCard(onClick={search="";vm.browse(r.text("id"))},shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()){
     Row(Modifier.padding(20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)){
      Icon(Icons.Rounded.Inventory2,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(30.dp))
      Column(Modifier.weight(1f)){Text(r.text("name"),fontWeight=FontWeight.SemiBold);Text("${r.text("owner")} · ${r.text("default_branch")}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
      Icon(Icons.Rounded.ChevronLeft,null)
     }
    }
   }
  }else{
   item{Row(verticalAlignment=Alignment.CenterVertically){MotionTextButton(onClick={search="";if(s.path.isEmpty())vm.home() else vm.browse(s.repositoryId,s.path.substringBeforeLast('/',""))}){Icon(Icons.AutoMirrored.Rounded.ArrowBack,null);Text("ارجع")};Text(s.path.ifEmpty{"البداية"},Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis);MotionIconButton(onClick={vm.browse(s.repositoryId,s.path)}){Icon(Icons.Rounded.Refresh,"حدّث")}}}
   if(s.role=="ADMIN") item{Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
    MotionAssistChip(onClick={filePicker.launch(arrayOf("*/*"))},label={Text("ارفع ملفات")},leadingIcon={Icon(Icons.Rounded.UploadFile,null)})
    MotionAssistChip(onClick={folderPicker.launch(null)},label={Text("ارفع مجلد")},leadingIcon={Icon(Icons.Rounded.DriveFolderUpload,null)})
    MotionAssistChip(onClick={dialog="mkdir"},label={Text("مجلد جديد")},leadingIcon={Icon(Icons.Rounded.CreateNewFolder,null)})
   }}
   item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){MotionTextButton(onClick={vm.download(if(s.selected.isEmpty())listOf(s.path) else s.selected.toList())}){Icon(Icons.Rounded.Download,null);Text(if(s.selected.isEmpty())"نزّل المجلد" else "نزّل ${s.selected.size} عناصر")};if(s.selected.isNotEmpty())MotionTextButton(onClick={s.selected.forEach(vm::toggle)}){Text("شيل التحديد")}}}
   if(s.items.isEmpty()&&!s.busy)item{Empty("المجلد فاضي","ارفع أول ملف")}
   items(s.items.filter{it.text("name").contains(search,true)}.sortedBy{it.text("type")!="dir"},key={it.text("path")}){f->
    val folder=f.text("type")=="dir";val selected=f.text("path") in s.selected
    var menu by remember{mutableStateOf(false)}
    MotionCard(onClick={if(folder){search="";vm.browse(s.repositoryId,f.text("path"))}else vm.toggle(f.text("path"))},shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=if(selected)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),modifier=Modifier.fillMaxWidth().animateContentSize()){
     Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically){
      Checkbox(selected,{vm.toggle(f.text("path"))})
      Icon(if(folder)Icons.Rounded.Folder else fileIcon(f.text("name")),null,tint=MaterialTheme.colorScheme.primary)
      Column(Modifier.weight(1f).padding(12.dp)){Text(f.text("name"),maxLines=2,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.Medium);Text(if(folder)"مجلد" else size(f["size"]?.jsonPrimitive?.long?:0),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
      Box{MotionIconButton(onClick={menu=true}){Icon(Icons.Rounded.MoreVert,"خيارات")};DropdownMenu(menu,{menu=false}){
       DropdownMenuItem(text={Text(if(folder)"نزّل ZIP" else "نزّل الملف")},onClick={menu=false;vm.download(listOf(f.text("path")),!folder)},leadingIcon={Icon(Icons.Rounded.Download,null)})
       if(s.role=="ADMIN"){
        DropdownMenuItem(text={Text("غيّر مكانه واسمه")},onClick={menu=false;target=f;dialog="move"},leadingIcon={Icon(Icons.Rounded.DriveFileRenameOutline,null)})
        DropdownMenuItem(text={Text("امسح")},onClick={menu=false;target=f;dialog="delete"},leadingIcon={Icon(Icons.Rounded.DeleteOutline,null)})
       }
      }}
     }
    }
   }
  }
 }
}
@Composable private fun LocalScreen(files:List<LocalFile>,vm:VaultViewModel,signedIn:Boolean) {
 var search by rememberSaveable{mutableStateOf("")}
 var prefix by rememberSaveable{mutableStateOf("")}
 var delete by remember{mutableStateOf<LocalFile?>(null)}
 val context=LocalContext.current
 if(delete!=null)Confirm("نمسحه من جهازك؟","GitHub مايتغيّر",{delete=null}){vm.deleteLocal(delete!!);delete=null}
 BackHandler(prefix.isNotEmpty()){prefix=prefix.substringBeforeLast('/',"")}
 val filtered=files.filter{it.relativePath.contains(search,true)}
 val folders=if(search.isBlank())filtered.filter{it.relativePath.startsWith(if(prefix.isEmpty())"" else "$prefix/")}.map{it.relativePath.removePrefix(if(prefix.isEmpty())"" else "$prefix/")}.filter{it.contains('/')}.map{it.substringBefore('/')}.distinct().sorted() else emptyList()
 val visible=if(search.isBlank())filtered.filter{it.relativePath.substringBeforeLast('/',"")==prefix} else filtered
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=16.dp)){
  item{Hero("ملفاتك عندك","عندك ${files.size} ملف",Icons.Rounded.OfflinePin)}
  item{Text("ملفاتك تبقى هنا",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
  item{Search(search,{search=it},"دوّر بملفاتك")}
  item{Row{if(prefix.isNotEmpty())MotionTextButton(onClick={prefix=prefix.substringBeforeLast('/',"")}){Text("ارجع")};if(signedIn || files.any{it.repositoryId.startsWith("public:saged7/")})MotionTextButton(onClick=vm::syncLocal){Icon(Icons.Rounded.Sync,null);Text("شوف الجديد")}}}
  if(files.isEmpty())item{Empty("مافي ملفات","نزّل أول ملف",Icons.Rounded.OfflinePin)}
  items(folders){folder->MotionCard(onClick={prefix=if(prefix.isEmpty())folder else "$prefix/$folder"},modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(16.dp)){Icon(Icons.Rounded.Folder,null);Spacer(Modifier.width(12.dp));Text(folder)}}}
  items(visible,key={it.id}){f->
   MotionCard(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){
    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
     Row(verticalAlignment=Alignment.CenterVertically){Icon(fileIcon(f.relativePath),null,tint=MaterialTheme.colorScheme.primary);Text(f.relativePath,Modifier.weight(1f).padding(horizontal=12.dp),maxLines=2,overflow=TextOverflow.Ellipsis);MotionIconButton(onClick={delete=f}){Icon(Icons.Rounded.DeleteOutline,"امسحه هنا")}}
     if(f.removedRemotely)Text("نسختك لسه هنا",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.labelMedium)
     else if(f.updateAvailable)Text("فيه نسخة جديدة",color=MaterialTheme.colorScheme.primary)
     Row{MotionTextButton(onClick={try{val file=File(f.localPath);check(file.exists()){ "الملف مش موجود" };val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file);val mime=MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())?:"application/octet-stream";context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).setDataAndType(uri,mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"افتح بواسطة"))}catch(e:Exception){vm.message("جيب تطبيق يفتحه")}}){Icon(Icons.AutoMirrored.Rounded.OpenInNew,null);Text("افتح")};if(f.updateAvailable&&(signedIn||f.repositoryId.startsWith("public:saged7/")))MotionTextButton(onClick={vm.updateLocal(f)}){Text("نزّل الجديد")}}
    }
   }
  }
 }
}
@Composable private fun TransfersScreen(transfers:List<Transfer>,vm:VaultViewModel) {TransferContent(transfers,vm::retry,vm::cancel)}

@Composable private fun UsersScreen(s:VaultState,vm:VaultViewModel) {
 var search by rememberSaveable{mutableStateOf("")};var dialog by remember{mutableStateOf("")};var target by remember{mutableStateOf<JsonObject?>(null)}
 if(dialog=="create")FormDialog("ضيف حساب",listOf("اسمك","باسورد مؤقت"),passwordIndices=setOf(1),onDismiss={dialog=""}){v->vm.userAction("create",extra=VaultViewModel.obj("username" to v[0].lowercase(),"password" to v[1]));dialog=""}
 if(dialog=="reset")FormDialog("غيّر الباسورد",listOf("الباسورد الجديد"),passwordIndices=setOf(0),onDismiss={dialog=""}){v->vm.userAction("reset",target!!.text("id"),VaultViewModel.obj("password" to v[0]));dialog=""}
 if(dialog=="access")AlertDialog(onDismissRequest={dialog=""},title={Text("صلاحيات ${target?.text("username")}")},text={Column(Modifier.heightIn(max=380.dp).verticalScroll(rememberScrollState())){s.managed.forEach{r->val grant=s.access.any{it.text("user_id")==target!!.text("id")&&it.text("repository_id")==r.text("id")};Row(verticalAlignment=Alignment.CenterVertically){Checkbox(grant,{vm.userAction(if(grant)"revoke" else "grant",target!!.text("id"),VaultViewModel.obj("repository_id" to r.text("id")))},enabled=!s.busy);Text(r.text("name"))}};if(s.managed.isEmpty())Text("اربط مستودع أول")}},confirmButton={MotionTextButton(onClick={dialog=""}){Text("تمام")}})
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=16.dp)){
  item{Hero("الناس وصلاحياتهم","عندك ${s.users.size} حساب",Icons.Rounded.Group)}
  item{Row{MotionButton(onClick={dialog="create"}){Icon(Icons.Rounded.PersonAdd,null);Text("ضيف مستخدم")};Spacer(Modifier.weight(1f));MotionIconButton(onClick=vm::users){Icon(Icons.Rounded.Refresh,"حدّث")}}}
  item{Search(search,{search=it},"دوّر اسم")}
  items(s.users.filter{it.text("username").contains(search,true)},key={it.text("id")}){u->MotionCard(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)){
   Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Rounded.AccountCircle,null,Modifier.size(36.dp),tint=MaterialTheme.colorScheme.primary);Column(Modifier.padding(horizontal=12.dp)){Text(u.text("username"),fontWeight=FontWeight.Bold);Text(if(u.text("role")=="ADMIN")"المشرف" else status(u.text("status")),style=MaterialTheme.typography.labelMedium)}}
    if(u.text("role")!="ADMIN")Row(Modifier.horizontalScroll(rememberScrollState())){
     if(u.text("status")=="PENDING"){
      MotionTextButton(onClick={vm.userAction("review",u.text("id"),buildJsonObject{put("approve",true)})}){Text("وافق")}
      MotionTextButton(onClick={vm.userAction("review",u.text("id"),buildJsonObject{put("approve",false)})}){Text("ارفض")}
     }else{
      if(u.text("status") in listOf("ACTIVE","DISABLED"))MotionTextButton(onClick={vm.userAction("status",u.text("id"),VaultViewModel.obj("status" to if(u.text("status")=="ACTIVE")"DISABLED" else "ACTIVE"))}){Text(if(u.text("status")=="ACTIVE")"وقّفه" else "شغّله")}
      MotionTextButton(onClick={target=u;dialog="access"}){Text("الصلاحيات")}
      MotionTextButton(onClick={target=u;dialog="reset"}){Text("الباسورد")}
     }
    }
   }
  }}
 }
}
@Composable private fun AdminScreen(s:VaultState,vm:VaultViewModel) {
 var link by remember{mutableStateOf(false)};var target by remember{mutableStateOf<JsonObject?>(null)}
 if(link)FormDialog("اربط مستودع",listOf("اسم المستودع"),onDismiss={link=false}){v->vm.repoAction("link",extra=VaultViewModel.obj("name" to v[0]));link=false}
 if(target!=null)Confirm("نفك الربط؟","GitHub ماينمسح",{target=null}){vm.repoAction("unlink",target!!.text("id"));target=null}
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=16.dp)){
  item{Hero("مين يدخل؟","اربط واضبط",Icons.Rounded.AdminPanelSettings)}
  item{Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){MotionButton(onClick={link=true}){Icon(Icons.Rounded.Add,null);Text("اربط مستودع")};MotionOutlinedButton(onClick={vm.available()}){Text("دوّر مستودعات")};MotionIconButton(onClick=vm::managed){Icon(Icons.Rounded.Refresh,"حدّث")}}}
  if(s.available.isNotEmpty())item{Text("تقدر تربطه",style=MaterialTheme.typography.titleMedium)}
  items(s.available.filter{a->s.managed.none{it.text("github_id")==a.text("github_id")}}){a->MotionCard(Modifier.fillMaxWidth()){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Text(a.text("name"),Modifier.weight(1f));MotionTextButton(onClick={vm.repoAction("link",extra=VaultViewModel.obj("name" to a.text("name")))}){Text("اربطه")}}}}
  items(s.managed,key={it.text("id")}){r->MotionCard(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(18.dp)){
   Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Rounded.FolderCopy,null,tint=MaterialTheme.colorScheme.primary);Text(r.text("name"),Modifier.weight(1f).padding(horizontal=12.dp),fontWeight=FontWeight.Bold);Switch(r["active"]?.jsonPrimitive?.boolean?:false,{vm.repoAction("status",r.text("id"),buildJsonObject{put("active",it)})})}
   Text("${r.text("owner")} / ${r.text("default_branch")}",style=MaterialTheme.typography.bodySmall)
   MotionTextButton(onClick={target=r}){Icon(Icons.Rounded.LinkOff,null);Text("فك الربط")}
  }}}
  if(s.managed.isEmpty()&&!s.busy)item{Empty("مافي ربط","اربط أول مستودع")}
 }
}
@Composable private fun AuditScreen(s:VaultState,vm:VaultViewModel) {
 var query by rememberSaveable{mutableStateOf("")};var outcome by rememberSaveable{mutableStateOf("")}
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=16.dp)){
  item{Hero("وش صار؟","شوف اللي صار",Icons.Rounded.History)}
  item{Search(query,{query=it},"دوّر عملية")}
  item{Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("" to "الكل","success" to "ضبط","failed" to "ما ضبط","started" to "بدأنا").forEach{(value,title)->MotionFilterChip(selected=outcome==value,onClick={outcome=value;vm.audit(0,query,value)},label={Text(title)})};MotionIconButton(onClick={vm.audit(0,query,outcome)}){Icon(Icons.Rounded.Search,"دوّر")}}}
  items(s.logs,key={it.text("id")}){log->MotionCard(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
   Row{Text(log.text("action"),Modifier.weight(1f),fontWeight=FontWeight.Medium);Icon(if(log.text("outcome")=="failed")Icons.Rounded.ErrorOutline else Icons.AutoMirrored.Rounded.FactCheck,null,tint=if(log.text("outcome")=="failed")MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)}
   Text(log.text("target"),style=MaterialTheme.typography.bodySmall,maxLines=3,overflow=TextOverflow.Ellipsis)
   Text(status(log.text("outcome")),style=MaterialTheme.typography.labelSmall)
   Text(log.text("created_at").replace('T',' ').take(19),style=MaterialTheme.typography.labelSmall)
   Text("الفاعل: ${log.text("actor_id").take(12)}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }}}
  if(s.logs.isEmpty()&&!s.busy)item{Empty("مافي نتائج","غيّر بحثك",Icons.Rounded.History)}
  item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){MotionTextButton(onClick={vm.audit((s.logOffset-50).coerceAtLeast(0),query,outcome)},enabled=s.logOffset>0&&!s.busy){Text("اللي قبل")};Text("${s.logOffset/50+1}");MotionTextButton(onClick={vm.audit(s.logOffset+50,query,outcome)},enabled=s.logs.size==50&&!s.busy){Text("اللي بعد")}}}
 }
}
@Composable private fun SettingsScreen(s:VaultState,vm:VaultViewModel,theme:String,onTheme:(String)->Unit) {
 var password by remember{mutableStateOf(false)};var logout by remember{mutableStateOf(false)}
 if(password)PasswordDialog(vm){password=false}
 if(logout)Confirm("بتطلع؟","بنوقّف الشغل",{logout=false}){vm.logout();logout=false}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){
  Hero(s.username,if(s.role=="ADMIN")"أنت المشرف" else "حسابك هنا",Icons.Rounded.VerifiedUser)
  com.saged.githubvault.feature.github.GitHubTokenSettings()
  Text("الشكل",style=MaterialTheme.typography.titleMedium)
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("system" to "زي الجهاز","light" to "فاتح","dark" to "غامق").forEach{(id,title)->MotionFilterChip(selected=theme==id,onClick={onTheme(id)},label={Text(title)})}}
  MotionOutlinedButton(onClick={password=true},modifier=Modifier.fillMaxWidth()){Icon(Icons.Rounded.Password,null);Spacer(Modifier.width(8.dp));Text("غيّر الباسورد")}
  MotionOutlinedButton(onClick={vm.tab("home")},modifier=Modifier.fillMaxWidth()){Text("إدارة السحابة")}
  Text("ملفاتك بأمان",style=MaterialTheme.typography.titleMedium)
  Text("حذف التطبيق يمسحها",color=MaterialTheme.colorScheme.onSurfaceVariant)
  Text("رفع saged7: 90MiB",style=MaterialTheme.typography.bodySmall)
  HorizontalDivider()
  MotionTextButton(onClick={logout=true},modifier=Modifier.fillMaxWidth()){Icon(Icons.AutoMirrored.Rounded.Logout,null);Spacer(Modifier.width(8.dp));Text("اطلع")}
  Text("Vault · 0.8.0",modifier=Modifier.align(Alignment.CenterHorizontally),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.outline)
 }
}
@Composable private fun PasswordScreen(s:VaultState,vm:VaultViewModel,forced:Boolean) {
 Column(Modifier.padding(28.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
  Hero("أمّن حسابك","غيّر الباسورد أول",Icons.Rounded.LockReset)
  PasswordForm(vm)
  MotionTextButton(onClick=vm::logout,enabled=!s.busy){Text("اطلع")}
 }
}
@Composable private fun PasswordDialog(vm:VaultViewModel,onDismiss:()->Unit) {AlertDialog(onDismissRequest=onDismiss,title={Text("غيّر الباسورد")},text={PasswordForm(vm,onDismiss)},confirmButton={},dismissButton={MotionTextButton(onClick=onDismiss){Text("سكّر")}})}
@Composable private fun PasswordForm(vm:VaultViewModel,done:()->Unit={}) {
 var current by remember{mutableStateOf("")};var next by remember{mutableStateOf("")};var confirm by remember{mutableStateOf("")}
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  listOf(Triple(current,{v:String->current=v},"الباسورد القديم"),Triple(next,{v:String->next=v},"الباسورد الجديد"),Triple(confirm,{v:String->confirm=v},"عيد الجديد")).forEach{(value,change,label)->OutlinedTextField(value,change,label={Text(label)},singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())}
  Text("الباسورد: 12–128 حرف",style=MaterialTheme.typography.bodySmall)
  MotionButton(onClick={vm.changePassword(current,next);current="";next="";confirm="";done()},enabled=current.isNotBlank()&&next.length in 12..128&&next==confirm&&next!=current,modifier=Modifier.fillMaxWidth()){Text("احفظ الباسورد")}
 }
}
@Composable private fun FormDialog(title:String,labels:List<String>,initial:List<String> = emptyList(),passwordIndices:Set<Int> = emptySet(),onDismiss:()->Unit,onConfirm:(List<String>)->Unit) {
 val values=remember{mutableStateListOf<String>().apply{addAll(labels.indices.map{initial.getOrNull(it)?:""})}}
 AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){labels.forEachIndexed{i,label->OutlinedTextField(values[i],{values[i]=it},label={Text(label)},singleLine=true,visualTransformation=if(i in passwordIndices)PasswordVisualTransformation() else VisualTransformation.None,modifier=Modifier.fillMaxWidth())}}},confirmButton={MotionTextButton(onClick={onConfirm(values.toList())},enabled=values.all{it.isNotBlank()}&&passwordIndices.all{values[it].length in 12..128}){Text("احفظ")}},dismissButton={MotionTextButton(onClick=onDismiss){Text("خلّيه")}})
}
@Composable private fun Confirm(title:String,text:String,onDismiss:()->Unit,onConfirm:()->Unit) {AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={Text(text)},confirmButton={MotionTextButton(onClick=onConfirm){Text("تمام")}},dismissButton={MotionTextButton(onClick=onDismiss){Text("خلّيه")}})}
private fun fileIcon(name:String):ImageVector=when(name.substringAfterLast('.').lowercase()){"pdf"->Icons.Rounded.PictureAsPdf;"jpg","jpeg","png","webp"->Icons.Rounded.Image;"mp4","mkv","mov"->Icons.Rounded.Movie;"zip","rar","7z"->Icons.Rounded.FolderZip;"mp3","wav"->Icons.Rounded.AudioFile;else->Icons.AutoMirrored.Rounded.InsertDriveFile}
private fun size(bytes:Long):String=when{bytes>=1024*1024->String.format(Locale.US,"%.1f MiB",bytes/1048576.0);bytes>=1024->String.format(Locale.US,"%.1f KiB",bytes/1024.0);else->"$bytes B"}
private fun status(code:String)=when(code){"QUEUED"->"بالطابور";"RUNNING"->"شغّال";"RETRYING"->"بنجرّب ثاني";"SUCCEEDED","success"->"خلصت";"FAILED","failed"->"ما ضبطت";"CANCELLED"->"وقفناها";"ACTIVE"->"شغّال";"DISABLED"->"موقوف";"PENDING","pending"->"ينتظر المشرف";"REJECTED"->"ما انقبل";"started"->"بدأنا";else->code}

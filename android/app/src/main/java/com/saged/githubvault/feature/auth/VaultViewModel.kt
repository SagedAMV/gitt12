package com.saged.githubvault.feature.auth

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.saged.githubvault.app.VaultApplication
import com.saged.githubvault.core.database.*
import com.saged.githubvault.core.network.ApiException
import com.saged.githubvault.core.network.userMessage
import com.saged.githubvault.feature.downloads.TransferWorker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import java.util.UUID

data class VaultState(
 val busy:Boolean=false,val message:String?=null,val signedIn:Boolean=false,
 val username:String="",val role:String="USER",val tab:String="github",
 val repositories:List<JsonObject> = emptyList(),val items:List<JsonObject> = emptyList(),
 val repositoryId:String?=null,val path:String="",val mustChangePassword:Boolean=false,
 val users:List<JsonObject> = emptyList(),val access:List<JsonObject> = emptyList(),
 val managed:List<JsonObject> = emptyList(),val available:List<JsonObject> = emptyList(),
 val logs:List<JsonObject> = emptyList(),val logOffset:Int=0,val selected:Set<String> = emptySet()
)
fun JsonObject.text(key:String)=this[key]?.jsonPrimitive?.content?:""
class VaultViewModel(application:Application):AndroidViewModel(application) {
 private val app=application as VaultApplication
 private val api=app.api
 private val mutable=MutableStateFlow(VaultState())
 val state=mutable.asStateFlow()
 val local=app.database.files().observe().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val transfers=app.database.transfers().observe().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 init {api.cachedProfile?.let{profile(it)};if(api.hasSession)task{profile(api.call("account",obj("action" to "me")).getValue("profile").jsonObject);if(!mutable.value.mustChangePassword)loadRepositories()}}
 private fun profile(p:JsonObject) {mutable.value=mutable.value.copy(signedIn=true,username=p.text("username"),role=p.text("role"),mustChangePassword=p["must_change_password"]?.jsonPrimitive?.boolean?:false)}
 private fun task(block:suspend()->Unit) {if(mutable.value.busy)return;viewModelScope.launch {
  mutable.value=mutable.value.copy(busy=true,message=null)
  try {block()}catch(e:CancellationException){throw e}catch(e:Exception){
   if(e is ApiException && e.code==401 && !api.hasSession)mutable.value=VaultState()
   mutable.value=mutable.value.copy(message=e.userMessage())
  }finally{mutable.value=mutable.value.copy(busy=false)}
 }}
 fun configure(url:String,key:String) {try{api.configure(url,key);mutable.value=VaultState(message="الاتصال جاهز")}catch(e:Exception){message(e.message)}}
 fun message(text:String?){mutable.value=mutable.value.copy(message=text)}
 fun login(name:String,password:String)=task {profile(api.login(name.trim().lowercase(),password));if(!mutable.value.mustChangePassword)loadRepositories()}
 fun register(name:String,password:String)=task{api.call("registration",obj("username" to name.trim().lowercase(),"password" to password),false);message("طلبك عند المشرف")}
 private suspend fun loadRepositories(){val d=api.call("repositories");mutable.value=mutable.value.copy(repositories=d.list("repositories"),repositoryId=null,path="",items=emptyList(),selected=emptySet())}
 fun home()=task{loadRepositories()}
 fun tab(value:String) {mutable.value=mutable.value.copy(tab=value);when(value){"home"->if(api.hasSession&&!mutable.value.mustChangePassword)home();"users"->users();"admin"->managed();"audit"->audit()}}
 fun browse(id:String,path:String="")=task{val d=api.call("files",obj("repository_id" to id,"path" to path));mutable.value=mutable.value.copy(items=d.list("items"),repositoryId=id,path=path,selected=emptySet(),tab="home")}
 fun toggle(path:String) {mutable.value=mutable.value.copy(selected=mutable.value.selected.let{if(path in it)it-path else it+path})}
 fun logout()=task {try{api.call("account",obj("action" to "logout"))}finally{WorkManager.getInstance(app).cancelAllWorkByTag("vault-transfer");for(t in app.database.transfers().pending())app.database.transfers().update(t.id,"CANCELLED",0f,"طلعت خلاص");api.clear();mutable.value=VaultState(tab="home")}}
 fun changePassword(current:String,next:String)=task{api.call("account",obj("action" to "password","current_password" to current,"password" to next));api.clear();mutable.value=VaultState(message="ادخل بالباسورد الجديد")}
 fun users()=task{loadUsers()}
 private suspend fun loadUsers(){val d=api.call("admin-users",obj("action" to "list"));val repos=api.call("admin-repositories",obj("action" to "list"));mutable.value=mutable.value.copy(users=d.list("users"),access=d.list("access"),managed=repos.list("repositories"))}
 fun userAction(action:String,id:String="",extra:JsonObject=buildJsonObject{})=task {api.call("admin-users",JsonObject(obj("action" to action,"user_id" to id)+extra));loadUsers();message("حسابك تحدّث")}
 fun managed()=task{loadManaged()}
 private suspend fun loadManaged(){mutable.value=mutable.value.copy(managed=api.call("admin-repositories",obj("action" to "list")).list("repositories"))}
 fun available(page:Int=1)=task{mutable.value=mutable.value.copy(available=api.call("admin-repositories",buildJsonObject{put("action","available");put("page",page)}).list("repositories"))}
 fun repoAction(action:String,id:String="",extra:JsonObject=buildJsonObject{})=task{api.call("admin-repositories",JsonObject(obj("action" to action,"repository_id" to id)+extra));loadManaged();message("حدّثنا المستودعات")}
 fun audit(offset:Int=0,query:String="",outcome:String="")=task{val d=api.call("audit",buildJsonObject{put("offset",offset);put("action",query);put("outcome",outcome)});mutable.value=mutable.value.copy(logs=d.list("logs"),logOffset=offset)}
 fun manageItem(action:String,path:String,destination:String="")=task{
  val id=mutable.value.repositoryId?:return@task
  api.call("admin-items",obj("action" to action,"repository_id" to id,"path" to path,"destination" to destination))
  val d=api.call("files",obj("repository_id" to id,"path" to mutable.value.path));mutable.value=mutable.value.copy(items=d.list("items"),selected=emptySet());message("ضبطت")
 }
 fun download(paths:List<String>,raw:Boolean=false)=task {
  val repo=mutable.value.repositoryId?:return@task
  queueDownload(repo,paths,raw);message("بدأنا التنزيل")
 }
 private suspend fun queueDownload(repo:String,paths:List<String>,raw:Boolean) {
  val payload=buildJsonObject{put("repository_id",repo);put("paths",JsonArray(paths.map{JsonPrimitive(it)}));put("format",if(raw)"file" else "zip")}
  queue("DOWNLOAD",repo,if(paths.size==1)paths[0].ifEmpty{"المستودع كله"} else "${paths.size} عناصر",payload)
 }
 private suspend fun queue(kind:String,repo:String,label:String,payload:JsonObject) {
  val id=UUID.randomUUID().toString();app.database.transfers().put(Transfer(id,kind,repo,label,payload.toString()));TransferWorker.enqueue(app,id)
 }
 fun retry(t:Transfer)=task{app.database.transfers().update(t.id,"QUEUED",0f);if(t.kind in listOf("PUBLIC_DOWNLOAD","DIRECT_UPLOAD"))com.saged.githubvault.feature.github.PublicTransferWorker.enqueue(app,t.id,t.kind=="DIRECT_UPLOAD") else TransferWorker.enqueue(app,t.id)}
 fun cancel(t:Transfer){WorkManager.getInstance(app).cancelUniqueWork(t.id);viewModelScope.launch{app.database.transfers().update(t.id,"CANCELLED",0f,"وقفناها")}}
 fun upload(uris:List<Uri>,folder:Boolean=false)=task {
  val repo=mutable.value.repositoryId?:return@task;val prefix=mutable.value.path
  withContext(Dispatchers.IO){
   val list=mutableListOf<Pair<DocumentFile,String>>()
   val emptyFolders=mutableListOf<String>()
   fun walk(f:DocumentFile,path:String) {check(list.size+emptyFolders.size<1000){"حدّك 1000 ملف"};if(f.isDirectory){val children=f.listFiles();if(children.isEmpty())emptyFolders+=path;children.forEach{walk(it,if(path.isEmpty())it.name?:"file" else "$path/${it.name?:"file"}")}}else list+=f to path}
   for(uri in uris) {
    runCatching{app.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
    val f=if(folder)DocumentFile.fromTreeUri(app,uri) else DocumentFile.fromSingleUri(app,uri)
    if(f!=null)walk(f,f.name?:"file")
   }
   check(list.sumOf{it.first.length()}<=200L*1024*1024){"حدّها 200MiB"}
   var queued=0;var rejected=0
   for(path in emptyFolders){val full=if(prefix.isEmpty())path else "$prefix/$path";queue("MKDIR",repo,full,obj("action" to "mkdir","repository_id" to repo,"path" to full));queued++}
   for((file,path) in list){
    if(file.length()>8L*1024*1024){rejected++;continue}
    val full=if(prefix.isEmpty())path else "$prefix/$path"
    val expected=mutable.value.items.find{it.text("path")==full}?.text("sha")
    val payload=buildJsonObject{put("uri",file.uri.toString());put("path",full);put("size",if(file.length()>0)file.length() else -1L);if(!expected.isNullOrEmpty())put("expected_sha",expected)}
    queue("UPLOAD",repo,full,payload);queued++
   }
   message(if(rejected>0)"رفضنا $rejected ملفات" else "بالطابور $queued ملفات")
  }
 }
 fun updateLocal(file:LocalFile)=task {
  if(file.repositoryId.startsWith("public:saged7/")) {
   val repo=file.repositoryId.removePrefix("public:saged7/");val remote=app.github.file(repo,file.relativePath)
   require((remote["size"]?.jsonPrimitive?.long?:0)<=90L*1024*1024){"حدّه 90MiB"}
   val id=UUID.randomUUID().toString()
   val payload=buildJsonObject{put("repo",repo);put("path",file.relativePath);put("sha",remote.getValue("sha"));put("size",remote["size"]?:JsonPrimitive(0))}
   app.database.transfers().put(Transfer(id,"PUBLIC_DOWNLOAD",file.repositoryId,"$repo/${file.relativePath}",payload.toString()))
   com.saged.githubvault.feature.github.PublicTransferWorker.enqueue(app,id)
  }else queueDownload(file.repositoryId,listOf(file.relativePath),true)
  message("بننزّل الجديد")
 }
 fun syncLocal()=task {
  for((repo,files) in app.database.files().all().groupBy{it.repositoryId}){
   try{
    val versions=if(repo.startsWith("public:saged7/"))app.github.versions(repo.removePrefix("public:saged7/"))else {
     if(!api.hasSession)continue
     api.call("files",obj("repository_id" to repo,"action" to "versions")).getValue("versions").jsonObject
    }
    for(f in files){val sha=versions[f.relativePath]?.jsonPrimitive?.content;app.database.files().flags(f.id,f.sha,sha==null,sha!=null&&sha!=f.sha)}
   }catch(e:ApiException){if(e.code!=403&&e.code!=404)throw e}
  };message("فحصنا ملفاتك")
 }
 fun deleteLocal(file:LocalFile)=task{java.io.File(file.localPath).delete();app.database.files().delete(file.id)}
 companion object {fun obj(vararg values:Pair<String,String>)=buildJsonObject{values.forEach{put(it.first,it.second)}}}
}
private fun JsonObject.list(key:String)=this[key]?.jsonArray?.map{it.jsonObject}?:emptyList()

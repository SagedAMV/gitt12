package com.saged.githubvault.feature.github
import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saged.githubvault.app.VaultApplication
import com.saged.githubvault.core.database.Transfer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import java.util.UUID

data class GitHubState(
 val busy:Boolean=false,val message:String?=null,val repositories:List<JsonObject> = emptyList(),
 val repo:String?=null,val branch:String="main",val path:String="",val files:List<JsonObject> = emptyList(),
 val page:Int=1,val hasMore:Boolean=false,val tokenSaved:Boolean=false,val tokenUser:String?=null,
 val preview:String?=null,val previewName:String=""
)
class GitHubViewModel(application:Application):AndroidViewModel(application) {
 private val app=application as VaultApplication
 private val mutable=MutableStateFlow(GitHubState(tokenSaved=app.github.hasToken))
 val state=mutable.asStateFlow()
 init {repositories()}
 private fun task(block:suspend()->Unit) {if(mutable.value.busy)return;viewModelScope.launch {mutable.value=mutable.value.copy(busy=true,message=null);try{block()}catch(e:CancellationException){throw e}catch(e:Exception){mutable.value=mutable.value.copy(message=if(e is java.io.IOException)"النت مش شغال" else SimpleCopy.message(e.message?:""))}finally{mutable.value=mutable.value.copy(busy=false)}}}
 fun createRepository(name:String)=task {
  app.github.createRepository(name.trim())
  val repos=app.github.repositories(1)
  mutable.value=mutable.value.copy(repositories=repos,repo=null,path="",message="المستودع جاهز")
 }
 fun clearMessage(){mutable.value=mutable.value.copy(message=null)}
 fun repositories(next:Boolean=false)=task {
  val page=if(next)mutable.value.page+1 else 1;val data=app.github.repositories(page)
  mutable.value=mutable.value.copy(repositories=if(next)(mutable.value.repositories+data).distinctBy{it["id"].toString()} else data,repo=null,path="",files=emptyList(),page=page,hasMore=data.size==100)
 }
 fun open(repo:String,branch:String,path:String="")=task {
  val files=app.github.files(repo,path,branch)
  mutable.value=mutable.value.copy(repo=repo,branch=branch,path=path,files=files,preview=null)
 }
 fun back() {val s=mutable.value;if(s.preview!=null)closePreview()else if(s.repo!=null && s.path.isNotEmpty())open(s.repo,s.branch,s.path.substringBeforeLast('/',""))else mutable.value=mutable.value.copy(repo=null,path="",files=emptyList())}
 fun preview(file:JsonObject)=task {val s=mutable.value;mutable.value=s.copy(busy=true,preview=app.github.preview(s.repo!!,file.getValue("sha").jsonPrimitive.content),previewName=file.getValue("name").jsonPrimitive.content)}
 fun closePreview(){mutable.value=mutable.value.copy(preview=null)}
 fun token(value:String)=task{val user=app.github.validateAndSaveToken(value);mutable.value=mutable.value.copy(tokenSaved=true,tokenUser=user,message="التوكن جاهز")}
 fun removeToken()=task {
  withContext(Dispatchers.IO){app.github.removeToken()}
  androidx.work.WorkManager.getInstance(app).cancelAllWorkByTag("public-upload")
  for(t in app.database.transfers().pending().filter{it.kind=="DIRECT_UPLOAD"})app.database.transfers().update(t.id,"CANCELLED",0f,"مسحنا التوكن")
  mutable.value=mutable.value.copy(tokenSaved=false,tokenUser=null,message="حذفنا التوكن")
 }
 private suspend fun queue(kind:String,repo:String,path:String,payload:JsonObject) {
  val id=UUID.randomUUID().toString();app.database.transfers().put(Transfer(id,kind,"public:saged7/$repo","$repo/$path",payload.toString()));PublicTransferWorker.enqueue(app,id,kind=="DIRECT_UPLOAD")
 }
 fun download(file:JsonObject)=task {
  val repo=mutable.value.repo!!;val path=file.getValue("path").jsonPrimitive.content
  require((file["size"]?.jsonPrimitive?.long?:0)<=90L*1024*1024){"الملف فوق الحد"}
  queue("PUBLIC_DOWNLOAD",repo,path,buildJsonObject{put("repo",repo);put("path",path);put("sha",file.getValue("sha"));put("size",file["size"]?:JsonPrimitive(0))})
  mutable.value=mutable.value.copy(message="بدأنا التنزيل")
 }
 fun upload(uris:List<Uri>)=task {
  val s=mutable.value;val repo=s.repo?:return@task
  require(app.github.hasToken){"حط توكنك أول"}
  var queued=0;var rejected=0
  withContext(Dispatchers.IO){
   require(uris.size<=100){"اختار ملفات أقل"}
   for(uri in uris){
    runCatching{app.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
    val file=DocumentFile.fromSingleUri(app,uri)?:continue
    if(file.length()>GitHubPolicy.UPLOAD_LIMIT){rejected++;continue}
    val name=file.name?:"file";require(!name.contains('/')){"غيّر اسم الملف"}
    val path=if(s.path.isEmpty())name else "${s.path}/$name";GitHubPolicy.path(path)
    val expected=s.files.find{it["path"]?.jsonPrimitive?.content==path}?.get("sha")
    queue("DIRECT_UPLOAD",repo,path,buildJsonObject{put("repo",repo);put("path",path);put("branch",s.branch);put("uri",uri.toString());if(expected!=null)put("expected",expected)})
    queued++
   }
  }
  mutable.value=mutable.value.copy(message=if(rejected>0)"رفضنا $rejected ملفات" else "بالطابور $queued ملفات")
 }
}

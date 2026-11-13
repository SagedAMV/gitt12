package com.saged.githubvault.feature.github

import android.content.Context
import com.saged.githubvault.core.security.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

object GitHubPolicy {
 const val OWNER="saged7"
 const val UPLOAD_LIMIT=90L*1024*1024
 fun repo(name:String):String {require(name.matches(Regex("[A-Za-z0-9_.-]{1,100}")) && name!="." && name!=".."){"غيّر اسم المستودع"};return name}
 fun path(path:String):String {
  require(path.length<=1024 && !path.startsWith('/') && !path.contains('\\') && !path.contains(':') && path.none{it.code<32||it.code==127} && (path.isEmpty() || path.split('/').none{it.isEmpty()||it=="."||it==".."})){"المسار مش مضبوط"}
  return path
 }
 fun name(raw:String):String {val v=raw.trim();require(v.isNotEmpty() && v.length<=100 && !v.contains('/') && !v.contains('\\') && !v.contains(':') && v!="." && v!=".." && v.none{it.code<32||it.code==127}){"الاسم مش مضبوط"};return v}
 fun token(value:String):String {require(!value.contains('\n')&&!value.contains('\r')){"التوكن مش مضبوط"};val t=value.trim();require(t.length in 20..512 && t.all{it.code in 33..126}){"التوكن مش مضبوط"};return t}
 fun sha(value:String):String {require(value.matches(Regex("[a-f0-9]{40}"))){"النسخة مش مضبوطة"};return value}
 fun root(repo:String)="/repos/$OWNER/${GitHubPolicy.repo(repo)}"
}
class GitHubFailure(val status:Int,message:String):Exception(message)
/** host/scheme/port/tokenProvider: بوابات اختبار فقط (عقد HTTP على JVM)؛ الاستخدام الفعلي يتركها افتراضية. */
class GitHubClient(context:Context?=null,private val host:String="api.github.com",private val scheme:String="https",private val port:Int=-1,private val tokenProvider:(()->String?)?=null) {
 private val tokenStore=if(context!=null)SessionStore(context,"github_device_token","github_device_token_v1") else null
 private val client=OkHttpClient.Builder().callTimeout(900,TimeUnit.SECONDS).writeTimeout(300,TimeUnit.SECONDS).readTimeout(300,TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build()
 private fun readToken():String?=tokenProvider?.invoke()?:tokenStore?.read()
 val hasToken:Boolean get()=!readToken().isNullOrBlank()
 fun removeToken() {
  tokenStore?.clear()
  (client.dispatcher.queuedCalls()+client.dispatcher.runningCalls()).filter{it.request().header("Authorization")!=null}.forEach{it.cancel()}
 }
 private fun request(path:String,query:Map<String,String> = emptyMap(),accept:String="application/vnd.github+json",token:String?=null,method:String="GET",payload:JsonObject?=null):Request {
  require(path.startsWith('/') && !path.startsWith("//"))
  val url=HttpUrl.Builder().scheme(scheme).host(host).apply {if(port>0)port(port);path.substring(1).split('/').forEach{addPathSegment(it)};query.forEach{(k,v)->addQueryParameter(k,v)}}.build()
  return Request.Builder().url(url).header("Accept",accept).header("X-GitHub-Api-Version","2022-11-28").header("User-Agent","GitHubVault-Android/0.3.0")
   .apply{token?.let{header("Authorization","Bearer $it")};if(method!="GET")method(method,(payload?:buildJsonObject{}).toString().toRequestBody("application/json".toMediaType()))}.build()
 }
 private fun failure(response:Response):Nothing {
  val code=response.code;val remaining=response.header("X-RateLimit-Remaining");val reset=response.header("X-RateLimit-Reset")?.toLongOrNull()
  response.close()
  val message=when {
   code==401->"التوكن مش شغال"
   code==429 || code==403 && remaining=="0"->"جرّب بعدين"
   code==403->"ما معك صلاحية"
   code==404->"مش موجود"
   code==409 || code==422->"راجع الاسم والنسخة"
   else->"الطلب ما ضبط"
  }
  throw GitHubFailure(code,message)
 }
 private suspend fun json(path:String,query:Map<String,String> = emptyMap(),token:String?=null,method:String="GET",payload:JsonObject?=null):JsonElement=withContext(Dispatchers.IO){
  client.newCall(request(path,query,token=token,method=method,payload=payload)).execute().use{r->if(!r.isSuccessful)failure(r);Json.parseToJsonElement(r.body?.string()?:error("مافي رد"))}
 }
 suspend fun repositories(page:Int):List<JsonObject> {
  require(page in 1..1000)
  return json("/users/${GitHubPolicy.OWNER}/repos",mapOf("type" to "owner","sort" to "updated","per_page" to "100","page" to page.toString())).jsonArray.map{it.jsonObject}.filter{it["private"]?.jsonPrimitive?.boolean==false && it["owner"]?.jsonObject?.get("login")?.jsonPrimitive?.content.equals(GitHubPolicy.OWNER,true)}
 }
 suspend fun files(repo:String,path:String,branch:String):List<JsonObject> {
  val result=try{json(GitHubPolicy.root(repo)+"/contents"+GitHubPolicy.path(path).let{if(it.isEmpty())"" else "/$it"},mapOf("ref" to branch))}catch(e:GitHubFailure){
   if(e.status==404 && path.isEmpty()) {
    val info=json(GitHubPolicy.root(repo)).jsonObject
    if(info["size"]?.jsonPrimitive?.long==0L && info["private"]?.jsonPrimitive?.boolean==false)return emptyList()
   };throw e
  }
  require(result is JsonArray){"هذا مش مجلد"}
  require(result.size<1000){"المجلد كبير مرّة"}
  return result.map{it.jsonObject}.filter{it["type"]?.jsonPrimitive?.content in listOf("dir","file")}
 }
 suspend fun file(repo:String,path:String):JsonObject {
  val value=json(GitHubPolicy.root(repo)+"/contents/"+GitHubPolicy.path(path)).jsonObject
  require(value["type"]?.jsonPrimitive?.content=="file"){"هذا مش ملف"};return value
 }
 suspend fun versions(repo:String):JsonObject {
  val commits=json(GitHubPolicy.root(repo)+"/commits",mapOf("per_page" to "1")).jsonArray
  if(commits.isEmpty())return buildJsonObject{}
  val tree=commits[0].jsonObject.getValue("commit").jsonObject.getValue("tree").jsonObject.getValue("sha").jsonPrimitive.content
  val result=json(GitHubPolicy.root(repo)+"/git/trees/"+GitHubPolicy.sha(tree),mapOf("recursive" to "1")).jsonObject
  require(result["truncated"]?.jsonPrimitive?.boolean!=true){"المستودع كبير مرّة"}
  return buildJsonObject{result.getValue("tree").jsonArray.forEach{e->val v=e.jsonObject;if(v["type"]?.jsonPrimitive?.content=="blob"&&v["mode"]?.jsonPrimitive?.content!="120000")put(v.getValue("path").jsonPrimitive.content,v.getValue("sha"))}}
 }
 suspend fun blob(repo:String,sha:String):Response=withContext(Dispatchers.IO){
  val r=client.newCall(request(GitHubPolicy.root(repo)+"/git/blobs/"+GitHubPolicy.sha(sha),accept="application/vnd.github.raw+json")).execute()
  if(!r.isSuccessful)failure(r);r
 }
 suspend fun preview(repo:String,sha:String):String=withContext(Dispatchers.IO){
  blob(repo,sha).use{r->val input=r.body?.byteStream()?:error("مافي بيانات");val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);while(output.size()<=512*1024){val n=input.read(buffer,0,minOf(buffer.size,512*1024+1-output.size()));if(n<0)break;output.write(buffer,0,n)};val bytes=output.toByteArray();require(bytes.size<=512*1024){"نزّله وشوفه"};require(bytes.none{it.toInt()==0}){"نزّله وافتحه"};String(bytes,Charsets.UTF_8)}
 }
 suspend fun validateAndSaveToken(raw:String):String {
  val value=GitHubPolicy.token(raw)
  val data=json("/user",token=value).jsonObject
  val login=data["login"]?.jsonPrimitive?.content?:error("التوكن مش شغال")
  withContext(Dispatchers.IO){tokenStore?.save(value)}
  return login
 }
 suspend fun createRepository(name:String) {
  GitHubPolicy.repo(name)
  val token=withContext(Dispatchers.IO){readToken()}?:error("حط التوكن أول")
  val user=json("/user",token=token).jsonObject
  require(user["login"]?.jsonPrimitive?.content.equals(GitHubPolicy.OWNER,true)){"ادخل بحساب saged7"}
  json("/user/repos",token=token,method="POST",payload=buildJsonObject{put("name",name);put("private",false);put("auto_init",true)})
 }
 suspend fun upload(repo:String,path:String,branch:String,file:java.io.File,expected:String?,progress:(Long,Long)->Unit = {_,_->})=withContext(Dispatchers.IO) {
  require(file.length()<=GitHubPolicy.UPLOAD_LIMIT){"الملف فوق الحد"}
  GitHubPolicy.path(path);require(path.isNotBlank() && path!=".git" && !path.startsWith(".git/"))
  val token=readToken()?:error("حط التوكن أول")
  val info=json(GitHubPolicy.root(repo),token=token).jsonObject
  require(info["private"]?.jsonPrimitive?.boolean==false && info["owner"]?.jsonObject?.get("login")?.jsonPrimitive?.content.equals(GitHubPolicy.OWNER,true)){"اختار مستودع عام"}
  require(info["permissions"]?.jsonObject?.get("push")?.jsonPrimitive?.boolean!=false){"ما معك صلاحية"}
  expected?.let{GitHubPolicy.sha(it)}
  val body=StreamingUploadBody(file,path,branch,expected,progress=progress)
  val req=request(GitHubPolicy.root(repo)+"/contents/$path",token=token).newBuilder().put(body).build()
  client.newCall(req).execute().use{if(!it.isSuccessful)failure(it)}
 }
}

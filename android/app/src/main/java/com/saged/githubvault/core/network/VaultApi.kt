package com.saged.githubvault.core.network

import com.saged.githubvault.BuildConfig
import android.content.Context
import com.saged.githubvault.core.security.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

class ApiException(val code:Int,message:String):Exception(message)
class VaultApi(private val store:SessionStore,context:Context) {
 private val config=context.getSharedPreferences("connection",Context.MODE_PRIVATE)
 init {
  val previous=config.getString("url",null)
  if(previous!=null && previous.trimEnd('/')!=BuildConfig.SUPABASE_URL.trimEnd('/')) {
   config.edit().clear().apply();store.clear()
  }
 }
 private val endpoint:String get()=BuildConfig.SUPABASE_URL
 private val anonKey:String get()=config.getString("key",null)?:BuildConfig.SUPABASE_ANON_KEY
 fun configure(url:String,key:String) {
  val parsed=java.net.URI(url.trim())
  require(parsed.scheme=="https" && parsed.host==java.net.URI(BuildConfig.SUPABASE_URL).host && parsed.port==-1 && parsed.userInfo==null && parsed.rawQuery==null && parsed.fragment==null && (parsed.path.isNullOrEmpty() || parsed.path=="/")){"المشروع مش مضبوط"}
  require(key.startsWith("sb_publishable_") || key.startsWith("eyJ")){"حط المفتاح العام"}
  if(key.startsWith("eyJ")) {
   val claims=runCatching{Json.parseToJsonElement(String(android.util.Base64.decode(key.split('.')[1],android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))).jsonObject}.getOrNull()
   require(claims?.get("role")?.jsonPrimitive?.content=="anon"){"لا تحط إداري"}
  }
  clear();config.edit().putString("url",url.trim().trimEnd('/')).putString("key",key.trim()).apply()
 }

 private val client=OkHttpClient.Builder().callTimeout(150,TimeUnit.SECONDS).readTimeout(100,TimeUnit.SECONDS).build()
 private val mutex=Mutex()
 private var stored:JsonObject?=store.read()?.let{runCatching{Json.parseToJsonElement(it).jsonObject}.getOrNull()}
 val hasSession:Boolean get()=stored!=null
 val cachedProfile:JsonObject? get()=stored?.get("profile")?.jsonObject
 private fun save(data:JsonObject) { stored=data;store.save(data.toString()) }
 fun clear() { stored=null;store.clear() }
 private fun request(function:String,payload:RequestBody,token:String?=null,query:String="",extra:Map<String,String> = emptyMap()):Response {
  check(endpoint.startsWith("https://")) { "كمّل إعداد الاتصال" }
  return client.newCall(Request.Builder().url("${endpoint.trimEnd('/')}/functions/v1/$function$query")
   .header("apikey",anonKey).apply { token?.let{header("Authorization","Bearer $it")}; extra.forEach{(k,v)->header(k,v)} }.post(payload).build()).execute()
 }
 private fun parse(response:Response):JsonObject = response.use {
  val data=runCatching{Json.parseToJsonElement(it.body?.string()?:"{}").jsonObject}.getOrDefault(buildJsonObject{})
  if(!it.isSuccessful) throw ApiException(it.code,data["error"]?.jsonPrimitive?.content ?: "فشل الاتصال (${it.code})")
  data
 }
 private fun jsonBody(data:JsonObject)=data.toString().toRequestBody("application/json".toMediaType())
 private suspend fun token(force:Boolean=false):String = mutex.withLock {
  val data=stored ?: throw ApiException(401,"ادخل أول")
  val s=data.getValue("session").jsonObject
  val expires=s["expires_at"]?.jsonPrimitive?.long ?: 0L
  if(force || expires<System.currentTimeMillis()/1000+90) {
   try {
    val next=withContext(Dispatchers.IO){parse(request("account",jsonBody(buildJsonObject{put("action","refresh");put("refresh_token",s.getValue("refresh_token"))})))}
    save(next)
   } catch(e:ApiException) { if(e.code==401 || e.code==403) clear();throw e }
  }
  stored!!.getValue("session").jsonObject.getValue("access_token").jsonPrimitive.content
 }
 suspend fun call(function:String,payload:JsonObject=buildJsonObject{},authenticated:Boolean=true):JsonObject = withContext(Dispatchers.IO) {
  val t=if(authenticated)token() else null
  var response=request(function,jsonBody(payload),t)
  if(authenticated && response.code==401) { response.close();response=request(function,jsonBody(payload),token(true)) }
  parse(response)
 }
 suspend fun login(name:String,password:String):JsonObject {
  val data=call("auth-login",buildJsonObject{put("username",name);put("password",password)},false);save(data);return data.getValue("profile").jsonObject
 }
 suspend fun download(payload:JsonObject):Response=withContext(Dispatchers.IO) {
  var res=request("downloads",jsonBody(payload),token())
  if(res.code==401){res.close();res=request("downloads",jsonBody(payload),token(true))}
  if(!res.isSuccessful){parse(res);error("Download failed")};res
 }
 suspend fun upload(repo:String,path:String,body:RequestBody,expected:String?):JsonObject=withContext(Dispatchers.IO) {
  val q=HttpUrl.Builder().scheme("https").host("vault.invalid").addQueryParameter("repository_id",repo).addQueryParameter("path",path).build().encodedQuery
  val headers=expected?.let{mapOf("x-expected-sha" to it)}?:emptyMap()
  var res=request("uploads",body,token(),"?$q",headers)
  if(res.code==401){res.close();res=request("uploads",body,token(true),"?$q",headers)}
  parse(res)
 }
}

fun Throwable.userMessage():String = when {
 this is java.io.IOException -> "النت مش شغال"
 this is ApiException -> when(code) {
  401 -> if(message?.contains("Current password")==true)"الباسورد مش صح" else "راجع بياناتك"
  403 -> if(message?.contains("Password change")==true)"غيّر الباسورد أول" else "حسابك مش مفعّل"
  404 -> "مش موجود"
  409 -> "الملف اتغيّر"
  413 -> "تعدّيت الحد"
  429 -> "اهدأ شوي"
  502 -> "GitHub مشغول"
  400 -> "راجع اللي كتبت"
  else -> "ما ضبطت العملية"
 }
 else -> message?:"ما ضبطت"
}

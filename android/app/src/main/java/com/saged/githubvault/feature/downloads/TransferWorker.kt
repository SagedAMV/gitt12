package com.saged.githubvault.feature.downloads

import android.content.Context
import android.net.Uri
import androidx.work.*
import androidx.room.withTransaction
import com.saged.githubvault.app.VaultApplication
import com.saged.githubvault.core.database.*
import com.saged.githubvault.core.security.SafeZip
import com.saged.githubvault.core.network.ApiException
import com.saged.githubvault.core.network.userMessage
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okio.BufferedSink
import java.io.File
import java.util.UUID

class TransferWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
 override suspend fun doWork():Result=withContext(Dispatchers.IO) {
  val app=applicationContext as VaultApplication
  val id=inputData.getString("id")?:return@withContext Result.failure()
  val dao=app.database.transfers();val t=dao.get(id)?:return@withContext Result.failure()
  val b=Json.parseToJsonElement(t.payload).jsonObject
  try {
   dao.update(id,"RUNNING",0f)
   if(t.kind=="DOWNLOAD") {
    val root=File(applicationContext.filesDir,"downloads/$id-${UUID.randomUUID()}");root.mkdirs()
    val partial=File(root,"payload.part")
    try {
     val raw=b["format"]?.jsonPrimitive?.content=="file"
     var sha=""
     app.api.download(b).use {response ->
      sha=response.header("X-Vault-Sha")?:""
      val body=response.body?:error("مافي بيانات")
      val total=body.contentLength();var count=0L;var last=0L
      body.byteStream().use {input -> partial.outputStream().use {out ->
       val buffer=ByteArray(65536)
       while(true) {
        ensureActive(); val n=input.read(buffer);if(n<0)break;count+=n
        check(count<=210L*1024*1024){"الملف كبير مرّة"};out.write(buffer,0,n)
        if(count-last>512*1024) {last=count;dao.update(id,"RUNNING",if(total>0)(count.toFloat()/total).coerceAtMost(.99f) else .1f)}
       }
      } }
     }
     val repo=b.getValue("repository_id").jsonPrimitive.content
     if(raw) {
      val path=b.getValue("paths").jsonArray.first().jsonPrimitive.content
      val dest=File(root,UUID.randomUUID().toString()+"-"+path.substringAfterLast('/'))
      check(partial.renameTo(dest))
      app.database.files().put(LocalFile("$repo:$path",repo,path,dest.path,sha,System.currentTimeMillis()))
     } else {
      val extracted=File(root,"extracted")
      SafeZip.extract(partial.inputStream(),extracted)
      val versions=app.api.call("files",buildJsonObject{put("repository_id",repo);put("action","versions");put("ref",sha)})["versions"]?.jsonObject
      app.database.withTransaction {
      extracted.walkTopDown().filter{it.isFile}.forEach {f ->
       val path=f.relativeTo(extracted).invariantSeparatorsPath
       val version=versions?.get(path)?.jsonPrimitive?.content?:""
       app.database.files().put(LocalFile("$repo:$path",repo,path,f.path,version,System.currentTimeMillis()))
      }
      }
      partial.delete()
     }
    } catch(e:Exception) {root.deleteRecursively();throw e}
   } else if(t.kind=="MKDIR") {
    app.api.call("admin-items",b)
   } else {
    val uri=Uri.parse(b.getValue("uri").jsonPrimitive.content)
    val size=b["size"]?.jsonPrimitive?.long?:-1
    val request=object:RequestBody() {
     override fun contentType()="application/octet-stream".toMediaType()
     override fun contentLength()=size
     override fun writeTo(sink:BufferedSink) {
      applicationContext.contentResolver.openInputStream(uri)!!.use {input ->
       var count=0L;var reported=0L;val buffer=ByteArray(65536)
       while(true) {if(isStopped) throw java.io.IOException("Cancelled");val n=input.read(buffer);if(n<0)break;count+=n;check(count<=8L*1024*1024){"حدّه 8MiB"};sink.write(buffer,0,n);if(count-reported>=256*1024){reported=count;runBlocking{dao.update(id,"RUNNING",if(size>0)(count.toFloat()/size).coerceAtMost(.99f) else .1f)}}}
      }
     }
    }
    app.api.upload(t.repositoryId,b.getValue("path").jsonPrimitive.content,request,b["expected_sha"]?.jsonPrimitive?.content)
   }
   dao.update(id,"SUCCEEDED",1f);Result.success()
  } catch(e:CancellationException) { withContext(NonCancellable){dao.update(id,"CANCELLED",0f,"وقفناها")};throw e }
  catch(e:Exception) {
   val transient=e is java.io.IOException || e is ApiException && (e.code>=500 || e.code==429 || e.code==409)
   val retry=transient && runAttemptCount<3
   dao.update(id,if(retry)"RETRYING" else "FAILED",0f,e.userMessage())
   if(retry)Result.retry() else Result.failure()
  }
 }
 companion object {
  fun enqueue(context:Context,id:String) {
   val request=OneTimeWorkRequestBuilder<TransferWorker>().setInputData(workDataOf("id" to id))
    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,java.util.concurrent.TimeUnit.SECONDS).addTag("vault-transfer").build()
   WorkManager.getInstance(context).enqueueUniqueWork(id,ExistingWorkPolicy.KEEP,request)
  }
 }
}

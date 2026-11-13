package com.saged.githubvault.feature.github
import android.content.Context
import android.net.Uri
import androidx.work.*
import com.saged.githubvault.app.VaultApplication
import com.saged.githubvault.core.database.LocalFile
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.io.File
import java.io.ByteArrayOutputStream
import java.util.UUID

class PublicTransferWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
 override suspend fun getForegroundInfo():ForegroundInfo {
  val manager=applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
  if(android.os.Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(android.app.NotificationChannel("public-transfers","نقل ملفاتك",android.app.NotificationManager.IMPORTANCE_LOW))
  val notification=androidx.core.app.NotificationCompat.Builder(applicationContext,"public-transfers").setSmallIcon(com.saged.githubvault.R.drawable.ic_vault).setContentTitle("بننقل ملفاتك").setContentText("خليك متصل").setOngoing(true).build()
  return if(android.os.Build.VERSION.SDK_INT>=29)ForegroundInfo(id.hashCode(),notification,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)else ForegroundInfo(id.hashCode(),notification)
 }
 override suspend fun doWork():Result=withContext(Dispatchers.IO) {
  val app=applicationContext as VaultApplication;val id=inputData.getString("id")?:return@withContext Result.failure()
  val dao=app.database.transfers();val task=dao.get(id)?:return@withContext Result.failure()
  val b=Json.parseToJsonElement(task.payload).jsonObject
  fun text(k:String)=b.getValue(k).jsonPrimitive.content
  var partial:File?=null
  try {
   setForeground(getForegroundInfo())
   dao.update(id,"RUNNING",0f)
   val repo=text("repo");val path=text("path")
   GitHubPolicy.repo(repo);GitHubPolicy.path(path);require(path.isNotEmpty())
   if(task.kind=="PUBLIC_DOWNLOAD") {
    val folder=File(app.filesDir,"downloads/public-${UUID.randomUUID()}");check(folder.mkdirs())
    val temp=File(folder,"payload.part");partial=temp
    app.github.blob(repo,text("sha")).use{r->
     val body=r.body?:error("مافي بيانات");var copied=0L;var reported=0L
     val total=b["size"]?.jsonPrimitive?.long?:body.contentLength()
     body.byteStream().use{input->temp.outputStream().use{out->val buffer=ByteArray(65536);while(true){ensureActive();val n=input.read(buffer);if(n<0)break;copied+=n;check(copied<=90L*1024*1024){"الملف فوق الحد"};out.write(buffer,0,n);if(copied-reported>=256*1024){reported=copied;dao.update(id,"RUNNING",if(total>0)(copied.toFloat()/total).coerceAtMost(.99f) else .1f)}}}}
    }
    val final=File(temp.parentFile,path.substringAfterLast('/'));check(temp.renameTo(final));partial=final
    val key="public:saged7/$repo"
    app.database.files().put(LocalFile("$key:$path",key,path,final.path,text("sha"),System.currentTimeMillis()))
    partial=null
   } else {
    val staged=File.createTempFile("github-upload-",".bin",app.cacheDir);partial=staged
    app.contentResolver.openInputStream(Uri.parse(text("uri")))!!.use{input->staged.outputStream().use{out->val buffer=ByteArray(65536);var copied=0L;while(true){ensureActive();val n=input.read(buffer);if(n<0)break;copied+=n;check(copied<=GitHubPolicy.UPLOAD_LIMIT){"الملف فوق الحد"};out.write(buffer,0,n)}}}
    dao.update(id,"RUNNING",.1f)
    var reported=0L
    app.github.upload(repo,path,text("branch"),staged,b["expected"]?.jsonPrimitive?.content){sent,total->
     if(isStopped)throw java.io.InterruptedIOException()
     if(sent-reported>=1024*1024){reported=sent;kotlinx.coroutines.runBlocking{dao.update(id,"RUNNING",(.1f+.85f*sent/total.coerceAtLeast(1)).coerceAtMost(.95f))}}
    }
    staged.delete();partial=null
   }
   dao.update(id,"SUCCEEDED",1f);Result.success()
  } catch(e:CancellationException) {partial?.delete();withContext(NonCancellable){dao.update(id,"CANCELLED",0f,"وقفناها")};throw e}
  catch(e:Exception) {partial?.delete();val retry=(e is java.io.IOException || e is GitHubFailure&&e.status>=500)&&runAttemptCount<3;dao.update(id,if(retry)"RETRYING" else "FAILED",0f,if(e is GitHubFailure)e.message else "ما ضبطت العملية");if(retry)Result.retry() else Result.failure()}
 }
 companion object {
  fun enqueue(context:Context,id:String,upload:Boolean=false) {WorkManager.getInstance(context).enqueueUniqueWork(id,ExistingWorkPolicy.KEEP,OneTimeWorkRequestBuilder<PublicTransferWorker>().setInputData(workDataOf("id" to id)).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,java.util.concurrent.TimeUnit.SECONDS).addTag("public-transfer").apply{if(upload)addTag("public-upload")}.build())}
 }
}

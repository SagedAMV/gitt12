package com.saged.githubvault.feature.downloads
import android.content.Context
import androidx.work.*
import com.saged.githubvault.app.VaultApplication
import com.saged.githubvault.core.network.ApiException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*
class SyncWorker(context:Context,parameters:WorkerParameters):CoroutineWorker(context,parameters) {
 override suspend fun doWork():Result {
  val app=applicationContext as VaultApplication
  return try {
   for((repo,files) in app.database.files().all().groupBy{it.repositoryId}) {
    try {
     val data=if(repo.startsWith("public:saged7/"))app.github.versions(repo.removePrefix("public:saged7/")) else {
      if(!app.api.hasSession)continue
      app.api.call("files",buildJsonObject{put("repository_id",repo);put("action","versions")}).getValue("versions").jsonObject
     }
     for(f in files){val sha=data[f.relativePath]?.jsonPrimitive?.content;app.database.files().flags(f.id,f.sha,sha==null,sha!=null&&sha!=f.sha)}
    } catch(e:ApiException){if(e.code !in listOf(401,403,404))throw e}
   };Result.success()
  } catch(e:CancellationException){throw e} catch(_:Exception){if(runAttemptCount<3)Result.retry() else Result.failure()}
 }
 companion object {
  fun enqueue(context:Context) {WorkManager.getInstance(context).enqueueUniqueWork("local-version-check",ExistingWorkPolicy.KEEP,OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())}
 }
}

package com.saged.githubvault.feature.github

import java.io.File
import kotlinx.serialization.json.*

/** الجانب الشبكي الذي يحتاجه المنفذ؛ يفصل التنقل عن GitHubClient حتى يُختبر بذاكرة نقيّة. */
interface TreeApi {
 suspend fun currentSha(repo:String,path:String):String?
 suspend fun blobToFile(repo:String,sha:String,dest:File)
 suspend fun writeFile(repo:String,path:String,branch:String,file:File,sha:String?,message:String)
 suspend fun removeFile(repo:String,path:String,branch:String,sha:String)
}

/** ربط حقيقي بـ GitHubClient: نسخ blob خام إلى ملف مؤقت ثم رفعه بنفس التدفق المحدد الذاكرة. */
class GitHubTreeApi(private val client:GitHubClient):TreeApi {
 override suspend fun currentSha(repo:String,path:String):String?=runCatching{client.file(repo,path)["sha"]?.jsonPrimitive?.content}.getOrNull()
 override suspend fun blobToFile(repo:String,sha:String,dest:File) {
  client.blob(repo,sha).use{r->val input=r.body?.byteStream()?:error("مافي بيانات");dest.outputStream().use{out->input.copyTo(out)}}
 }
 override suspend fun writeFile(repo:String,path:String,branch:String,file:File,sha:String?,message:String)=client.writeFile(repo,path,branch,file,sha,message)
 override suspend fun removeFile(repo:String,path:String,branch:String,sha:String)=client.removeFile(repo,path,branch,sha)
}

/** ينفّذ خطوات TreeOp بالترتيب مع تقدير تقدم؛ فشل أي خطوة يوقف السلسلة كاملة ويرمي للـ worker.
*  PUT يلتقط sha الملف الموجود أولاً (upsert) ويقفز إذا كان المحتوى مطابقاً، فتصبح إعادة المحاولة بعد فشل جزئي آمنة ورخيصة. */
class TreeOpRunner(private val api:TreeApi,private val tempDir:File) {
 suspend fun run(ops:List<TreeOp>,onProgress:(Int,Int)->Unit={_,_->}) {
  require(ops.isNotEmpty()){"مافي شي جديد"}
  require(ops.size<=FileOps.OPS_LIMIT){"الكثير مرّة"}
  for((index,op) in ops.withIndex()){
   when(op.kind) {
    "PUT"->{
     val staging=File.createTempFile("tree-op-",".bin",tempDir)
     try {
      val src=op.srcSha
      val expected=api.currentSha(op.repo,op.path)
      if(expected==null || src==null || expected!=src) {
       if(src==null)staging.writeBytes(ByteArray(0)) else {
        GitHubPolicy.sha(src)
        api.blobToFile(requireNotNull(op.srcRepo),src,staging)
       }
       require(staging.length()<=GitHubPolicy.UPLOAD_LIMIT){"الملف فوق الحد"}
       api.writeFile(op.repo,op.path,op.branch,staging,expected,"Update ${op.path} from GitHub Vault")
      }
     } finally {staging.delete()}
    }
    "DELETE"->{
     val sha=op.sha?:throw IllegalArgumentException("النسخة مش مضبوطة")
     GitHubPolicy.sha(sha)
     api.removeFile(op.repo,op.path,op.branch,sha)
    }
    else->throw IllegalArgumentException("المسار مش مضبوط")
   }
   onProgress(index+1,ops.size)
  }
 }
}

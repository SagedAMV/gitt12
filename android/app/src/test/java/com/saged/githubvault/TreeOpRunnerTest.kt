package com.saged.githubvault
import com.saged.githubvault.feature.github.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.io.File

/** عقد منفّذ خطوات الشجرة: ترتيب PUT→DELETE، upsert لإعادة المحاولة الآمنة، تقدم متقدم، وتنظيف الملفات المؤقتة. */
class TreeOpRunnerTest {
 private class RecordingApi:TreeApi {
  val log=ArrayList<String>();val tempPaths=ArrayList<String>();val existing=HashMap<String,String>()
  override suspend fun currentSha(repo:String,path:String):String?{log+="SHA:$repo/$path";return existing["$repo/$path"]}
  override suspend fun blobToFile(repo:String,sha:String,dest:File){dest.writeBytes(ByteArray(16){1});log+="BLOB:$repo/$sha";tempPaths+=dest.absolutePath}
  override suspend fun writeFile(repo:String,path:String,branch:String,file:File,sha:String?,message:String){log+="PUT:$repo/$path/${sha?:"new"}";existing["$repo/$path"]=sha?:"new"}
  override suspend fun removeFile(repo:String,path:String,branch:String,sha:String){log+="DEL:$repo/$path";existing.remove("$repo/$path")}
 }
 private fun tempDir():File=Files.createTempDirectory("tree-runner").toFile()
 private fun moveOps()=listOf(
  TreeOp("PUT","r","d/b.txt","main",srcRepo="r",srcSha="a".repeat(40),label="d/b.txt"),
  TreeOp("DELETE","r","s/a.txt","main",sha="b".repeat(40)))

 @Test fun moveWritesThenDeletesInOrder(){
  val api=RecordingApi();val dir=tempDir()
  try{runBlocking{TreeOpRunner(api,dir).run(moveOps())}}finally{dir.deleteRecursively()}
  assertEquals(listOf("SHA:r/d/b.txt","BLOB:r/"+"a".repeat(40),"PUT:r/d/b.txt/new","DEL:r/s/a.txt"),api.log)
 }
 @Test fun copyHasNoDeleteStep(){
  val api=RecordingApi();val dir=tempDir()
  try{runBlocking{TreeOpRunner(api,dir).run(listOf(TreeOp("PUT","r","d/b.txt","main",srcRepo="r",srcSha="a".repeat(40))))}}finally{dir.deleteRecursively()}
  assertEquals(listOf("SHA:r/d/b.txt","BLOB:r/"+"a".repeat(40),"PUT:r/d/b.txt/new"),api.log)
 }
 @Test fun upsertReusesExistingShaSoRetryIsSafe(){
  val api=RecordingApi();api.existing["r/d/b.txt"]="e".repeat(40);val dir=tempDir()
  try{runBlocking{TreeOpRunner(api,dir).run(moveOps())}}finally{dir.deleteRecursively()}
  assertTrue("expected upsert sha",api.log.contains("PUT:r/d/b.txt/"+"e".repeat(40)))
 }
 @Test fun identicalUploadIsSkippedForRetry(){
  val api=RecordingApi();api.existing["r/d/b.txt"]="a".repeat(40);val dir=tempDir()
  try{runBlocking{TreeOpRunner(api,dir).run(moveOps())}}finally{dir.deleteRecursively()}
  assertEquals("identical content must be skipped",listOf("SHA:r/d/b.txt","DEL:r/s/a.txt"),api.log)
 }
 @Test fun mkdirWritesEmptyFileWithoutBlob(){
  val api=RecordingApi();val dir=tempDir()
  try{runBlocking{TreeOpRunner(api,dir).run(listOf(TreeOp("PUT","r","folder/.gitkeep","main")))}}finally{dir.deleteRecursively()}
  assertEquals(listOf("SHA:r/folder/.gitkeep","PUT:r/folder/.gitkeep/new"),api.log)
  assertTrue("no blob read for empty file",api.log.none{it.startsWith("BLOB:")})
 }
 @Test fun failureStopsTheChain(){
  val api=RecordingApi();val dir=tempDir()
  val failing=object:TreeApi by api{
   override suspend fun blobToFile(repo:String,sha:String,dest:File){if(sha=="d".repeat(40))throw java.io.IOException("نت مقطوع") else api.blobToFile(repo,sha,dest)}
  }
  val ops=listOf(
   TreeOp("PUT","r","ok.txt","main",srcRepo="r",srcSha="a".repeat(40)),
   TreeOp("PUT","r","boom.txt","main",srcRepo="r",srcSha="d".repeat(40)),
   TreeOp("DELETE","r","tail.txt","main",sha="b".repeat(40)))
  var progress=0
  try{runBlocking{TreeOpRunner(failing,dir).run(ops){done,_->progress=done}};fail("must propagate")}
  catch(e:java.io.IOException){assertEquals("نت مقطوع",e.message)}
  finally{dir.deleteRecursively()}
  assertEquals("chain stopped after first success",1,progress)
  assertEquals(listOf("SHA:r/ok.txt","BLOB:r/"+"a".repeat(40),"PUT:r/ok.txt/new","SHA:r/boom.txt"),api.log)
 }
 @Test fun progressIsMonotonic(){
  val api=RecordingApi();val dir=tempDir();val steps=ArrayList<Pair<Int,Int>>()
  try{runBlocking{TreeOpRunner(api,dir).run(moveOps()){done,total->steps+=done to total}}}finally{dir.deleteRecursively()}
  assertEquals(listOf(1 to 2,2 to 2),steps)
 }
 @Test fun tempFilesAreCleanedUp(){
  val api=RecordingApi();val dir=tempDir()
  try{runBlocking{TreeOpRunner(api,dir).run(moveOps())}}finally{dir.deleteRecursively()}
  assertTrue(api.tempPaths.isNotEmpty());assertTrue(api.tempPaths.all{!File(it).exists()})
 }
 @Test fun overLimitIsRejected(){
  val api=RecordingApi();val dir=tempDir()
  val many=(1..FileOps.OPS_LIMIT+1).map{TreeOp("DELETE","r","f$it.txt","main",sha="b".repeat(40))}
  try{runBlocking{TreeOpRunner(api,dir).run(many)};fail("must reject")}catch(e:IllegalArgumentException){assertEquals("الكثير مرّة",e.message)}finally{dir.deleteRecursively()}
 }
 @Test fun deleteWithoutShaIsRejected(){
  val api=RecordingApi();val dir=tempDir()
  try{runBlocking{TreeOpRunner(api,dir).run(listOf(TreeOp("DELETE","r","p.txt","main",sha=null)))};fail("must reject")}
  catch(e:IllegalArgumentException){assertEquals("النسخة مش مضبوطة",e.message)}finally{dir.deleteRecursively()}
 }
 @Test fun deleteWithBadShaIsRejected(){
  val api=RecordingApi();val dir=tempDir()
  try{runBlocking{TreeOpRunner(api,dir).run(listOf(TreeOp("DELETE","r","p.txt","main",sha="short")))};fail("must reject")}
  catch(e:IllegalArgumentException){assertEquals("النسخة مش مضبوطة",e.message)}finally{dir.deleteRecursively()}
 }
}

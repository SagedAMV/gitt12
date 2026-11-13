package com.saged.githubvault
import com.saged.githubvault.feature.github.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

/** عقد مخطط عمليات الشجرة: بنيوي صرف، بلا شبكة ولا أندرويد، فيصير تحت كومة 64MiB. */
class FileOpsTest {
 private fun fileItem(path:String,sha:String?=null,repo:String="sageer")=ClipItem(repo,path,"file","main",sha?:("a".repeat(40)),10L)
 private fun dirItem(path:String,repo:String="sageer")=ClipItem(repo,path,"dir","main",null,0L)

 @Test fun singleSegmentNames(){
  assertEquals("docs",FileOps.name(" docs "))
  assertEquals("ملف.txt",GitHubPolicy.name("ملف.txt"))
  for(bad in listOf("","  ",".","..","a/b","a\\b","a:b","a\u0000b","x".repeat(101)))reject{FileOps.name(bad)}
 }
 @Test fun uniqueNamesKeepExtension(){
  assertEquals("report (1).pdf",FileOps.unique(setOf("report.pdf"),"report.pdf"))
  assertEquals("report (2).pdf",FileOps.unique(setOf("report.pdf","report (1).pdf"),"report.pdf"))
  assertEquals("file (1)",FileOps.unique(setOf("file"),"file"))
  assertEquals("a.tar (1).gz",FileOps.unique(setOf("a.tar.gz"),"a.tar.gz"))
  assertEquals("new.txt",FileOps.unique(setOf("other.txt"),"new.txt"))
 }
 @Test fun pastePreservesFolderStructure(){
  val clip=Clip(ClipMode.COPY,listOf(dirItem("docs")))
  val sources=listOf(PasteSource(dirItem("docs"),listOf(fileItem("docs/sub/a.txt"),fileItem("docs/b.txt"))))
  val ops=FileOps.paste(clip,"vault2","","main",emptySet(),sources)
  assertEquals(2,ops.size)
  assertTrue(ops.all{it.kind=="PUT" && it.repo=="vault2" && it.srcRepo=="sageer"})
  assertEquals("docs/sub/a.txt",ops[0].path);assertEquals("a".repeat(40),ops[0].srcSha)
  assertEquals("docs/b.txt",ops[1].path)
 }
 @Test fun cutPasteAddsDeleteForSource(){
  val clip=Clip(ClipMode.CUT,listOf(fileItem("a.txt")))
  val ops=FileOps.paste(clip,"sageer","","main",setOf("a.txt"),listOf(PasteSource(clip.items[0],emptyList())))
  assertEquals(2,ops.size)
  assertEquals("a (1).txt",ops[0].path);assertEquals("PUT",ops[0].kind)
  assertEquals("a.txt",ops[1].path);assertEquals("DELETE",ops[1].kind);assertEquals("a".repeat(40),ops[1].sha)
 }
 @Test fun pasteIntoSameSpotIsSkipped(){
  val clip=Clip(ClipMode.COPY,listOf(fileItem("a.txt")))
  rejectWith(clip,emptySet(),listOf(PasteSource(clip.items[0],emptyList())),"مافي شي جديد")
  val dirClip=Clip(ClipMode.COPY,listOf(dirItem("docs")))
  rejectWith(dirClip,emptySet(),listOf(PasteSource(dirItem("docs"),listOf(fileItem("docs/a.txt")))),"مافي شي جديد")
 }
 private fun rejectWith(clip:Clip,existing:Set<String>,sources:List<PasteSource>,expected:String){
  try{FileOps.paste(clip,"sageer","","main",existing,sources);fail("must reject")}catch(e:IllegalArgumentException){assertEquals(expected,e.message)}
 }
 @Test fun deleteFileUsesShaAndFolderExpandsTree(){
  val tree=mapOf("docs/a.txt" to "b".repeat(40),"docs/sub/c.txt" to "c".repeat(40),"other.txt" to "d".repeat(40))
  val ops=FileOps.delete(listOf(dirItem("docs")),"sageer","main",tree)
  assertEquals(2,ops.size);assertTrue(ops.all{it.kind=="DELETE" && it.repo=="sageer" && it.branch=="main"})
  assertEquals("docs/a.txt",ops[0].path);assertEquals("docs/sub/c.txt",ops[1].path);assertEquals("c".repeat(40),ops[1].sha)
  val single=FileOps.delete(listOf(fileItem("other.txt","d".repeat(40))),"sageer","main",emptyMap())
  assertEquals(1,single.size);assertEquals("d".repeat(40),single[0].sha)
 }
 @Test fun deleteMissingFolderIsRejected(){
  try{FileOps.delete(listOf(dirItem("nope")),"sageer","main",emptyMap());fail("must reject")}catch(e:IllegalArgumentException){assertEquals("مش موجود",e.message)}
 }
 @Test fun renameFileProducesPutDeletePair(){
  val ops=FileOps.rename(fileItem("old/name.txt"),"new.txt","main",emptyMap())
  assertEquals(2,ops.size)
  assertEquals("old/new.txt",ops[0].path);assertEquals("old/name.txt",ops[1].path)
  assertEquals("PUT",ops[0].kind);assertEquals("DELETE",ops[1].kind)
 }
 @Test fun renameFolderMovesWholePrefix(){
  val tree=mapOf("docs/a.txt" to "b".repeat(40),"docs/sub/c.txt" to "c".repeat(40))
  val ops=FileOps.rename(dirItem("docs"),"docs2","main",tree)
  assertEquals(4,ops.size)
  assertEquals("docs2/a.txt",ops[0].path);assertEquals("DELETE",ops[1].kind)
  assertEquals("docs2/sub/c.txt",ops[2].path);assertEquals("docs/sub/c.txt",ops[3].path)
 }
 @Test fun renameToSameNameIsNoOp(){assertEquals(0,FileOps.rename(fileItem("a.txt"),"a.txt","main",emptyMap()).size)}
 @Test fun mkdirUsesGitkeep(){
  val ops=FileOps.mkdir("sageer","folder","main")
  assertEquals(1,ops.size);assertEquals("folder/.gitkeep",ops[0].path);assertNull(ops[0].srcSha);assertEquals("PUT",ops[0].kind)
 }
 @Test fun opsRoundTripThroughJson(){
  val ops=listOf(
   TreeOp("PUT","r","p/x.txt","main",srcRepo="r2",srcSha="a".repeat(40),size=5L,label="p/x.txt"),
   TreeOp("DELETE","r","q.bin","main",sha="b".repeat(40),label="q.bin"))
  assertEquals(ops,FileOps.decodeOps(FileOps.encodeOps(ops)))
  val element=Json.parseToJsonElement(FileOps.encodeOps(ops))
  assertTrue(element is kotlinx.serialization.json.JsonArray)
 }
 @Test fun opsAreBounded(){
  val many=(1..FileOps.OPS_LIMIT+1).map{fileItem("f$it.txt")}
  try{FileOps.delete(many,"sageer","main",emptyMap());fail("must reject")}catch(e:IllegalArgumentException){assertEquals("الكثير مرّة",e.message)}
 }
 @Test fun crossRepoPasteCarriesSourceRepo(){
  val clip=Clip(ClipMode.COPY,listOf(fileItem("a.txt",repo="other")))
  val ops=FileOps.paste(clip,"sageer","","main",emptySet(),listOf(PasteSource(clip.items[0],emptyList())))
  assertEquals(1,ops.size);assertEquals("other",ops[0].srcRepo);assertEquals("sageer",ops[0].repo)
 }
 @Test fun uniqueForkNames(){
  assertEquals("sageer-copy",FileOps.uniqueFork(setOf("other"),"sageer"))
  assertEquals("sageer-copy-2",FileOps.uniqueFork(setOf("sageer-copy"),"sageer"))
  assertEquals("sageer-copy-3",FileOps.uniqueFork(setOf("sageer-copy","sageer-copy-2"),"sageer"))
 }
 private fun reject(block:()->Unit){try{block();fail("Expected rejection")}catch(_:IllegalArgumentException){}}
}

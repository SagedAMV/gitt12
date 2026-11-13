package com.saged.githubvault
import com.saged.githubvault.core.security.SafeZip
import org.junit.Test
import org.junit.Assert.*
import java.io.*
import java.nio.file.Files
import java.util.zip.*
class SafeZipTest {
 private fun archive(name:String):InputStream { val bytes=ByteArrayOutputStream(); ZipOutputStream(bytes).use { it.putNextEntry(ZipEntry(name)); it.write("ok".toByteArray()); it.closeEntry() }; return bytes.toByteArray().inputStream() }
 @Test fun extractsFile() { val parent=Files.createTempDirectory("vault").toFile(); try { val dest=File(parent,"output"); SafeZip.extract(archive("folder/test.txt"),dest); assertEquals("ok",File(dest,"folder/test.txt").readText()) } finally { parent.deleteRecursively() } }
 @Test fun blocksTraversal() { val parent=Files.createTempDirectory("vault").toFile(); try { val dest=File(parent,"output"); try { SafeZip.extract(archive("../escape.txt"),dest); fail("Traversal accepted") } catch(_:IllegalArgumentException) { assertFalse(dest.exists()); assertFalse(File(parent,"escape.txt").exists()) } } finally { parent.deleteRecursively() } }
 @Test fun blocksExpansion() { val parent=Files.createTempDirectory("vault").toFile(); try { try { SafeZip.extract(archive("test.txt"),File(parent,"output"),maxBytes=1); fail("Size accepted") } catch(_:IllegalArgumentException) { assertFalse(File(parent,"output").exists()) } } finally { parent.deleteRecursively() } }

 @Test fun blocksAbsolutePath() = rejectArchive("/absolute.txt")
 @Test fun blocksWindowsPath() = rejectArchive("C:\\outside.txt")
 @Test fun blocksBackslash() = rejectArchive("folder\\file.txt")
 @Test fun blocksDotSegment() = rejectArchive("folder/./file.txt")
 @Test fun respectsFileCount() { val parent=Files.createTempDirectory("vault").toFile(); try { try { SafeZip.extract(archive("test.txt"),File(parent,"output"),maxFiles=0); fail("Count accepted") } catch(_:IllegalArgumentException) { assertFalse(File(parent,"output").exists()) } } finally {parent.deleteRecursively()} }
 @Test fun rejectsGarbage() { val parent=Files.createTempDirectory("vault").toFile(); try { try {SafeZip.extract("not a zip".byteInputStream(),File(parent,"output"));fail("Garbage accepted")}catch(_:IllegalArgumentException){assertFalse(File(parent,"output").exists())} } finally{parent.deleteRecursively()} }
 @Test fun extractsUnicode() {val parent=Files.createTempDirectory("vault").toFile();try{val dest=File(parent,"output");SafeZip.extract(archive("مجلد/ملف.txt"),dest);assertEquals("ok",File(dest,"مجلد/ملف.txt").readText())}finally{parent.deleteRecursively()}}
 private fun rejectArchive(path:String) { val parent=Files.createTempDirectory("vault").toFile();try{val dest=File(parent,"output");try{SafeZip.extract(archive(path),dest);fail("Path accepted")}catch(_:IllegalArgumentException){assertFalse(dest.exists())}}finally{parent.deleteRecursively()} }
}

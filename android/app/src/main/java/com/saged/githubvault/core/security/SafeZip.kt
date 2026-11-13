package com.saged.githubvault.core.security

import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

object SafeZip {
 fun extract(input:InputStream, destination:File, maxFiles:Int=1000, maxBytes:Long=200L*1024*1024) {
  require(!destination.exists()) { "Use a fresh destination" }
  check(destination.mkdirs())
  val root=destination.canonicalFile
  try {
   ZipInputStream(input.buffered()).use { zip ->
    var files=0; var bytes=0L
    val seen=mutableSetOf<String>()
    while(true) {
     val entry=zip.nextEntry ?: break
     require(++files<=maxFiles) { "Too many ZIP entries" }
     require(!entry.name.startsWith("/") && !entry.name.contains('\\') && !entry.name.contains(':')) { "Invalid ZIP path" }
     require(entry.name.split('/').none { it==".." || it=="." })
     val target=File(root,entry.name).canonicalFile
     require(target.path.startsWith(root.path+File.separator) && seen.add(target.path)) { "Unsafe or duplicate ZIP entry" }
     if(entry.isDirectory) { check(target.mkdirs() || target.isDirectory) } else {
      check(target.parentFile!!.mkdirs() || target.parentFile!!.isDirectory)
      target.outputStream().use { out ->
       val buffer=ByteArray(8192)
       while(true) { val n=zip.read(buffer); if(n<0) break; bytes+=n; require(bytes<=maxBytes) { "ZIP size limit exceeded" }; out.write(buffer,0,n) }
      }
     }
     zip.closeEntry()
    }
    require(files>0) { "Empty or invalid archive" }
   }
  } catch(e:Exception) { destination.deleteRecursively(); throw e }
 }
}

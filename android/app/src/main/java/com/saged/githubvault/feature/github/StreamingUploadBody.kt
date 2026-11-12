package com.saged.githubvault.feature.github
import java.io.File
import java.io.FilterOutputStream
import java.util.Base64
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okio.BufferedSink

/** Bounded-memory binary -> Base64 -> HTTP streaming. */
class StreamingUploadBody(private val file:File,path:String,branch:String,sha:String?,message:String="Upload $path from GitHub Vault",private val progress:(Long,Long)->Unit={_,_->}):RequestBody() {
 private val size=file.length()
 private val prefix="{\"content\":\"".toByteArray()
 private val suffix=("\","+buildJsonObject{put("message",message);put("branch",branch);sha?.let{put("sha",it)}}.toString().drop(1)).toByteArray()
 init {require(size<=GitHubPolicy.UPLOAD_LIMIT){"الملف فوق الحد"}}
 override fun contentType()="application/json".toMediaType()
 override fun contentLength()=prefix.size+4*((size+2)/3)+suffix.size
 override fun writeTo(sink:BufferedSink) {
  sink.write(prefix)
  val output=object:FilterOutputStream(sink.outputStream()) {override fun close(){flush()};override fun write(b:ByteArray,off:Int,len:Int){out.write(b,off,len)}}
  var copied=0L
  Base64.getEncoder().wrap(output).use{encoded->file.inputStream().use{input->val buffer=ByteArray(65536);while(true){if(Thread.currentThread().isInterrupted)throw java.io.InterruptedIOException();val n=input.read(buffer);if(n<0)break;copied+=n;require(copied<=size){"الملف اتغيّر"};encoded.write(buffer,0,n);progress(copied,size)}}}
  require(copied==size){"الملف اتغيّر"}
  sink.write(suffix)
 }
}

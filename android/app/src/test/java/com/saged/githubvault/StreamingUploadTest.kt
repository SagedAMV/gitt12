package com.saged.githubvault
import com.saged.githubvault.feature.github.*
import org.junit.Test
import org.junit.Assert.*
import java.io.*
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.atomic.AtomicReference
import java.net.ServerSocket
import kotlin.concurrent.thread
import okhttp3.*
import okio.Buffer
import kotlinx.serialization.json.*

class StreamingUploadTest {
 @Test fun real90MiBHttpTransfer() {
  assertTrue("Run this regression with a bounded heap",Runtime.getRuntime().maxMemory()<=70L*1024*1024)
  val file=File.createTempFile("upload90-",".bin")
  val server=ServerSocket(0,1,java.net.InetAddress.getByName("127.0.0.1"))
  val received=AtomicReference<ByteArray>();val problem=AtomicReference<Throwable>()
  try {
   val digest=MessageDigest.getInstance("SHA-256")
   file.outputStream().use{out->val block=ByteArray(65536){((it*31+17)%251).toByte()};repeat(1440){out.write(block);digest.update(block)}}
   assertEquals(90L*1024*1024,file.length())
   val expected=digest.digest()
   val receiver=thread(name="upload-test-server",isDaemon=true){
    val socket=server.accept();socket.soTimeout=120000
    val input=BufferedInputStream(socket.getInputStream())
    fun line():String{val b=ByteArrayOutputStream();while(true){val c=input.read();if(c<0)throw EOFException();if(c==10)break;if(c!=13)b.write(c)};return b.toString("UTF-8")}
    line();var length=0L
    while(true){val h=line();if(h.isEmpty())break;if(h.startsWith("Content-Length:",true))length=h.substringAfter(':').trim().toLong()}

    try {
     val prefix="{\"content\":\"".toByteArray();assertArrayEquals(prefix,input.readNBytes(prefix.size))
     var remaining=4*((file.length()+2)/3)
     val bounded=object:InputStream(){override fun read():Int {if(remaining==0L)return -1;val n=input.read();if(n>=0)remaining--;return n};override fun read(b:ByteArray,o:Int,l:Int):Int{if(remaining==0L)return -1;val n=input.read(b,o,minOf(l.toLong(),remaining).toInt());if(n>0)remaining-=n;return n}}
     val decoded=Base64.getDecoder().wrap(bounded);val hash=MessageDigest.getInstance("SHA-256");val block=ByteArray(65536);var count=0L
     while(true){val n=decoded.read(block);if(n<0)break;hash.update(block,0,n);count+=n}
     assertEquals(file.length(),count)
     val tail=String(input.readNBytes((length-prefix.size-4*((file.length()+2)/3)).toInt()));val metadata=Json.parseToJsonElement("{\"content\":\""+tail).jsonObject
     assertEquals("main",metadata.getValue("branch").jsonPrimitive.content)
     received.set(hash.digest());socket.getOutputStream().write("HTTP/1.1 201 Created\r\nContent-Length: 2\r\nConnection: close\r\n\r\n{}".toByteArray())
    }catch(t:Throwable){problem.set(t);socket.getOutputStream().write("HTTP/1.1 500 Error\r\nContent-Length: 0\r\n\r\n".toByteArray())}finally{socket.close()}
   }
   var last=0L
   val body=StreamingUploadBody(file,"كبير.bin","main",null){sent,_->last=sent}
   OkHttpClient.Builder().callTimeout(120,java.util.concurrent.TimeUnit.SECONDS).build().newCall(Request.Builder().url("http://127.0.0.1:${server.localPort}/upload").put(body).build()).execute().use{assertEquals(201,it.code)}
   receiver.join(120000)
   problem.get()?.let{throw AssertionError(it)}
   assertArrayEquals(expected,received.get());assertEquals(file.length(),last)
   println("Heap limit=${Runtime.getRuntime().maxMemory()}; 90 MiB HTTP upload verified: ${file.length()} bytes; SHA-256 preserved; streamed request bytes=${body.contentLength()}")
  }finally{server.close();file.delete()}
 }
 @Test fun rejectsOver90MiB(){val f=File.createTempFile("oversize-",".bin");try{RandomAccessFile(f,"rw").use{it.setLength(GitHubPolicy.UPLOAD_LIMIT+1)};try{StreamingUploadBody(f,"x","main",null);fail("must reject")}catch(_:IllegalArgumentException){}}finally{f.delete()}}
 @Test fun paddingAndEscaping(){for(n in listOf(0,1,2,3,65537)){val f=File.createTempFile("padding-",".bin");try{val bytes=ByteArray(n){it.toByte()};f.writeBytes(bytes);val body=StreamingUploadBody(f,"a\"b","main","a".repeat(40));val buffer=Buffer();body.writeTo(buffer);assertEquals(body.contentLength(),buffer.size);val json=Json.parseToJsonElement(buffer.readUtf8()).jsonObject;assertArrayEquals(bytes,Base64.getDecoder().decode(json.getValue("content").jsonPrimitive.content));assertEquals("a".repeat(40),json.getValue("sha").jsonPrimitive.content)}finally{f.delete()}}}
}

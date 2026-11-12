package com.saged.githubvault
import com.saged.githubvault.feature.github.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/** عقد HTTP حقيقي لعمليات إدارة المستودعات: الطريقة والمسار والجسم والتوكن، بلا مكتبات خارجية.
*  أشد العمليات خطورة في التطبيق (حذف نهائي) فيستحق اختبار عقد لا اختبار وحدات شكلي. */
class RepoAdminTest {
 private val token="unit-test-token-1234567890"
 private fun serve(response:String,captured:AtomicReference<String>):ServerSocket {
  val server=ServerSocket(0,1,InetAddress.getByName("127.0.0.1"))
  thread(name="repo-admin-test-server",isDaemon=true){
   try {
    val socket=server.accept();socket.soTimeout=15000
    val input=BufferedInputStream(socket.getInputStream())
    fun line():String{val b=ByteArrayOutputStream();while(true){val c=input.read();if(c<0)throw EOFException();if(c==10)break;if(c!=13)b.write(c)};return b.toString("UTF-8")}
    val requestLine=line();var length=0L;var auth=""
    while(true){val h=line();if(h.isEmpty())break;if(h.startsWith("Content-Length:",true))length=h.substringAfter(':').trim().toLong();if(h.startsWith("Authorization:",true))auth=h}
    val body=if(length>0){val bytes=ByteArray(length.toInt());var read=0;while(read<length){val n=input.read(bytes,read,(length-read).toInt());if(n<0)break;read+=n};String(bytes,0,read,Charsets.UTF_8)}else ""
    captured.set("$requestLine\n$auth\n$body")
    socket.getOutputStream().use{it.write(response.toByteArray())};socket.close()
   }catch(_:Throwable){}
  }
  return server
 }
 private fun client(server:ServerSocket)=GitHubClient(context=null,host="127.0.0.1",scheme="http",port=server.localPort,tokenProvider={token})
 private fun run(response:String,action:suspend (GitHubClient)->Unit):String {
  val captured=AtomicReference<String>();val server=serve(response,captured)
  try{runBlocking{action(client(server))}}finally{server.close()}
  return captured.get()
 }
 @Test fun deleteSendsDeleteAndAccepts204(){
  val seen=run("HTTP/1.1 204 No Content\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"){it.deleteRepository("sageer")}
  assertEquals("DELETE /repos/saged7/sageer HTTP/1.1",seen.split("\n")[0])
  assertTrue("token travels",seen.split("\n")[1].startsWith("Authorization: Bearer $token"))
  assertEquals("delete carries empty json body only","{}",seen.split("\n").getOrNull(2)?:"")
 }
 @Test fun renameSendsPatchWithNewName(){
  val seen=run("HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\n{}"){it.renameRepository("sageer","sageer2")}
  assertEquals("PATCH /repos/saged7/sageer HTTP/1.1",seen.split("\n")[0])
  assertTrue("body has new name",seen.split("\n")[2].contains("\"name\":\"sageer2\""))
 }
 @Test fun archiveSendsBoolean(){
  val seen=run("HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\n{}"){it.setArchived("sageer",true)}
  assertEquals("PATCH /repos/saged7/sageer HTTP/1.1",seen.split("\n")[0])
  assertTrue(seen.split("\n")[2].contains("\"archived\":true"))
 }
 @Test fun forkPostsToForksEndpoint(){
  val seen=run("HTTP/1.1 202 Accepted\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"){it.forkRepository("sageer","sageer-copy")}
  assertEquals("POST /repos/saged7/sageer/forks HTTP/1.1",seen.split("\n")[0])
  assertTrue(seen.split("\n")[2].contains("\"name\":\"sageer-copy\""))
 }
 @Test fun missingTokenFailsFast(){
  val bare=GitHubClient(context=null,tokenProvider={null})
  try{runBlocking{bare.deleteRepository("sageer")};fail("must fail")}catch(e:IllegalStateException){assertEquals("حط التوكن أول",e.message)}
 }
}

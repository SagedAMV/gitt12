package com.saged.githubvault
import org.junit.Test
import org.junit.Assert.*
import java.io.File
class CopySourceTest {
 @Test fun arabicSourceCopyIsAtMostThreeWords(){
  val root=File("src/main");assertTrue("Expected Android app working directory",root.exists())
  val literal=Regex("\"(?:[^\"\\\\]|\\\\.)*\"")
  var checked=0
  root.walkTopDown().filter{it.isFile && it.extension in listOf("kt","xml")}.forEach{file->
   literal.findAll(file.readText()).forEach{match->
    val text=match.value.drop(1).dropLast(1).replace("\\n"," ")
    if(text.any{it in '\u0600'..'\u06ff'}){
     checked++;assertTrue("${file.name}: $text",text.split(Regex("\\s+")).filter{it.isNotBlank()}.size<=3)
    }
   }
  }
  assertTrue("Audit must cover the whole app",checked>300)
 }
}

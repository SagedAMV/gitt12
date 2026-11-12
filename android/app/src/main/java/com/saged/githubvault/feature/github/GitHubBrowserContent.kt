package com.saged.githubvault.feature.github

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.saged.githubvault.core.ui.*
import kotlinx.serialization.json.*

private fun JsonObject.label(key:String)=this[key]?.jsonPrimitive?.content?:""

/** Pure screen used by the live app and deterministic Android screenshot tests. No demo data here. */
@Composable fun GitHubBrowserContent(
 s:GitHubState,search:String,onSearch:(String)->Unit,onBack:()->Unit,onRefresh:()->Unit,
 onOpen:(String,String,String)->Unit,onPreview:(JsonObject)->Unit,onDownload:(JsonObject)->Unit,
 onMore:()->Unit,onAction:()->Unit,onDismiss:()->Unit
) {
 var filter by remember(s.repo,s.path){mutableStateOf("all")}
 val palette=MaterialTheme.colorScheme
 Column(Modifier.fillMaxSize().background(palette.background)) {
  if(s.busy)LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
  if(s.preview!=null) {
   Column(Modifier.weight(1f).padding(horizontal=18.dp)){
    BrowserBack(onBack)
    BrowserTitle(s.previewName,"تفرّج بس")
    SelectionContainer{Text(s.preview,fontFamily=FontFamily.Monospace,fontSize=12.sp,lineHeight=19.2.sp,modifier=Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(vertical=16.dp))}
   }
  } else {
   LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(start=18.dp,end=18.dp,bottom=18.dp),verticalArrangement=Arrangement.spacedBy(0.dp)){
    if(s.repo!=null)item{BrowserBack(onBack)}
    item{BrowserTitle(s.repo?:"مستودعاتك",if(s.repo==null)"saged7 · عام" else "الفرع: ${s.branch}")}
    if(s.repo!=null)item{Text(s.path.ifEmpty{"${s.branch} /"},style=TextStyle(fontFamily=DesignLatinFont,textDirection=TextDirection.Ltr),fontSize=11.sp,lineHeight=17.6.sp,color=palette.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(bottom=19.dp))}
    item{MotionNotice(s.message,onDismiss);if(s.message!=null)Spacer(Modifier.height(14.dp))}
    item{BrowserSearch(search,onSearch,if(s.repo==null)"دوّر مستودع" else "دوّر ملف")}
    if(s.repo==null) {
     item{Row(Modifier.fillMaxWidth().height(29.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("مستودعاتك",fontSize=12.sp,lineHeight=19.2.sp,fontWeight=FontWeight.Bold);MotionTextButton(onClick=onRefresh,enabled=!s.busy){Text("حدّث",fontSize=10.sp,lineHeight=16.sp)}}}
     if(s.repositories.isEmpty()&&!s.busy)item{BrowserEmpty("مافي مستودعات","جرّب زر حدّث")}
     itemsIndexed(s.repositories.filter{it.label("name").contains(search,true)},key={_,it->it.label("id")}){repoIndex,repo->
      MotionCard(modifier=Modifier.fillMaxWidth().padding(bottom=10.dp),shape=RoundedCornerShape(20.dp),onClick={onOpen(repo.label("name"),repo.label("default_branch"),"")},enabled=!s.busy){
       Row(Modifier.fillMaxWidth().heightIn(min=86.dp).padding(horizontal=13.dp,vertical=17.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(if(repoIndex==0)palette.primaryContainer else palette.background),contentAlignment=Alignment.Center){ConceptIcon("folder",tint=if(repoIndex==0)palette.primary else palette.onSurfaceVariant,modifier=Modifier.size(23.dp))}
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){
         Text(repo.label("name"),fontSize=17.sp,lineHeight=22.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis,style=TextStyle(fontFamily=DesignLatinFont,letterSpacing=0.sp,textDirection=TextDirection.ContentOrLtr))
         Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){BrowserTag(if(repo["archived"]?.jsonPrimitive?.boolean==true)"مؤرشف" else "عام");Text(repo.label("default_branch"),fontFamily=DesignLatinFont,fontSize=10.sp,lineHeight=16.sp,color=palette.onSurfaceVariant)}
        }
        ConceptIcon("arrow",tint=palette.onSurfaceVariant,modifier=Modifier.size(17.dp))
       }
      }
     }
     if(s.hasMore)item{MotionOutlinedButton(onClick=onMore,enabled=!s.busy,modifier=Modifier.fillMaxWidth()){Text("ورّيني زيادة")}}
     item{Row(Modifier.fillMaxWidth().padding(top=8.dp).clip(RoundedCornerShape(17.dp)).background(palette.primaryContainer).padding(15.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){ConceptIcon("lock",tint=palette.primary,modifier=Modifier.size(20.dp));Text("تفرّج بدون توكن",fontSize=11.sp,lineHeight=17.6.sp,color=palette.primary)}}
    } else {
     item{Row(Modifier.padding(bottom=15.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){
      listOf("all" to "الكل","dir" to "مجلدات","file" to "ملفات").forEach{(id,name)->MotionFilterChip(selected=filter==id,onClick={filter=id},label={Text(name,fontSize=10.sp,lineHeight=16.sp)})}
      Spacer(Modifier.weight(1f));MotionIconButton(onClick=onRefresh,enabled=!s.busy){Icon(Icons.Rounded.Refresh,"حدّث",modifier=Modifier.size(20.dp))}
     }}
     val files=s.files.filter{it.label("name").contains(search,true)&&(filter=="all"||it.label("type")==filter)}.sortedBy{it.label("type")!="dir"}
     if(files.isEmpty()&&!s.busy)item{BrowserEmpty(if(s.files.isEmpty())"مافي ملفات" else "مافي نتائج",if(s.files.isEmpty())"ارفع أول ملف" else "غيّر بحثك")}
     itemsIndexed(files,key={_,f->f.label("path")}){index,f->
      val folder=f.label("type")=="dir"
      val corners=RoundedCornerShape(topStart=if(index==0)19.dp else 0.dp,topEnd=if(index==0)19.dp else 0.dp,bottomStart=if(index==files.lastIndex)19.dp else 0.dp,bottomEnd=if(index==files.lastIndex)19.dp else 0.dp)
      Surface(color=palette.surface,shape=corners,border=BorderStroke(.5.dp,palette.outlineVariant)){
       Row(Modifier.fillMaxWidth().heightIn(min=72.dp).padding(horizontal=10.dp,vertical=11.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
        Box(Modifier.width(37.dp).height(40.dp).clip(RoundedCornerShape(11.dp)).background(palette.background),contentAlignment=Alignment.Center){ConceptIcon(if(folder)"folder"else "file",tint=palette.onSurfaceVariant,modifier=Modifier.size(23.dp))}
        Column(Modifier.weight(1f).heightIn(min=48.dp).clickable(enabled=!s.busy){if(folder)onOpen(s.repo,s.branch,f.label("path"))else onPreview(f)},verticalArrangement=Arrangement.Center){
         Text(f.label("name"),fontSize=12.sp,lineHeight=19.2.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis,style=TextStyle(fontFamily=DesignLatinFont,letterSpacing=0.sp,textDirection=TextDirection.ContentOrLtr))
         Text(if(folder)"مجلد" else browserSize(f["size"]?.jsonPrimitive?.long?:0),style=TextStyle(fontFamily=if(folder)DesignArabicFont else DesignLatinFont,textDirection=if(folder)TextDirection.Rtl else TextDirection.Ltr),fontSize=10.sp,lineHeight=16.sp,color=palette.onSurfaceVariant,modifier=Modifier.padding(top=4.dp))
        }
        if(folder)Box(Modifier.size(48.dp).clickable(enabled=!s.busy){onOpen(s.repo,s.branch,f.label("path"))},contentAlignment=Alignment.CenterEnd){ConceptIcon("arrow","افتح المجلد",modifier=Modifier.size(17.dp))}
        else Surface(shape=RoundedCornerShape(14.dp),color=palette.primaryContainer){MotionIconButton(onClick={onDownload(f)},enabled=!s.busy,modifier=Modifier.size(48.dp)){ConceptIcon("download","نزّل الملف",tint=palette.primary,modifier=Modifier.size(20.dp))}}
       }
      }
     }
    }
   }
   HorizontalDivider(color=palette.outlineVariant)
   Box(Modifier.fillMaxWidth().padding(start=18.dp,end=18.dp,top=10.dp,bottom=12.dp)){
    MotionButton(onClick=onAction,enabled=!s.busy,modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(17.dp)){
     ConceptIcon(if(s.repo==null)"plus"else "upload",modifier=Modifier.size(21.dp));Spacer(Modifier.width(9.dp));Text(if(s.repo==null)"مستودع جديد" else "ارفع ملفات",fontSize=13.sp,lineHeight=20.8.sp,fontWeight=FontWeight.Bold)
    }
   }
  }
 }
}
@Composable fun BrowserTitle(title:String,subtitle:String,top:Int=14){
 Text(title,fontSize=31.sp,lineHeight=46.5.sp,letterSpacing=(-1).sp,fontFamily=if(title.any{it in '\u0600'..'\u06ff'})DesignArabicFont else DesignLatinFont,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=top.dp,bottom=3.dp))
 Row(Modifier.padding(bottom=15.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){Box(Modifier.size(6.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF236D48)));Text(subtitle,fontSize=11.sp,lineHeight=17.6.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
}
@Composable fun BrowserTag(label:String){Text(label,fontSize=9.sp,lineHeight=14.4.sp,color=MaterialTheme.colorScheme.primary,modifier=Modifier.clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal=6.dp,vertical=2.dp))}
@Composable private fun BrowserBack(action:()->Unit){MotionTextButton(onClick=action,modifier=Modifier.heightIn(min=48.dp),contentPadding=PaddingValues(0.dp)){ConceptIcon("back",modifier=Modifier.size(17.dp));Spacer(Modifier.width(7.dp));Text("ارجع",fontSize=12.sp,lineHeight=19.2.sp)}}
@Composable private fun BrowserSearch(value:String,onChange:(String)->Unit,hint:String){
 val p=MaterialTheme.colorScheme
 Row(Modifier.fillMaxWidth().padding(bottom=17.dp).height(48.dp).clip(RoundedCornerShape(16.dp)).background(p.surface).border(1.dp,p.outlineVariant,RoundedCornerShape(16.dp)).padding(horizontal=13.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){
  ConceptIcon("search",tint=p.onSurfaceVariant,modifier=Modifier.size(19.dp))
  BasicTextField(value,onChange,modifier=Modifier.weight(1f),singleLine=true,textStyle=TextStyle(color=p.onSurface,fontSize=12.sp,lineHeight=19.2.sp),decorationBox={inner->Box{if(value.isEmpty())Text(hint,fontSize=12.sp,lineHeight=19.2.sp,color=p.onSurfaceVariant);inner()}})
 }
}
@Composable private fun BrowserEmpty(title:String,hint:String){Column(Modifier.fillMaxWidth().padding(vertical=27.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Rounded.FolderOpen,null,tint=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.size(34.dp));Spacer(Modifier.height(14.dp));Text(title,fontSize=15.sp,lineHeight=24.sp,fontWeight=FontWeight.Bold);Text(hint,fontSize=12.sp,lineHeight=19.2.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
private fun browserSize(bytes:Long)=when{bytes>=1048576->String.format(java.util.Locale.US,"%.1f MB",bytes/1000000.0);bytes>=1024->String.format(java.util.Locale.US,"%.1f KB",bytes/1000.0);else->"$bytes B"}

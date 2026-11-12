package com.saged.githubvault.core.ui
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saged.githubvault.core.database.Transfer
import com.saged.githubvault.feature.github.*

@Composable fun TransferContent(transfers:List<Transfer>,onRetry:(Transfer)->Unit,onCancel:(Transfer)->Unit){
 val p=MaterialTheme.colorScheme
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(start=18.dp,end=18.dp,bottom=18.dp),verticalArrangement=Arrangement.spacedBy(0.dp)){
  item{BrowserTitle("الشغل","ارفع ونزّل وتابع")}
  if(transfers.isEmpty())item{Column(Modifier.fillMaxWidth().padding(vertical=27.dp)){Text("مافي شغل",fontSize=15.sp,lineHeight=24.sp);Text("الشغل بيظهر هنا",fontSize=12.sp,lineHeight=19.2.sp,color=p.onSurfaceVariant)}}
  items(transfers,key={it.id}){t->
   val progress by animateFloatAsState(t.progress.coerceIn(0f,1f),tween(400),label="transfer")
   val running=t.status in listOf("QUEUED","RUNNING","RETRYING")
   MotionCard(Modifier.fillMaxWidth().padding(bottom=11.dp).animateContentSize(),shape=RoundedCornerShape(19.dp)){
    Column(Modifier.padding(16.dp)){
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(t.label,Modifier.weight(1f),fontSize=11.sp,lineHeight=17.6.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis);Spacer(Modifier.width(9.dp));BrowserTag(transferLabel(t.status))}
     if(running){Spacer(Modifier.height(15.dp));if(t.progress>0)LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth().height(7.dp),trackColor=p.outlineVariant)else LinearProgressIndicator(Modifier.fillMaxWidth().height(7.dp));Spacer(Modifier.height(10.dp))}else Spacer(Modifier.height(12.dp))
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(transferKind(t.kind),fontSize=10.sp,lineHeight=16.sp,color=p.onSurfaceVariant);Text(if(t.status=="SUCCEEDED")"خلصت"else "${(progress*100).toInt()}%",fontSize=10.sp,lineHeight=16.sp,color=p.onSurfaceVariant)}
     t.error?.let{error->var hidden by remember(t.id,error){mutableStateOf(false)};MotionNotice(if(hidden)null else error,{hidden=true})}
     if(running||t.status in listOf("FAILED","CANCELLED"))Row{
      if(t.status in listOf("FAILED","CANCELLED"))MotionTextButton(onClick={onRetry(t)}){Icon(Icons.Rounded.Refresh,null);Text("جرّب ثاني")}
      if(running)MotionTextButton(onClick={onCancel(t)}){Text("وقفها")}
     }
    }
   }
  }
 }
}
private fun transferKind(kind:String)=when(kind){"DOWNLOAD","PUBLIC_DOWNLOAD"->"تنزيل";"TREE_EDIT"->"تعديل";else->"رفع"}
private fun transferLabel(code:String)=when(code){"QUEUED"->"بالطابور";"RUNNING"->"شغّال";"RETRYING"->"بنجرّب ثاني";"SUCCEEDED"->"خلصت";"FAILED"->"ما ضبطت";"CANCELLED"->"وقفناها";else->"بالطابور"}

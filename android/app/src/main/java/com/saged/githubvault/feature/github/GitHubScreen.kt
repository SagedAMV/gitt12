@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.saged.githubvault.feature.github

import com.saged.githubvault.core.ui.*
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.json.*

private fun JsonObject.text(k:String)=this[k]?.jsonPrimitive?.content?:""
@Composable fun GitHubScreen(vm:GitHubViewModel=viewModel()) {
 val s by vm.state.collectAsStateWithLifecycle()
 var search by rememberSaveable{mutableStateOf("")}
 var createDialog by remember{mutableStateOf(false)}
 var repoName by rememberSaveable{mutableStateOf("")}
 var tokenDialog by remember{mutableStateOf(false)}
 var token by remember{mutableStateOf("")}
 var pending by remember{mutableStateOf<List<Uri>?>(null)}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){if(it.isNotEmpty())pending=it}
 BackHandler(s.repo!=null || s.preview!=null){search="";vm.back()}
 if(createDialog)AlertDialog(onDismissRequest={createDialog=false},title={Text("مستودع جديد")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("بيشوفه أي أحد",style=MaterialTheme.typography.bodySmall)
  Text("حسابك: saged7")
  Text("Administration: write",style=MaterialTheme.typography.labelSmall)
  OutlinedTextField(repoName,{repoName=it},label={Text("اسم المستودع")},singleLine=true,modifier=Modifier.fillMaxWidth())
 }},confirmButton={MotionTextButton(onClick={vm.createRepository(repoName);createDialog=false;repoName=""},enabled=repoName.matches(Regex("[A-Za-z0-9_.-]{1,100}"))&&repoName!="."&&repoName!=".."&&!s.busy){Text("سوّه")}},dismissButton={MotionTextButton(onClick={createDialog=false}){Text("خلّيه")}})
 if(tokenDialog)AlertDialog(onDismissRequest={token="";tokenDialog=false},title={Text("توكنك هنا")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("التوكن للكتابة بس")
  Text("Contents: write",style=MaterialTheme.typography.labelSmall)
  Text("Administration: write",style=MaterialTheme.typography.labelSmall)
  Text("محفوظ بجهازك بس",style=MaterialTheme.typography.bodySmall)
  if(s.tokenSaved)Text("توكنك محفوظ هنا",color=MaterialTheme.colorScheme.primary)
  OutlinedTextField(token,{token=it},label={Text(if(s.tokenSaved)"توكن بداله" else "التوكن")},visualTransformation=PasswordVisualTransformation(),singleLine=true,modifier=Modifier.fillMaxWidth())
  Text("لا تشارك توكنك",style=MaterialTheme.typography.labelMedium)
 }},confirmButton={MotionTextButton(onClick={vm.token(token);token="";tokenDialog=false},enabled=token.trim().length>=20&&!s.busy){Text("احفظه")}},dismissButton={Row{if(s.tokenSaved)MotionTextButton(onClick={vm.removeToken();token="";tokenDialog=false}){Text("امسحه")};MotionTextButton(onClick={token="";tokenDialog=false}){Text("سكّر")}}})
 if(pending!=null)AlertDialog(onDismissRequest={pending=null},title={Text("نرفع الملفات؟")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
 Text("${pending!!.size} ملفات")
 Text("الموجود ممكن يتبدّل")
 Text("حدّه 90MiB")
 Text(s.repo?:"",maxLines=1,overflow=TextOverflow.Ellipsis)
 Text(s.path.ifEmpty{"البداية"},maxLines=1,overflow=TextOverflow.Ellipsis)
 Text(s.branch,maxLines=1,overflow=TextOverflow.Ellipsis)
 }},confirmButton={MotionTextButton(onClick={vm.upload(pending!!);pending=null}){Text("ارفعها")}},dismissButton={MotionTextButton(onClick={pending=null}){Text("خلّيه")}})
 Column(Modifier.fillMaxSize()) {
  AnimatedVisibility(s.busy){LinearProgressIndicator(Modifier.fillMaxWidth())}
  com.saged.githubvault.core.ui.MotionNotice(s.message,vm::clearMessage,Modifier.padding(horizontal=16.dp,vertical=8.dp))
  AnimatedContent(targetState=s.preview!=null,transitionSpec={fadeIn(tween(220)) togetherWith fadeOut(tween(140))},label="public-preview"){preview->
   if(preview){
    Column(Modifier.fillMaxSize().padding(horizontal=20.dp)){
     Row(verticalAlignment=Alignment.CenterVertically){MotionIconButton(onClick=vm::closePreview){Icon(Icons.AutoMirrored.Rounded.ArrowBack,"ارجع")};Text(s.previewName,style=MaterialTheme.typography.titleMedium,maxLines=2)}
     Text("تفرّج بس",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     SelectionContainer{Text(s.preview?:"",fontFamily=FontFamily.Monospace,modifier=Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical=16.dp))}
    }
   } else {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),contentPadding=PaddingValues(vertical=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
     item{MotionCard(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth()){
      Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
       Text(if(s.repo==null)"مستودعات saged7" else s.repo!!,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
       Text(if(s.repo==null)"افتح، شوف، ونزّل" else s.path.ifEmpty{"البداية"},style=MaterialTheme.typography.bodyMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
       if(s.repo!=null)Text(s.branch,style=MaterialTheme.typography.labelSmall,maxLines=1,overflow=TextOverflow.Ellipsis)
       Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Rounded.Shield,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(8.dp));Text(if(s.tokenSaved)"توكنك محفوظ هنا" else "تقدر تتفرّج",style=MaterialTheme.typography.labelMedium)}
      }
     }}
     if(s.repo==null)item{MotionFilledTonalButton(onClick={if(s.tokenSaved)createDialog=true else tokenDialog=true},enabled=!s.busy,modifier=Modifier.fillMaxWidth()){Text("مستودع جديد")}}
     item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
      if(s.repo!=null)MotionTextButton(onClick={search="";vm.back()},enabled=!s.busy){Icon(Icons.AutoMirrored.Rounded.ArrowBack,null);Text("ارجع")}
      MotionTextButton(onClick={tokenDialog=true},enabled=!s.busy){Icon(Icons.Rounded.Password,null);Spacer(Modifier.width(6.dp));Text("توكنك")}
      MotionIconButton(onClick={if(s.repo==null)vm.repositories() else vm.open(s.repo!!,s.branch,s.path)},enabled=!s.busy){Icon(Icons.Rounded.Refresh,"حدّث")}
     }}
     item{OutlinedTextField(search,{search=it},Modifier.fillMaxWidth(),placeholder={Text(if(s.repo==null)"دوّر مستودع" else "دوّر ملف")},leadingIcon={Icon(Icons.Rounded.Search,null)},singleLine=true,shape=RoundedCornerShape(18.dp))}
     if(s.repo==null){
      if(s.repositories.isEmpty()&&!s.busy)item{EmptyGitHub("مافي مستودعات","جرّب زر حدّث")}
      items(s.repositories.filter{it.text("name").contains(search,true)},key={it.text("id")}){repo->MotionCard(onClick={search="";vm.open(repo.text("name"),repo.text("default_branch"))},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)){
       Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){
        Icon(Icons.Rounded.FolderCopy,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(30.dp))
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){
         Text(repo.text("name"),fontWeight=FontWeight.Bold)
         Text(if(repo["archived"]?.jsonPrimitive?.boolean==true)"مؤرشف" else "عام",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        };Icon(Icons.Rounded.ChevronLeft,null)
       }
      }}
      if(s.hasMore)item{MotionOutlinedButton(onClick={vm.repositories(true)},enabled=!s.busy,modifier=Modifier.fillMaxWidth()){Text("ورّيني زيادة")}}

     }else{
      item{MotionButton(onClick={if(s.tokenSaved)picker.launch(arrayOf("*/*"))else tokenDialog=true},enabled=!s.busy,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp)){Icon(Icons.Rounded.UploadFile,null);Spacer(Modifier.width(8.dp));Text(if(s.tokenSaved)"ارفع ملفات" else "حط توكنك")}}
      if(s.files.isEmpty()&&!s.busy)item{EmptyGitHub("مافي ملفات","ارفع أول ملف")}
      items(s.files.filter{it.text("name").contains(search,true)}.sortedBy{it.text("type")!="dir"},key={it.text("path")}){f->
       val directory=f.text("type")=="dir"
       MotionCard(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),enabled=!s.busy,onClick={if(directory){search="";vm.open(s.repo!!,s.branch,f.text("path"))}else vm.preview(f)}){
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
         Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){
          Icon(if(directory)Icons.Rounded.Folder else Icons.AutoMirrored.Rounded.InsertDriveFile,null,tint=MaterialTheme.colorScheme.primary)
          Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(f.text("name"),fontWeight=FontWeight.Medium,maxLines=2,overflow=TextOverflow.Ellipsis);Text(if(directory)"مجلد" else "${f.text("size")} بايت",style=MaterialTheme.typography.labelSmall)}
          if(directory)Icon(Icons.Rounded.ChevronLeft,null)
         }
         if(!directory)Row{MotionTextButton(onClick={vm.preview(f)},enabled=!s.busy){Text("شوف النص")};MotionTextButton(onClick={vm.download(f)},enabled=!s.busy){Icon(Icons.Rounded.Download,null);Text("نزّل")}}
        }
       }
      }
     }
    }
   }
  }
 }
}
@Composable private fun EmptyGitHub(title:String,body:String){Column(Modifier.fillMaxWidth().padding(vertical=32.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)){Icon(Icons.Rounded.FolderOpen,null,Modifier.size(48.dp),tint=MaterialTheme.colorScheme.outline);Text(title,style=MaterialTheme.typography.titleMedium);Text(body,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}

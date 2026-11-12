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
 var mkdirDialog by remember{mutableStateOf(false)};var folderName by remember{mutableStateOf("")}
 var renameDialog by remember{mutableStateOf(false)};var newName by remember{mutableStateOf("")}
 var deleteConfirm by remember{mutableStateOf(false)}
 var repoDeleteDialog by remember{mutableStateOf(false)}
 var repoRenameDialog by remember{mutableStateOf(false)};var repoNewName by remember{mutableStateOf("")}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){if(it.isNotEmpty())pending=it}
 BackHandler(s.repo!=null || s.preview!=null || s.selectedRepos.isNotEmpty()){search="";when{
  s.preview!=null->vm.closePreview()
  s.selected.isNotEmpty()->vm.clearSelection()
  s.selectedRepos.isNotEmpty()->vm.clearRepoSelection()
  else->vm.back()
 }}
 if(repoDeleteDialog)AlertDialog(onDismissRequest={repoDeleteDialog=false},title={Text("نمسح ${s.selectedRepos.size} مستودعات؟")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
  Text(s.selectedRepos.take(3).joinToString("، "),maxLines=3,overflow=TextOverflow.Ellipsis,fontFamily=FontFamily.Monospace)
  Text("نهائي وما يرجع")
  Text("التوكن يحتاج Administration")
 }},confirmButton={MotionTextButton(onClick={vm.deleteRepos();repoDeleteDialog=false},enabled=!s.busy){Text("امسحها")}},dismissButton={MotionTextButton(onClick={repoDeleteDialog=false}){Text("خلّيه")}})
 if(repoRenameDialog)AlertDialog(onDismissRequest={repoRenameDialog=false},title={Text("غيّر اسم المستودع")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text(s.selectedRepos.firstOrNull()?:"",maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall)
  Text("الروابط القديمة تتحول",style=MaterialTheme.typography.bodySmall)
  OutlinedTextField(repoNewName,{repoNewName=it},label={Text("الاسم الجديد")},singleLine=true,modifier=Modifier.fillMaxWidth())
 }},confirmButton={MotionTextButton(onClick={vm.renameRepo(s.selectedRepos.first(),repoNewName);repoRenameDialog=false},enabled=runCatching{GitHubPolicy.repo(repoNewName.trim())}.isSuccess&&repoNewName.trim()!=s.selectedRepos.firstOrNull()&&!s.busy){Text("غيّره")}},dismissButton={MotionTextButton(onClick={repoRenameDialog=false}){Text("خلّيه")}})
 if(mkdirDialog)AlertDialog(onDismissRequest={mkdirDialog=false},title={Text("مجلد جديد")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("بيُنشأ بمكانك الحالي",style=MaterialTheme.typography.bodySmall)
  OutlinedTextField(folderName,{folderName=it},label={Text("اسم المجلد")},singleLine=true,modifier=Modifier.fillMaxWidth())
 }},confirmButton={MotionTextButton(onClick={vm.makeFolder(folderName);mkdirDialog=false;folderName=""},enabled=runCatching{GitHubPolicy.name(folderName)}.isSuccess&&!s.busy){Text("سوّه")}},dismissButton={MotionTextButton(onClick={mkdirDialog=false}){Text("خلّيه")}})
 if(renameDialog){val current=s.files.find{it.getValue("path").jsonPrimitive.content==s.selected.firstOrNull()}
  AlertDialog(onDismissRequest={renameDialog=false},title={Text("غيّر الاسم")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
   Text(current?.getValue("path")?.jsonPrimitive?.content?:"",maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall)
   OutlinedTextField(newName,{newName=it},label={Text("الاسم الجديد")},singleLine=true,modifier=Modifier.fillMaxWidth())
  }},confirmButton={MotionTextButton(onClick={vm.renameSelected(newName);renameDialog=false},enabled=runCatching{GitHubPolicy.name(newName)}.isSuccess&&!s.busy){Text("غيّره")}},dismissButton={MotionTextButton(onClick={renameDialog=false}){Text("خلّيه")}})}
 if(deleteConfirm)AlertDialog(onDismissRequest={deleteConfirm=false},title={Text("نمسح ${s.selected.size} عناصر؟")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
  Text("من GitHub نهائي")
  Text("بينفذ بالطابور")
  Text(s.repo?:"",maxLines=1,overflow=TextOverflow.Ellipsis)
 }},confirmButton={MotionTextButton(onClick={vm.removeSelected();deleteConfirm=false},enabled=!s.busy){Text("امسحها")}},dismissButton={MotionTextButton(onClick={deleteConfirm=false}){Text("خلّيه")}})
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
 GitHubBrowserContent(s,search,{search=it},
  onBack={search="";vm.back()},onRefresh={if(s.repo==null)vm.repositories()else vm.open(s.repo!!,s.branch,s.path)},
  onOpen={repo,branch,path->search="";vm.open(repo,branch,path)},onPreview=vm::preview,onDownload=vm::download,
  onMore={vm.repositories(true)},onAction={if(s.repo==null){if(s.tokenSaved)createDialog=true else tokenDialog=true}else if(s.tokenSaved)picker.launch(arrayOf("*/*"))else tokenDialog=true},
  onDismiss=vm::clearMessage,
  onToggleSelect=vm::toggleSelect,
  onCopy={if(s.tokenSaved)vm.clipSelection(ClipMode.COPY) else tokenDialog=true},
  onCut={if(s.tokenSaved)vm.clipSelection(ClipMode.CUT) else tokenDialog=true},
  onPaste={if(s.tokenSaved)vm.pasteClip() else tokenDialog=true},
  onDelete={if(s.tokenSaved)deleteConfirm=true else tokenDialog=true},
  onRename={val path=s.selected.firstOrNull()?:"";val current=s.files.find{it.getValue("path").jsonPrimitive.content==path};newName=current?.getValue("name")?.jsonPrimitive?.content?:"";renameDialog=true},
  onMkdir={if(s.tokenSaved){folderName="";mkdirDialog=true} else tokenDialog=true},
  onClearClip=vm::clearClip,onClearSelection=vm::clearSelection,
  onToggleSelectRepo=vm::toggleSelectRepo,
  onRepoDelete={if(s.tokenSaved)repoDeleteDialog=true else tokenDialog=true},
  onRepoRename={if(s.tokenSaved){repoNewName=s.selectedRepos.firstOrNull()?:"";repoRenameDialog=true} else tokenDialog=true},
  onRepoArchive={if(s.tokenSaved)s.selectedRepos.firstOrNull()?.let{vm.toggleArchiveRepo(it)} else tokenDialog=true},
  onRepoFork={if(s.tokenSaved)vm.forkSelected() else tokenDialog=true},
  onClearRepoSelection=vm::clearRepoSelection)
}

@Composable fun GitHubTokenSettings(vm:GitHubViewModel=viewModel()) {
 val s by vm.state.collectAsStateWithLifecycle()
 var tokenDialog by remember{mutableStateOf(false)}
 var token by remember{mutableStateOf("")}
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("توكنك",fontWeight=FontWeight.Bold);Text(if(s.tokenSaved)"محفوظ هنا" else "مش محفوظ",color=MaterialTheme.colorScheme.onSurfaceVariant)}
  MotionOutlinedButton(onClick={tokenDialog=true},modifier=Modifier.fillMaxWidth()){Icon(Icons.Rounded.Password,null);Spacer(Modifier.width(8.dp));Text(if(s.tokenSaved)"عدّل توكنك" else "حط توكنك")}
  MotionNotice(s.message,vm::clearMessage)
 }
 if(tokenDialog)AlertDialog(onDismissRequest={token="";tokenDialog=false},title={Text("توكنك هنا")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("التوكن للكتابة بس")
  Text("Contents: write",style=MaterialTheme.typography.labelSmall)
  Text("Administration: write",style=MaterialTheme.typography.labelSmall)
  Text("محفوظ بجهازك بس",style=MaterialTheme.typography.bodySmall)
  if(s.tokenSaved)Text("توكنك محفوظ هنا",color=MaterialTheme.colorScheme.primary)
  OutlinedTextField(token,{token=it},label={Text(if(s.tokenSaved)"توكن بداله" else "التوكن")},visualTransformation=PasswordVisualTransformation(),singleLine=true,modifier=Modifier.fillMaxWidth())
  Text("لا تشارك توكنك",style=MaterialTheme.typography.labelMedium)
 }},confirmButton={MotionTextButton(onClick={vm.token(token);token="";tokenDialog=false},enabled=token.trim().length>=20&&!s.busy){Text("احفظه")}},dismissButton={Row{if(s.tokenSaved)MotionTextButton(onClick={vm.removeToken();token="";tokenDialog=false}){Text("امسحه")};MotionTextButton(onClick={token="";tokenDialog=false}){Text("سكّر")}}})
}

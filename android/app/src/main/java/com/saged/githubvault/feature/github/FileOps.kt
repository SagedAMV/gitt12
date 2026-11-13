package com.saged.githubvault.feature.github

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** نمط الحافظة: نسخ يبقي الأصل، قص يمسحه عند اللصق. */
enum class ClipMode { COPY, CUT }

/** عنصر محدد من قائمة المستودع: ملف بـ sha معروف، أو مجلد يُفك لاحقاً من شجرة git. */
@Serializable
data class ClipItem(val repo:String,val path:String,val type:String,val branch:String,val sha:String?=null,val size:Long=0)

 data class Clip(val mode:ClipMode,val items:List<ClipItem>) {
 val count:Int get()=items.size
 val repo:String get()=items.firstOrNull()?.repo?:""
}

/** عنصر جاهز للصق: ملف وحده، أو مجلد مع ملفاته الكاملة من شجرة git (مساراتها الأصلية). */
data class PasteSource(val item:ClipItem,val children:List<ClipItem>)

/** خطوة واحدة قابلة للتنفيذ: PUT يكتب ملفاً (من blob مصدر أو ملف فارغ للمجلدات)، DELETE يمسح ملفاً بـ sha. */
@Serializable
data class TreeOp(
 val kind:String, /** "PUT" | "DELETE" */
 val repo:String,val path:String,val branch:String,
 val sha:String?=null, /** DELETE: sha الحالي للملف */
 val srcRepo:String?=null, /** PUT: مصدر المحتوى، فارغ = ملف فاضي (mkdir) */
 val srcSha:String?=null,
 val size:Long=0,
 val label:String=""
)

/** تخطيط عمليات الشجرة: دوال نقية تأخذ بيانات مقروءة وترجع خطوات للطابور، بدون شبكة ولا أندرويد. */
object FileOps {
 val OPS_LIMIT=400
 private val json=Json { ignoreUnknownKeys=true }
 private val shaHex=Regex("[a-f0-9]{40}")

 fun name(raw:String):String=GitHubPolicy.name(raw)
 fun join(dir:String,name:String)=if(dir.isEmpty())name else "$dir/$name"
 fun parent(path:String)=path.substringBeforeLast('/',"")
 fun baseName(path:String)=path.substringAfterLast('/')

 /** "report.pdf" محجوز → "report (1).pdf" يحافظ على الامتداد. */
 fun unique(existing:Set<String>,desired:String):String {
  if(desired !in existing)return desired
  val dot=desired.lastIndexOf('.')
  val stem=if(dot<=0)desired else desired.substring(0,dot)
  val ext=if(dot<=0)"" else desired.substring(dot)
  var n=1
  while(true){val candidate="$stem ($n)$ext";if(candidate !in existing)return candidate;n++}
 }

 /** يفك مجلداً محدداً إلى ملفاته من شجرة git (مسار→sha) مع الاحتفاظ ببنية المجلدات الفرعية. */
 fun dirBlobs(item:ClipItem,blobs:Map<String,String>):List<ClipItem> {
  val prefix=item.path+"/"
  val children=blobs.entries.filter{it.key.startsWith(prefix)}
  require(children.isNotEmpty()){"مش موجود"}
  return children.map{(path,sha)->ClipItem(item.repo,path,"file",item.branch,sha,0L)}
 }

 /** لصق الحافظة في مجلد الهدف: بنية المجلدات تُحفظ، تنافر الأسماء يُحل بـ unique، والقص يضيف DELETE للمصدر. */
 fun paste(clip:Clip,destRepo:String,destDir:String,branch:String,existing:Set<String>,sources:List<PasteSource>):List<TreeOp> {
  GitHubPolicy.path(destDir)
  val used=HashSet(existing);val ops=ArrayList<TreeOp>()
  for(source in sources){
   val item=source.item
   if(item.type=="dir") {
    require(source.children.isNotEmpty()){"مش موجود"}
    val topDir=unique(used,baseName(item.path))
    val sameSpot=item.repo==destRepo && parent(item.path)==destDir && topDir==baseName(item.path)
    if(sameSpot)continue
    for(child in source.children){
     val destPath=join(destDir,join(topDir,child.path.removePrefix(item.path+"/")))
     GitHubPolicy.path(destPath)
     val sha=child.sha;require(sha!=null && shaHex.matches(sha)){"النسخة مش مضبوطة"}
     ops+=TreeOp("PUT",destRepo,destPath,branch,srcRepo=child.repo,srcSha=sha,label=destPath)
     if(clip.mode==ClipMode.CUT)ops+=TreeOp("DELETE",child.repo,child.path,child.branch,sha=sha,label=child.path)
    }
    used+=topDir
   } else {
    val sha=item.sha;require(sha!=null && shaHex.matches(sha)){"النسخة مش مضبوطة"}
    val destPath=join(destDir,unique(used,baseName(item.path)))
    if(!(item.repo==destRepo && item.path==destPath)) {
     GitHubPolicy.path(destPath)
     ops+=TreeOp("PUT",destRepo,destPath,branch,srcRepo=item.repo,srcSha=sha,size=item.size,label=destPath)
     if(clip.mode==ClipMode.CUT)ops+=TreeOp("DELETE",item.repo,item.path,item.branch,sha=sha,label=item.path)
    }
    used+=destPath
   }
   require(ops.size<=OPS_LIMIT){"الكثير مرّة"}
  }
  require(ops.isNotEmpty()){"مافي شي جديد"}
  require(ops.size<=OPS_LIMIT){"الكثير مرّة"}
  return ops
 }

 /** مسح تحديد: ملف = خطوة واحدة بـ sha، مجلد = كل ملفات تحته من شجرة git (المجلدات الفارغة تختفي تلقائياً في git). */
 fun delete(items:List<ClipItem>,repo:String,branch:String,tree:Map<String,String>):List<TreeOp> {
  val ops=ArrayList<TreeOp>()
  for(item in items){
   if(item.type=="dir"){
    val prefix=item.path+"/"
    val children=tree.entries.filter{it.key.startsWith(prefix)}
    require(children.isNotEmpty()){"مش موجود"}
    require(ops.size+children.size<=OPS_LIMIT){"الكثير مرّة"}
    children.forEach{(path,sha)->ops+=TreeOp("DELETE",repo,path,branch,sha=sha,label=path)}
   } else {
    val sha=item.sha;require(sha!=null && shaHex.matches(sha)){"النسخة مش مضبوطة"}
    ops+=TreeOp("DELETE",repo,item.path,branch,sha=sha,label=item.path)
   }
  }
  require(ops.isNotEmpty()){"مافي شي جديد"}
  require(ops.size<=OPS_LIMIT){"الكثير مرّة"}
  return ops
 }

 /** تسمية جديدة في نفس المجلد: ملف = زوج PUT+DELETE، مجلد = زوج لكل ملف تحته. */
 fun rename(item:ClipItem,newName:String,branch:String,tree:Map<String,String>):List<TreeOp> {
  val clean=name(newName)
  if(clean==baseName(item.path))return emptyList()
  val target=join(parent(item.path),clean);GitHubPolicy.path(target)
  if(item.type=="file"){
   val sha=item.sha;require(sha!=null && shaHex.matches(sha)){"النسخة مش مضبوطة"}
   return listOf(
    TreeOp("PUT",item.repo,target,branch,srcRepo=item.repo,srcSha=sha,size=item.size,label=target),
    TreeOp("DELETE",item.repo,item.path,branch,sha=sha,label=item.path))
  }
  val prefix=item.path+"/"
  val children=tree.entries.filter{it.key.startsWith(prefix)}
  require(children.isNotEmpty()){"مش موجود"}
  require(children.size*2<=OPS_LIMIT){"الكثير مرّة"}
  return children.flatMap{(path,sha)->
   val dest=join(target,path.removePrefix(prefix));GitHubPolicy.path(dest)
   listOf(
    TreeOp("PUT",item.repo,dest,branch,srcRepo=item.repo,srcSha=sha,label=dest),
    TreeOp("DELETE",item.repo,path,branch,sha=sha,label=path))
  }
 }

 /** مجلد جديد: git ما فيه مجلدات فارغة، فنكتب .gitkeep. */
 fun mkdir(repo:String,path:String,branch:String):List<TreeOp> {
  GitHubPolicy.path(path)
  return listOf(TreeOp("PUT",repo,join(path,".gitkeep"),branch,label=path))
 }

 fun encodeOps(ops:List<TreeOp>)=json.encodeToString(ListSerializer(TreeOp.serializer()),ops)
 fun decodeOps(raw:String):List<TreeOp> {return json.decodeFromString(ListSerializer(TreeOp.serializer()),raw)}

 /** اسم متاح لنسخة المستودع عبر fork: sageer → sageer-copy → sageer-copy-2 … */
 fun uniqueFork(existing:Set<String>,source:String):String {
  if("$source-copy" !in existing)return "$source-copy"
  var n=2
  while(true){val candidate="$source-copy-$n";if(candidate !in existing)return candidate;n++}
 }
}

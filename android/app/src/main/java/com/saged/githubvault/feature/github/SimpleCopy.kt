package com.saged.githubvault.feature.github

data class UserFeedback(val title:String,val hint:String,val reference:String,val kind:String="error")
object SimpleCopy {
 private val knownSuccess=setOf("ضبطت","الاتصال جاهز","طلبك عند المشرف","طلعت خلاص","ادخل بالباسورد الجديد","حسابك تحدّث","حدّثنا المستودعات","بدأنا التنزيل","بننزّل الجديد","فحصنا ملفاتك","المستودع جاهز","التوكن جاهز","حذفنا التوكن","مسحنا التوكن","انمسح","تمّت")
 private fun words(s:String)=s.trim().split(Regex("\\s+")).size
 fun feedback(raw:String):UserFeedback {
  val s=raw.trim()
  if(s in knownSuccess)return UserFeedback(s,"تقدر تكمل","OK","success")
  if(s.startsWith("بالطابور ") && Regex("بالطابور [0-9]+ ملفات").matches(s))return UserFeedback(s,"تابع من الشغل","WAIT","info")
  if(s=="وقفناها")return UserFeedback(s,"تقدر تعيدها","STOP","info")
  val approvedErrors=setOf("غيّر اسم المستودع","المسار مش مضبوط","التوكن مش مضبوط","النسخة مش مضبوطة","التوكن مش شغال","جرّب بعدين","ما معك صلاحية","مش موجود","راجع الاسم والنسخة","الطلب ما ضبط","مافي رد","هذا مش مجلد","هذا مش ملف","مافي بيانات","نزّله وشوفه","نزّله وافتحه","حط التوكن أول","الملف فوق الحد","اختار مستودع عام","حط توكنك أول","اختار ملفات أقل","غيّر اسم الملف","الملف اتغيّر","الباسورد مش صح","راجع بياناتك","غيّر الباسورد أول","راجع اللي كتبت","جيب تطبيق يفتحه","ما ضبطت العملية","ما ضبطت")
  val safe=s.takeIf{it in approvedErrors || Regex("رفضنا [0-9]+ ملفات").matches(it)}
  return when {
   s.contains("مش مفعّل")->UserFeedback("حسابك مش مفعّل","كلّم المشرف","E12")
   s.contains("المجلد كبير")->UserFeedback("المجلد كبير مرّة","جرّب مجلد ثاني","E13")
   s.contains("المستودع كبير")->UserFeedback("المستودع كبير مرّة","جرّب مستودع ثاني","E13")
   s.contains("ZIP",true) || s.contains("archive",true)->UserFeedback("المضغوط مش مضبوط","نزّل الملف ثاني","E14")

   s.contains("توكن") || s.contains("token",true)->UserFeedback(safe?:"التوكن مش شغال","حط توكن جديد","E02")
   s.contains("نت") || s.contains("اتصال") || s.contains("network",true) || s.contains("IOException")->UserFeedback("النت مش شغال","شيّك النت","E01")
   s.contains("صلاحية") || s.contains("403")->UserFeedback("ما معك صلاحية","راجع صلاحية التوكن","E03")
   s.contains("باسورد") || s.contains("بياناتك") || s.contains("401") || s.contains("password",true)->UserFeedback(safe?:"راجع بياناتك","راجع الاسم والباسورد","E04")
   s.contains("كبير") || s.contains("حد") || s.contains("MiB") || s.contains("رفضنا") || s.contains("413")->UserFeedback(safe?:"الملف كبير مرّة","اختار ملف أصغر","E05")
   s.contains("اتغيّر") || s.contains("نسخة") || s.contains("409") || s.contains("422")->UserFeedback(safe?:"الملف اتغيّر","حدّث وجرّب ثاني","E06")
   s.contains("مش موجود") || s.contains("404")->UserFeedback("مش موجود","حدّث القائمة","E07")
   s.contains("بعدين") || s.contains("اهدأ") || s.contains("429")->UserFeedback("اهدأ شوي","جرّب بعدين","E08")
   s.contains("اسم") || s.contains("مسار") || s.contains("كتبت")->UserFeedback(safe?:"راجع اللي كتبت","عدّل وجرّب ثاني","E09")
   s.contains("saged7")->UserFeedback("ادخل بحساب saged7","راجع حساب التوكن","E10")
   s.contains("يفتحه") || s.contains("نزّله")->UserFeedback(safe?:"نزّله وافتحه","جيب تطبيق يفتحه","E11")
   else->UserFeedback(safe?:"ما ضبطت العملية","جرّب ثاني","E00")
  }
 }
 fun message(raw:String)=feedback(raw).title
}

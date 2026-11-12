package com.saged.githubvault
import com.saged.githubvault.feature.github.SimpleCopy
import org.junit.Test
import org.junit.Assert.*
class SimpleCopyTest {
 private fun checkShort(s:String){assertTrue("Long copy: $s",s.trim().split(Regex("\\s+")).size<=3);assertFalse(s.contains("http"));assertFalse(s.contains("secret"))}
 @Test fun shortErrors(){for(raw in listOf("ما معك صلاحية","التوكن مش شغال","HTTP 401 password failed","رسالة طويلة جداً جداً لا ينبغي عرضها كاملة للمستخدم","java.io.IOException: http://example.invalid/secret","x ".repeat(1000))){val f=SimpleCopy.feedback(raw);checkShort(f.title);checkShort(f.hint);assertEquals("error",f.kind)}}
 @Test fun actionableReferences(){for((raw,ref) in mapOf("النت مش شغال" to "E01","التوكن مش شغال" to "E02","ما معك صلاحية" to "E03","راجع بياناتك" to "E04","الملف فوق الحد" to "E05","الملف اتغيّر" to "E06","مش موجود" to "E07","اهدأ شوي" to "E08","غيّر اسم المستودع" to "E09","ادخل بحساب saged7" to "E10","جيب تطبيق يفتحه" to "E11","حسابك مش مفعّل" to "E12","المجلد كبير مرّة" to "E13","Invalid ZIP" to "E14","خطأ غير معروف طويل جداً جداً" to "E00")){val f=SimpleCopy.feedback(raw);assertEquals(raw,ref,f.reference);checkShort(f.title);checkShort(f.hint)}}
 @Test fun successIsNotAnError(){for(raw in listOf("ضبطت","الاتصال جاهز","التوكن جاهز","المستودع جاهز","طلبك عند المشرف","بننزّل الجديد","بدأنا التنزيل")){val f=SimpleCopy.feedback(raw);assertEquals("success",f.kind);checkShort(f.title);checkShort(f.hint)}}
 @Test fun queueAndCancellationAreNeutral(){for(raw in listOf("بالطابور 20 ملفات","وقفناها","بالطابور 3 عناصر")){val f=SimpleCopy.feedback(raw);assertEquals("info",f.kind);checkShort(f.title);checkShort(f.hint)}}
 @Test fun treeOpsCopyIsPoliced(){
  for(raw in listOf("جاهزة للنسخ","جاهزة للقص","مافي شي جديد","بالطابور عنصر واحد")){val f=SimpleCopy.feedback(raw);assertEquals("success",f.kind);checkShort(f.title);checkShort(f.hint)}
  for(raw in listOf("الاسم مش مضبوط","الكثير مرّة")){val f=SimpleCopy.feedback(raw);assertEquals("error",f.kind);checkShort(f.title);checkShort(f.hint)}
 }
 @Test fun longBackendTextIsNotExposed(){val s="server rejected secret_token at http://example.invalid because of internal details";assertEquals("التوكن مش شغال",SimpleCopy.message(s));checkShort(SimpleCopy.message(s))}
}

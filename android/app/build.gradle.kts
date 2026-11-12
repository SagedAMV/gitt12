import java.util.Properties

plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
 id("org.jetbrains.kotlin.plugin.serialization")
 id("com.google.devtools.ksp")
}
val withoutTests = providers.gradleProperty("withoutTests").map(String::toBoolean).getOrElse(false)
val projectConfig = Properties().apply { rootProject.file("vault-project.properties").inputStream().use { load(it) } }
android {
 namespace = "com.saged.githubvault"
 compileSdk = 35
 defaultConfig {
  applicationId = "com.saged.githubvault"; minSdk = 26; targetSdk = 35; versionCode = 12; versionName = "1.0.0"
  buildConfigField("String", "SUPABASE_URL", "\"${projectConfig.getProperty("SUPABASE_URL", "")}\"")
  buildConfigField("String", "SUPABASE_ANON_KEY", "\"${projectConfig.getProperty("SUPABASE_ANON_KEY", "")}\"")
 }
 testOptions { unitTests.isIncludeAndroidResources = true }
 buildFeatures { compose = true; buildConfig = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2024.12.01"))
 implementation("androidx.activity:activity-compose:1.9.3")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.material:material-icons-core")
 implementation("androidx.documentfile:documentfile:1.0.1")
 implementation("androidx.core:core-ktx:1.15.0")
 implementation("androidx.webkit:webkit:1.12.1")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
 implementation("androidx.navigation:navigation-compose:2.8.5")
 implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
 implementation("com.squareup.okhttp3:okhttp:4.12.0")
 implementation("androidx.room:room-runtime:2.6.1")
 implementation("androidx.room:room-ktx:2.6.1")
 ksp("androidx.room:room-compiler:2.6.1")
 implementation("androidx.work:work-runtime-ktx:2.10.0")
 if (!withoutTests) {
 testImplementation("junit:junit:4.13.2")
 testImplementation("org.robolectric:robolectric:4.14.1")
 testImplementation("io.github.takahirom.roborazzi:roborazzi:1.39.0")
 testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.39.0")
 testImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
 testImplementation("androidx.compose.ui:ui-test-junit4")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
 }
}

androidComponents {
 beforeVariants(selector().all()) { variant ->
  if (withoutTests) {
   variant.enableUnitTest = false
   variant.enableAndroidTest = false
  }
 }
}

if (!withoutTests) {
// A 90 MiB file must transfer even when the test heap cannot hold that file.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
 maxHeapSize = "64m"
 if(name=="testDebugUnitTest")exclude("**/DesignScreenshotTest*")
}

tasks.register<org.gradle.api.tasks.testing.Test>("designScreenshotTest") {
 val nativeUnitTests=tasks.named<org.gradle.api.tasks.testing.Test>("testDebugUnitTest")
 dependsOn("compileDebugUnitTestKotlin","generateDebugUnitTestConfig")
 testClassesDirs=nativeUnitTests.get().testClassesDirs
 classpath=nativeUnitTests.get().classpath
 filter { includeTestsMatching("com.saged.githubvault.DesignScreenshotTest") }
 maxHeapSize="384m"
 val renderTemp=layout.buildDirectory.dir("tmp/design-screens").get().asFile
 doFirst { renderTemp.mkdirs() }
 systemProperty("java.io.tmpdir",renderTemp.absolutePath)
 jvmArgs("-XX:ActiveProcessorCount=2","-XX:+UseSerialGC","-XX:MaxMetaspaceSize=192m","-XX:ReservedCodeCacheSize=48m")
 systemProperty("roborazzi.test.record","true")
 systemProperty("robolectric.dependency.repo.url","https://repo.maven.apache.org/maven2")
}

}

import java.util.Properties

plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
 id("org.jetbrains.kotlin.plugin.serialization")
 id("com.google.devtools.ksp")
}
val projectConfig = Properties().apply { rootProject.file("vault-project.properties").inputStream().use { load(it) } }
android {
 namespace = "com.saged.githubvault"
 compileSdk = 35
 defaultConfig {
  applicationId = "com.saged.githubvault"; minSdk = 26; targetSdk = 35; versionCode = 7; versionName = "0.7.0"
  buildConfigField("String", "SUPABASE_URL", "\"${projectConfig.getProperty("SUPABASE_URL", "")}\"")
  buildConfigField("String", "SUPABASE_ANON_KEY", "\"${projectConfig.getProperty("SUPABASE_ANON_KEY", "")}\"")
 }
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
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
 implementation("androidx.navigation:navigation-compose:2.8.5")
 implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
 implementation("com.squareup.okhttp3:okhttp:4.12.0")
 implementation("androidx.room:room-runtime:2.6.1")
 implementation("androidx.room:room-ktx:2.6.1")
 ksp("androidx.room:room-compiler:2.6.1")
 implementation("androidx.work:work-runtime-ktx:2.10.0")
 testImplementation("junit:junit:4.13.2")
}

// A 90 MiB file must transfer even when the test heap cannot hold that file.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
 maxHeapSize = "64m"
}

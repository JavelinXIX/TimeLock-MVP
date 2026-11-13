plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
 namespace = "jp.timelock"; compileSdkVersion("android-37.0")
 defaultConfig { applicationId = "jp.timelock"; minSdk = 30; targetSdk = 37; versionCode = providers.environmentVariable("VERSION_CODE").orNull?.toInt() ?: 1; versionName = "0.1.0" }
 val keyPath = System.getenv("SIGNING_KEYSTORE_PATH")
 signingConfigs { if (keyPath != null) create("release") { storeFile = file(keyPath); storePassword = System.getenv("SIGNING_STORE_PASSWORD"); keyAlias = System.getenv("SIGNING_KEY_ALIAS"); keyPassword = System.getenv("SIGNING_KEY_PASSWORD") } }
 buildTypes { getByName("release") { if (keyPath != null) signingConfig = signingConfigs.getByName("release") } }
 buildFeatures { compose = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2025.11.01"))
 implementation("androidx.activity:activity-compose:1.12.0")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.foundation:foundation")
 testImplementation("junit:junit:4.13.2")
}

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val pexelsApiKey = (
    localProperties.getProperty("PEXELS_API_KEY")
        ?: providers.gradleProperty("PEXELS_API_KEY").orNull
        ?: ""
).replace("\\", "\\\\").replace("\"", "\\\"")

// Debug and closed-test bundles default to Google's rewarded test unit.
fun adSetting(name: String) =
    providers.gradleProperty(name).orNull ?: localProperties.getProperty(name)

val privacyPolicyUrl = adSetting("PRIVACY_POLICY_URL").orEmpty()
require(privacyPolicyUrl.isEmpty() || (
    privacyPolicyUrl.startsWith("https://") && privacyPolicyUrl.none { it.isWhitespace() || it == '"' || it == '\\' }
))
val demoAppId = "ca-app-pub-3940256099942544~3347511713"
val demoRewardId = "ca-app-pub-3940256099942544/5224354917"
val appIdForAds = adSetting("ADMOB_APP_ID") ?: demoAppId
val liveRewards = adSetting("ADMOB_LIVE_ADS") == "true"
val releaseRewardId = if (liveRewards) requireNotNull(adSetting("ADMOB_REWARDED_ID")) else demoRewardId
require(appIdForAds.matches(Regex("ca-app-pub-[0-9]{16}~[0-9]{10}")))
if (liveRewards) {
    require(!appIdForAds.startsWith("ca-app-pub-3940256099942544"))
    require(releaseRewardId.matches(Regex("ca-app-pub-[0-9]{16}/[0-9]{10}")))
    require(releaseRewardId.substringBefore('/') == appIdForAds.substringBefore('~'))
}

android {
    namespace = "com.hb.puzz"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hb.puzz"
        minSdk = 25
        targetSdk = 37
        versionCode = 3
        versionName = "1.2"

        buildConfigField("String", "PRIVACY_POLICY_URL", "\"$privacyPolicyUrl\"")
        manifestPlaceholders["admobAppId"] = appIdForAds
        buildConfigField("String", "REWARDED_AD_ID", "\"$demoRewardId\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "PEXELS_API_KEY", "\"$pexelsApiKey\"")
    }

    buildTypes {
        release {
            buildConfigField("String", "REWARDED_AD_ID", "\"$releaseRewardId\"")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-ads:25.5.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    implementation("com.google.android.material:material:1.14.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:${libs.versions.lifecycle.get()}")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation(libs.androidx.compose.ui.tooling)
}

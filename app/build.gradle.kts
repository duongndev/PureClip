import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        load(FileInputStream(file))
    }
}

val admobAppId = localProperties.getProperty("ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")
val appOpenId = localProperties.getProperty("APP_OPEN_ID", "ca-app-pub-3940256099942544/9257395921")
val bannerMainId = localProperties.getProperty("BANNER_MAIN_ID", "ca-app-pub-3940256099942544/6300978111")
val adaptiveBannerId = localProperties.getProperty("ADAPTIVE_BANNER_ID", "ca-app-pub-3940256099942544/9214589741")
val interstitialDownloadId = localProperties.getProperty("INTERSTITIAL_DOWNLOAD_ID", "ca-app-pub-3940256099942544/1033173712")
val rewardedHdId = localProperties.getProperty("REWARDED_HD_ID", "ca-app-pub-3940256099942544/5224354917")
val rewardedInterstitialId = localProperties.getProperty("REWARDED_INTERSTITIAL_ID", "ca-app-pub-3940256099942544/5354046379")
val nativeId = localProperties.getProperty("NATIVE_ID", "ca-app-pub-3940256099942544/2247696110")
val nativeVideoId = localProperties.getProperty("NATIVE_VIDEO_ID", "ca-app-pub-3940256099942544/1044960115")
val baseApiUrl = localProperties.getProperty("BASE_API_URL", "http://10.0.2.2:3000/")

android {
    namespace = "com.pureclip.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.pureclip.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["AD_MOB_APP_ID"] = admobAppId
        buildConfigField("String", "BASE_API_URL", "\"$baseApiUrl\"")
        buildConfigField("String", "ADMOB_APP_ID", "\"$admobAppId\"")
        buildConfigField("String", "APP_OPEN_ID", "\"$appOpenId\"")
        buildConfigField("String", "BANNER_MAIN_ID", "\"$bannerMainId\"")
        buildConfigField("String", "ADAPTIVE_BANNER_ID", "\"$adaptiveBannerId\"")
        buildConfigField("String", "INTERSTITIAL_DOWNLOAD_ID", "\"$interstitialDownloadId\"")
        buildConfigField("String", "REWARDED_HD_ID", "\"$rewardedHdId\"")
        buildConfigField("String", "REWARDED_INTERSTITIAL_ID", "\"$rewardedInterstitialId\"")
        buildConfigField("String", "NATIVE_ID", "\"$nativeId\"")
        buildConfigField("String", "NATIVE_VIDEO_ID", "\"$nativeVideoId\"")
    }

    signingConfigs {
        create("release") {
            val keyStorePath = localProperties.getProperty("RELEASE_STORE_FILE")
            if (!keyStorePath.isNullOrBlank() && file(keyStorePath).exists()) {
                storeFile = file(keyStorePath)
                storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.kotlinCoroutines)
    implementation(libs.retrofit)
    implementation(libs.retrofitConverterGson)
    implementation(libs.playServicesAds)
    implementation(libs.glide)
    
    // Firebase BoM & Analytics
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

    annotationProcessor(libs.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

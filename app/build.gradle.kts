plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "com.rabu.hyphen"
  compileSdk = 36
  
  defaultConfig {
      applicationId = "com.rabu.hyphen"
      minSdk = 24
      targetSdk = 36
      versionCode = 1
      versionName = "1.0"

      // for only my mobile apk-
      ndk {
          abiFilters.add("arm64-v8a")
      }

      // फालतू की भाषाओं के रिसोर्सेज को हटाकर साइज कम करता है
      resConfigs("en") 

    }

      signingConfigs {
        create("release") {
            // आपकी चाबी वाली फाइल का नाम (यह फाइल आपके 'app' फोल्डर के अंदर होनी चाहिए)
            storeFile = file("../my-release-key.jks")
            storePassword = "8107214203" // जो पासवर्ड आपने चाबी बनाते समय रखा था
            keyAlias = "hypen-alias"                // आपकी चाबी का एलियास नाम
            keyPassword = "8107214203"        // आपकी चाबी का पासवर्ड
        }
      }





  buildTypes {
      release {
        isMinifyEnabled = true // 1. कोड कंप्रेस (R8) चालू करें
        isShrinkResources = true // 2. बिना इस्तेमाल वाले रिसोर्स हटाएं
        //proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

        signingConfig = signingConfigs.getByName("release")
      }
  }
  compileOptions {
      sourceCompatibility = JavaVersion.VERSION_17
      targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    aidl = false
    buildConfig = false
    shaders = false
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  //View Model Type Safety 
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")  // Compose ke liye

  //Navigation 
  implementation("androidx.navigation:navigation-compose:2.9.8")

  implementation("androidx.datastore:datastore-preferences:1.1.7")

}
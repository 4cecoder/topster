plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
}

android {
    namespace = "com.topster.tv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.topster.tv"
        minSdk = 21  // Android TV requires API 21+
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        // Enable MultiDex for large apps (following SmartTube)
        multiDexEnabled = true

        // Vector drawable support for older devices
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Product flavors for different distributions
    flavorDimensions += "version"
    productFlavors {
        create("stable") {
            dimension = "version"
            applicationIdSuffix = ".stable"
            versionNameSuffix = "-stable"
        }
        create("beta") {
            dimension = "version"
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
        }
    }

    // Split APKs by ABI (following SmartTube)
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = false
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            // Disable obfuscation in debug for easier debugging
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            // Following SmartTube: Don't obfuscate for better stack traces
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Enable resource shrinking
            isShrinkResources = true
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // MultiDex support (following SmartTube)
    implementation("androidx.multidex:multidex:2.0.1")

    // Compose for TV
    implementation("androidx.tv:tv-foundation:1.0.0-alpha10")
    implementation("androidx.tv:tv-material:1.0.0-alpha10")
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
    implementation("androidx.activity:activity-compose:1.8.1")

    // Lifecycle and ViewModel
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // ExoPlayer for video playback
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // HTML Parsing (for scraping)
    implementation("org.jsoup:jsoup:1.17.2")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Room database for history
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Caching
    implementation("androidx.collection:collection-ktx:1.4.0")

    // Leanback for TV
    implementation("androidx.leanback:leanback:1.0.0")

    // NanoHTTPD for embedded HTTP server (OTA updates)
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    // Core AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test:core-ktx:1.5.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
    androidTestImplementation("androidx.media3:media3-test-utils:1.2.0")

    // Debugging
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.4")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")
}

 // Configure test options
    android {
        testOptions {
            unitTests {
                isIncludeAndroidResources = true
                isReturnDefaultValues = true
            }
        }

        // Lint configuration - STRICT
        lint {
            checkDependencies = true
            checkTestSources = true
            abortOnError = true
            warningsAsErrors = true
            absolutePaths = false
            checkAllWarnings = true

        // Disable issues we intentionally ignore
        disable.addAll(listOf(
            "TypographyEllipsis",
            "OldTargetApi",
            "ExpiredTargetSdkVersion",
            "ContentDescription"
        ))

        // Enable additional checks
        enable.addAll(listOf(
            "UnusedResources",
            "Overdraw",
            "VectorPath",
            "RLog",
            "RestrictedApi",
            "KotlinConstantConditions"
        ))

            // Configure output
            htmlReport = true
            xmlReport = true
            htmlOutput = file("${project.buildDir}/reports/lint-results.html")
            xmlOutput = file("${project.buildDir}/reports/lint-results.xml")
        }
    }

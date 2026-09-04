plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.quserh.eorzeaphone"
    compileSdk = 36
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "com.quserh.eorzeaphone"
        minSdk = 24
        targetSdk = 36
        versionCode = 362
        versionName = "0.7.342"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        // Release is signed with the *debug* key on purpose. Every build handed out so far
        // (0.7.258 onward) was debug-signed, so reusing that key keeps release installable
        // straight over the top with no uninstall and no data loss. Swap in a real keystore
        // before any public distribution.
        create("releaseWithDebugKey") {
            storeFile = File(System.getProperty("user.home"), ".android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            // The point of this build type: debuggable=false. A debuggable APK loses ART's
            // optimizations, and Compose is allocation-heavy — on real mid-range ARM that
            // reads as "animations don't work" because frames are produced but far too slow.
            // Emulators on a desktop CPU mask it, which is why MuMu always looked fine.
            isDebuggable = false
            // R8 left off deliberately: msgpack / JNA / lazysodium resolve reflectively and
            // would need keep rules first. debuggable=false is the win worth having now;
            // shrinking is a separate change with its own verification.
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("releaseWithDebugKey")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    // The app ships its own chat-rendering baseline profile. Keep the installer direct
    // instead of relying on Activity's transitive dependency so that profile delivery
    // cannot silently disappear after an unrelated dependency upgrade.
    implementation("androidx.profileinstaller:profileinstaller:1.4.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.msgpack:msgpack-core:0.9.9")
    implementation("com.goterl:lazysodium-android:5.1.0") {
        exclude(group = "net.java.dev.jna", module = "jna")
    }
    implementation("net.java.dev.jna:jna:5.12.1@aar")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

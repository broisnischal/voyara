import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// TomTom key: TOMTOM_API_KEY in local.properties (kept out of version control) or the environment.
// Without one the app falls back to OpenFreeMap tiles.
val tomtomApiKey: String = rootProject.file("local.properties").takeIf { it.exists() }
    ?.let { f -> Properties().apply { f.inputStream().use(::load) }.getProperty("TOMTOM_API_KEY") }
    ?: System.getenv("TOMTOM_API_KEY")
    ?: ""

// Release signing: keystore.properties (kept out of version control) points at the upload key.
val releaseKeys: Properties? = rootProject.file("keystore.properties").takeIf { it.exists() }
    ?.let { f -> Properties().apply { f.inputStream().use(::load) } }

android {
    namespace = "app.voyara"
    compileSdk = 37

    defaultConfig {
        applicationId = "click.stroke.voyara"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0"
        buildConfigField("String", "TOMTOM_API_KEY", "\"$tomtomApiKey\"")
        resValue("string", "app_name", "Voyara")
    }

    signingConfigs {
        if (releaseKeys != null) {
            create("release") {
                storeFile = file(releaseKeys.getProperty("storeFile"))
                storePassword = releaseKeys.getProperty("storePassword")
                keyAlias = releaseKeys.getProperty("keyAlias")
                keyPassword = releaseKeys.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        // Dev builds install next to the real app, under their own id and name.
        debug {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "Voyara Dev")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Ship native symbols so Play symbolicates MapLibre crash traces.
            ndk { debugSymbolLevel = "SYMBOL_TABLE" }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release") ?: run {
                logger.warn("keystore.properties not found: release is signed with the debug key.")
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.maplibre.gl:android-sdk:13.6.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0") // MapLibre's own version; used to extend tile caching
    implementation("com.google.android.gms:play-services-location:21.4.0")

    testImplementation("junit:junit:4.13.2")
}

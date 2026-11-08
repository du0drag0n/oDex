plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "io.lemoncore.opendatadex"
    compileSdk = 37

    defaultConfig {
        // Permanent once published. The code's namespace above keeps the project's first name.
        applicationId = "io.lemoncore.odex"
        minSdk = 26
        targetSdk = 36
        // Play and GitHub share both: versionCode only ever goes up, and versionName is what Obtainium compares.
        versionCode = 1
        versionName = "0.9.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        // The Ko-fi page Support links to, set as odex.kofiUrl in gradle.properties; the button is hidden while it's empty.
        val kofiUrl = providers.gradleProperty("odex.kofiUrl").orNull?.trim().orEmpty()
        require(kofiUrl.isEmpty() || Regex("""https://ko-fi\.com/[A-Za-z0-9_]+/?""").matches(kofiUrl)) {
            "odex.kofiUrl must look like https://ko-fi.com/yourname, not \"$kofiUrl\""
        }
        buildConfigField("String", "KOFI_URL", "\"$kofiUrl\"")
        // -Podex.silhouettes=true draws Pokémon as flat shapes, for screenshots that don't reproduce the artwork.
        buildConfigField("boolean", "SILHOUETTES", (providers.gradleProperty("odex.silhouettes").orNull == "true").toString())
    }

    // The release key lives outside the repository: set odex.signing.storeFile, storePassword, keyAlias and keyPassword in
    // ~/.gradle/gradle.properties (or pass them as ODEX_SIGNING_* environment variables, as CI would).
    val signing = { name: String ->
        providers.gradleProperty("odex.signing.$name").orNull ?: System.getenv("ODEX_SIGNING_" + name.uppercase())
    }
    val releaseKey = signing("storeFile")?.let { file(it) }?.takeIf { it.isFile }

    signingConfigs {
        if (releaseKey != null) create("release") {
            storeFile = releaseKey
            storePassword = signing("storePassword")
            keyAlias = signing("keyAlias")
            keyPassword = signing("keyPassword")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Without a key the build is unsigned rather than quietly signed with the debug key, which Play would refuse and
            // which would make updates from the real key impossible.
            signingConfig = signingConfigs.findByName("release")
        }
        // A debug build shrunk small enough to send to a phone; it updates the debug app in place and keeps its data.
        create("sideload") {
            initWith(getByName("debug"))
            matchingFallbacks += "debug"
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
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

    // Lists the app's languages in Android 13+ per-app language settings (default strings are English).
    androidResources { generateLocaleConfig = true }
    // Every language stays in the app, since the app language can be switched inside the app.
    bundle { language { enableSplit = false } }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    testImplementation(libs.junit)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.palette)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.gif)
}

import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}


val hxoModulePath = project(":HxoLoader")
val hxoDexBuildDir = hxoModulePath.layout.buildDirectory.dir("intermediates/dex")
val assetsOutputDir = layout.projectDirectory.dir("src/main/assets/loader")
val hxoProject = project(":HxoLoader")

android {
    namespace = "io.kitsuri.m1rage"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.kitsuri.m1rage"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

tasks.matching { it.name.startsWith("assemble") }.configureEach {
    dependsOn("exportHxoDex")
}



tasks.register<Exec>("exportHxoDex") {
    group = "build"
    description = "Copies HxoLoader dex into app assets"

    dependsOn(project(":HxoLoader").tasks.named("aarToDexRelease"))

    commandLine("python", "--version")

    commandLine(
        "python",
        "${rootProject.projectDir}/scripts/export_hxo_dex.py",
        "${rootProject.projectDir}/out",
        "${projectDir}/src/main/assets/loader"
    )
}


dependencies {

    implementation(project(":TablerIcons"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.apksig)
    implementation(libs.axml)
    implementation(libs.crashReporter)
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation(libs.ui.graphics)
    implementation(libs.apkutils)
    implementation(libs.adapters)
    implementation(libs.credits)
    implementation(libs.fileutils)
    implementation(libs.installerutils)
    implementation(libs.packageutils)
    implementation(libs.permissionutils)
    implementation(libs.themeutils)
    implementation(libs.translatorutils)

    implementation(libs.documentfile)
    implementation(libs.material)
    implementation(libs.zip4j)
    implementation(libs.baksmali)
    implementation(libs.smali)
}
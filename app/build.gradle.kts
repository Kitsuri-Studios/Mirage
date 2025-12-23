import java.io.File
import java.util.regex.Pattern
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization")
}


val hxoModulePath = project(":HxoInjector")
val hxoDexBuildDir = hxoModulePath.layout.buildDirectory.dir("intermediates/dex")
val assetsOutputDir = layout.projectDirectory.dir("src/main/assets/loader")
val hxoProject = project(":HxoInjector")

android {
    namespace = "io.kitsuri.m1rage"
    compileSdk = 36
    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "io.kitsuri.m1rage"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField(
            "String",
            "HXO_VERSION",
            "\"${readHxoVersion()}\""
        )
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
        buildConfig = true
    }


}

tasks.matching { it.name.startsWith("assemble") }.configureEach {
    dependsOn("exportHxoDex")
}



tasks.register<Exec>("exportHxoDex") {
    group = "build"
    description = "Copies HxoLoader dex into app assets"

    dependsOn(project(":HxoInjector").tasks.named("aarToDexRelease"))

    commandLine("python", "--version")

    commandLine(
        "python",
        "${rootProject.projectDir}/export_hxo_dex.py",
        "${rootProject.projectDir}/out",
        "${projectDir}/src/main/assets/loader"
    )
}

afterEvaluate {
    tasks.matching { it.name == "preBuild" }.configureEach {
        dependsOn(":buildHxoAll")
    }
}

fun readHxoVersion(): String {
    val cmake = file("$rootDir/hxo-loader/CMakeLists.txt").readText()
    val matcher = Pattern
        .compile("""project\s*\(\s*hxo-loader\s+VERSION\s+([0-9.]+)\s*\)""")
        .matcher(cmake)

    return if (matcher.find()) matcher.group(1) else "unknown"
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
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
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
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.hxo.loader"
    compileSdk = 36

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}
val d8 by configurations.creating

dependencies {
    d8("com.android.tools:r8:8.3.37")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

tasks.register<JavaExec>("aarToDexRelease") {
    dependsOn(tasks.matching { it.name.startsWith("assemble") && it.name.endsWith("Release") })


    val aarFile = file("$buildDir/outputs/aar/HxoLoader-release.aar")
    val tempDir = file("$buildDir/tmp/aarDex")
    val outDir = file("${rootProject.projectDir}/out")

    doFirst {
        if (!aarFile.exists()) {
            throw GradleException("AAR not found: $aarFile")
        }

        tempDir.deleteRecursively()
        tempDir.mkdirs()
        outDir.mkdirs()

        copy {
            from(zipTree(aarFile))
            into(tempDir)
        }
    }

    classpath = d8
    mainClass.set("com.android.tools.r8.D8")

    val androidJarArgs = android.bootClasspath.map {
        listOf("--lib", it.absolutePath)
    }.flatten()

    args(
        "--min-api", "21",
        "--release",
        "--output", outDir.absolutePath,
        *androidJarArgs.toTypedArray(),
        file("$tempDir/classes.jar").absolutePath
    )
}




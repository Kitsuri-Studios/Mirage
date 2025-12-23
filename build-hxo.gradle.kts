import org.gradle.api.tasks.Exec
import java.io.File


val hxoDir = file("hxo-loader")
val buildDir = file("$hxoDir/build")
val appAssetsDir = file("app/src/main/assets/libs")

val abis = listOf(
    "arm64-v8a",
    "armeabi-v7a",
    "x86",
    "x86_64"
)

rootProject.project(":app").plugins.withId("com.android.application") {

    val androidExt = rootProject.project(":app")
        .extensions.getByName("android")

    val ndkDir = androidExt::class.java
        .methods
        .first { it.name == "getNdkDirectory" }
        .invoke(androidExt) as File

    println("HXO build using NDK: $ndkDir")

    abis.forEach { abi ->

        val abiBuildDir = File(buildDir, abi)

        val configure = tasks.register<Exec>("configureHxo_$abi") {
            group = "hxo"
            commandLine(
                "cmake",
                "-S", hxoDir.absolutePath,
                "-B", abiBuildDir.absolutePath,
                "-DCMAKE_TOOLCHAIN_FILE=${ndkDir}/build/cmake/android.toolchain.cmake",
                "-DANDROID_ABI=$abi",
                "-DANDROID_PLATFORM=android-21",
                "-DANDROID_NDK=${ndkDir.absolutePath}"
            )
        }

        val build = tasks.register<Exec>("buildHxo_$abi") {
            group = "hxo"
            dependsOn(configure)
            commandLine("cmake", "--build", abiBuildDir.absolutePath)
        }

        tasks.register<Copy>("copyHxo_$abi") {
            group = "hxo"
            dependsOn(build)
            from(File(abiBuildDir, "libhxo.so"))
            into(File(appAssetsDir, abi))
        }
    }

    tasks.register("buildHxoAll") {
        group = "hxo"
        dependsOn(abis.map { "copyHxo_$it" })
    }
}

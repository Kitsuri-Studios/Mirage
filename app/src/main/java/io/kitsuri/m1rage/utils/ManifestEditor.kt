package io.kitsuri.m1rage.utils

import android.content.Context
import com.apk.axml.aXMLDecoder
import com.apk.axml.aXMLEncoder
import java.io.File

object ManifestEditor {

    fun addProvider(
        context: Context,
        manifestFile: File,
        packageName: String
    ) {
           val xml = aXMLDecoder(manifestFile.inputStream()).decodeAsString()
            ?: error("Failed to decode AndroidManifest.xml")

        if (xml.contains("com.hxo.loader.HxoLoader")) {
            return
        }

        val providerXml = """
            <provider
                android:name="com.hxo.loader.HxoLoader"
                android:authorities="$packageName.hxo.init"
                android:exported="false"
                android:initOrder="1000"/>
        """.trimIndent()

        val modifiedXml = xml.replaceFirst(
            "</application>",
            "$providerXml\n</application>"
        )

        val encoder = aXMLEncoder()
        val encoded = encoder.encodeString(context, modifiedXml)
        manifestFile.writeBytes(encoded)
    }

    fun addMetaData(
        context: Context,
        manifestFile: File,
        name: String,
        value: String
    ) {
        val xml = aXMLDecoder(manifestFile.inputStream()).decodeAsString()
            ?: error("Failed to decode AndroidManifest.xml")

        if (xml.contains("android:name=\"$name\"")) {
            return
        }

        val metaDataXml = """
            <meta-data
                android:name="$name"
                android:value="$value"/>
        """.trimIndent()

        val modifiedXml = xml.replaceFirst(
            "</application>",
            "$metaDataXml\n</application>"
        )

        val encoder = aXMLEncoder()
        val encoded = encoder.encodeString(context, modifiedXml)
        manifestFile.writeBytes(encoded)
    }

    fun setVersionCode(
        context: Context,
        manifestFile: File,
        versionCode: Int
    ) {
        val xml = aXMLDecoder(manifestFile.inputStream()).decodeAsString()
            ?: error("Failed to decode AndroidManifest.xml")

        val versionCodeRegex = Regex("""android:versionCode\s*=\s*["'](\d+)["']""")

        val modifiedXml = if (versionCodeRegex.containsMatchIn(xml)) {
            versionCodeRegex.replace(xml) { "android:versionCode=\"$versionCode\"" }
        } else {
            xml.replaceFirst(
                "<manifest",
                "<manifest android:versionCode=\"$versionCode\""
            )
        }

        val encoder = aXMLEncoder()
        val encoded = encoder.encodeString(context, modifiedXml)
        manifestFile.writeBytes(encoded)
    }

    fun setDebuggable(
        context: Context,
        manifestFile: File,
        debuggable: Boolean
    ) {
        val xml = aXMLDecoder(manifestFile.inputStream()).decodeAsString()
            ?: error("Failed to decode AndroidManifest.xml")

        val debuggableRegex = Regex("""android:debuggable\s*=\s*["'](true|false)["']""")
        val debuggableValue = debuggable.toString()

        val modifiedXml = if (debuggableRegex.containsMatchIn(xml)) {
            debuggableRegex.replace(xml) { "android:debuggable=\"$debuggableValue\"" }
        } else {
            xml.replaceFirst(
                "<application",
                "<application android:debuggable=\"$debuggableValue\""
            )
        }

        val encoder = aXMLEncoder()
        val encoded = encoder.encodeString(context, modifiedXml)
        manifestFile.writeBytes(encoded)
    }
}
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
            return // already injected
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
}

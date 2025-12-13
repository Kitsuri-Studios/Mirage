package io.kitsuri.m1rage.utils

import android.content.Context
import android.util.Log
import com.apk.axml.aXMLDecoder
import org.xmlpull.v1.XmlPullParser
import android.util.Xml
import java.io.File
import java.io.FileInputStream

/**
 * Binary AndroidManifest.xml parser utility
 */
object ManifestParser {

    private const val TAG = "ManifestParser"

    /**
     * Find the launcher activity from a binary AndroidManifest.xml file
     *
     * @param manifestFile Binary AndroidManifest.xml file
     * @return Fully qualified launcher activity name, or null if not found
     */
    fun findLauncherActivity(manifestFile: File): String? {
        if (!manifestFile.exists()) {
            Log.e(TAG, "Manifest file does not exist: ${manifestFile.absolutePath}")
            return null
        }

        try {
            // Decode binary XML to string using aXMLDecoder
            FileInputStream(manifestFile).use { inputStream ->
                val decoder = aXMLDecoder(inputStream)
                val xmlString = decoder.decodeAsString()

                if (xmlString.isNullOrEmpty()) {
                    Log.e(TAG, "Failed to decode manifest - empty result")
                    return null
                }

                Log.i(TAG, "Successfully decoded binary manifest (${xmlString.length} chars)")

                // Parse the decoded XML string
                return parseManifestXml(xmlString)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing binary manifest", e)
            e.printStackTrace()
            return null
        }
    }

    /**
     * Parse XML string to find launcher activity
     */
    private fun parseManifestXml(xmlString: String): String? {
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(java.io.StringReader(xmlString))

            var packageName = ""
            var launcherActivity: String? = null
            var currentActivity: String? = null
            var currentActivityType: String? = null
            var hasMainAction = false
            var hasLauncherCategory = false
            var insideIntentFilter = false

            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tagName = parser.name

                        when (tagName) {
                            "manifest" -> {
                                // Extract package name
                                packageName = getAttributeValue(parser, "package") ?: ""
                                Log.i(TAG, "Package name: $packageName")
                            }

                            "activity", "activity-alias" -> {
                                // Extract activity name
                                currentActivity = getAttributeValue(parser, "name")
                                currentActivityType = tagName
                                hasMainAction = false
                                hasLauncherCategory = false
                                insideIntentFilter = false

                                if (currentActivity != null) {
                                    Log.d(TAG, "Found $tagName: $currentActivity")
                                }
                            }

                            "intent-filter" -> {
                                insideIntentFilter = true
                            }

                            "action" -> {
                                if (insideIntentFilter) {
                                    val actionName = getAttributeValue(parser, "name")
                                    if (actionName == "android.intent.action.MAIN") {
                                        hasMainAction = true
                                        Log.d(TAG, "  → Has MAIN action")
                                    }
                                }
                            }

                            "category" -> {
                                if (insideIntentFilter) {
                                    val categoryName = getAttributeValue(parser, "name")
                                    if (categoryName == "android.intent.category.LAUNCHER") {
                                        hasLauncherCategory = true
                                        Log.d(TAG, "  → Has LAUNCHER category")
                                    }
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        val tagName = parser.name

                        when (tagName) {
                            "intent-filter" -> {
                                insideIntentFilter = false
                            }

                            "activity", "activity-alias" -> {
                                // Check if this activity is the launcher
                                if (currentActivity != null && hasMainAction && hasLauncherCategory) {
                                    launcherActivity = currentActivity
                                    Log.i(TAG, "✓ Found launcher activity: $launcherActivity")
                                }
                                currentActivity = null
                                currentActivityType = null
                            }
                        }
                    }
                }

                eventType = parser.next()
            }

            if (launcherActivity == null) {
                Log.e(TAG, "No launcher activity found in manifest")
                return null
            }

            // Resolve full activity name
            val fullActivityName = resolveActivityName(launcherActivity, packageName)
            Log.i(TAG, "Resolved launcher activity: $fullActivityName")

            return fullActivityName

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing XML string", e)
            e.printStackTrace()
            return null
        }
    }

    /**
     * Get attribute value from XmlPullParser, handling namespaces
     */
    private fun getAttributeValue(parser: XmlPullParser, attributeName: String): String? {
        // Try without namespace first
        var value = parser.getAttributeValue(null, attributeName)

        if (value == null) {
            // Try with android namespace
            value = parser.getAttributeValue("http://schemas.android.com/apk/res/android", attributeName)
        }

        if (value == null) {
            // Try searching all attributes
            for (i in 0 until parser.attributeCount) {
                val name = parser.getAttributeName(i)
                if (name == attributeName || name.endsWith(":$attributeName")) {
                    value = parser.getAttributeValue(i)
                    break
                }
            }
        }

        return value
    }

    /**
     * Resolve relative activity names to fully qualified names
     */
    private fun resolveActivityName(activityName: String, packageName: String): String {
        return when {
            activityName.startsWith(".") -> {
                // Relative name starting with dot
                packageName + activityName
            }
            activityName.contains(".") -> {
                // Already has package qualifier or is fully qualified
                if (activityName.startsWith(packageName)) {
                    // Already fully qualified
                    activityName
                } else {
                    // Might be from a different package (library)
                    activityName
                }
            }
            else -> {
                // Simple name without any dots
                "$packageName.$activityName"
            }
        }
    }

    /**
     * Check if XML string is valid
     */
    fun isXmlValid(xmlString: String): Boolean {
        return try {
            javax.xml.parsers.SAXParserFactory.newInstance()
                .newSAXParser()
                .xmlReader
                .parse(org.xml.sax.InputSource(java.io.StringReader(xmlString)))
            true
        } catch (e: Exception) {
            Log.w(TAG, "Invalid XML", e)
            false
        }
    }
}
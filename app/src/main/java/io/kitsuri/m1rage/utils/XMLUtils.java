package io.kitsuri.m1rage.utils;

import android.util.Log;

import com.apk.axml.aXMLEncoder;
import com.apk.axml.serializableItems.ResEntry;
import com.apk.axml.serializableItems.XMLEntry;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

public class XMLUtils {

    private static final String TAG = "XMLUtils";

    public static boolean isValidXml(String xml) {
        try {
            SAXParserFactory.newInstance().newSAXParser().getXMLReader()
                    .parse(new InputSource(new StringReader(xml)));
            return true;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            Log.w(TAG, "Invalid XML", e);
            return false;
        }
    }

    public static String getFileExtension(String path) {
        if (path == null || path.isEmpty()) return null;
        String normalized = path.replace("\\", "/");
        int slash = normalized.lastIndexOf('/');
        String name = slash == -1 ? normalized : normalized.substring(slash + 1);
        int dot = name.lastIndexOf('.');
        return (dot == -1 || dot == name.length() - 1) ? null : name.substring(dot + 1);
    }

    public static String entriesToXmlString(List<XMLEntry> entries, List<ResEntry> resEntries) {
        StringBuilder sb = new StringBuilder();
        for (XMLEntry entry : entries) {
            if (entry.isEmpty()) continue;
            String tag = entry.getTag().trim();
            if ("android:debuggable".equals(tag) || "android:testOnly".equals(tag)) continue;

            if (resEntries != null && !resEntries.isEmpty()) {
                sb.append(entry.getText(resEntries));
            } else {
                sb.append(entry.getText());
            }
            sb.append('\n');
        }
        return sb.toString().trim();
    }

    public static boolean encodeToBinaryXml(String xmlContent, String outputPath) {
        if (!isValidXml(xmlContent)) {
            Log.e(TAG, "XML is corrupted or invalid");
            return false;
        }

        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            aXMLEncoder encoder = new aXMLEncoder();
            byte[] data = encoder.encodeString(null, xmlContent);
            fos.write(data);
            return true;
        } catch (IOException | XmlPullParserException e) {
            Log.e(TAG, "Failed to encode binary XML", e);
            return false;
        }
    }
}
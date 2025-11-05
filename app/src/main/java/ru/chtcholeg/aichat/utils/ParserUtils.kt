package ru.chtcholeg.aichat.utils

import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

class ParserUtils {

    fun parseJson(jsonString: String): Map<String, Any?> {
        val result = mutableMapOf<String, Any>()
        val jsonObject = JSONObject(jsonString)

        jsonObject.keys().forEach { key ->
            result[key] = when (val value = jsonObject.get(key)) {
                is JSONObject -> parseJson(value.toString())
                is String -> value
                is Number -> value
                is Boolean -> value
                else -> value.toString()
            }
        }

        return result
    }

    fun parseXml(xmlString: String): Map<String, Any?> {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xmlString))

        val result = mutableMapOf<String, Any>()
        var currentTag: String? = null
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                }

                XmlPullParser.TEXT -> {
                    val text = parser.text.trim()
                    if (text.isNotEmpty() && currentTag != null) {
                        result[currentTag!!] = text
                        currentTag = null
                    }
                }
            }
            eventType = parser.next()
        }

        return result
    }
}

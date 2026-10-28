package com.example.data

/**
 * Lightweight, zero-dependency, pure-Kotlin RFC 8259 JSON parser and serializer.
 * Works identically on Android runtime and pure JVM unit tests (where Android's
 * stubbed `org.json.JSONObject` is not available without Robolectric).
 */
class SimpleJsonObject(
    private val map: MutableMap<String, Any?> = LinkedHashMap()
) {
    fun keys(): Iterator<String> = map.keys.iterator()

    fun keySet(): Set<String> = map.keys

    fun has(key: String): Boolean = map.containsKey(key)

    fun isNull(key: String): Boolean = !map.containsKey(key) || map[key] == null

    fun opt(key: String): Any? = map[key]

    fun optString(key: String, fallback: String = ""): String {
        val v = map[key] ?: return fallback
        return when (v) {
            is String -> v
            is Number, is Boolean -> v.toString()
            else -> fallback
        }
    }

    fun getString(key: String): String {
        val v = map[key] ?: throw IllegalArgumentException("Missing string key '$key'")
        return v.toString()
    }

    fun optDouble(key: String, fallback: Double = Double.NaN): Double {
        val v = map[key] ?: return fallback
        return when (v) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull() ?: fallback
            else -> fallback
        }
    }

    fun getDouble(key: String): Double {
        val v = optDouble(key, Double.NaN)
        if (v.isNaN()) throw IllegalArgumentException("Invalid or missing double for key '$key'")
        return v
    }

    fun optInt(key: String, fallback: Int = 0): Int {
        val v = map[key] ?: return fallback
        return when (v) {
            is Number -> v.toInt()
            is String -> v.toIntOrNull() ?: fallback
            else -> fallback
        }
    }

    fun getInt(key: String): Int {
        val v = map[key] ?: throw IllegalArgumentException("Missing int key '$key'")
        return when (v) {
            is Number -> v.toInt()
            is String -> v.toInt()
            else -> throw IllegalArgumentException("Invalid int for key '$key'")
        }
    }

    fun optLong(key: String, fallback: Long = 0L): Long {
        val v = map[key] ?: return fallback
        return when (v) {
            is Number -> v.toLong()
            is String -> v.toLongOrNull() ?: fallback
            else -> fallback
        }
    }

    fun getLong(key: String): Long {
        val v = map[key] ?: throw IllegalArgumentException("Missing long key '$key'")
        return when (v) {
            is Number -> v.toLong()
            is String -> v.toLong()
            else -> throw IllegalArgumentException("Invalid long for key '$key'")
        }
    }

    fun optBoolean(key: String, fallback: Boolean = false): Boolean {
        val v = map[key] ?: return fallback
        return when (v) {
            is Boolean -> v
            is String -> v.toBooleanStrictOrNull() ?: fallback
            else -> fallback
        }
    }

    fun getBoolean(key: String): Boolean {
        val v = map[key] ?: throw IllegalArgumentException("Missing boolean key '$key'")
        return when (v) {
            is Boolean -> v
            is String -> v.toBooleanStrict()
            else -> throw IllegalArgumentException("Invalid boolean for key '$key'")
        }
    }

    fun optJSONObject(key: String): SimpleJsonObject? {
        return map[key] as? SimpleJsonObject
    }

    fun optJSONArray(key: String): SimpleJsonArray? {
        return map[key] as? SimpleJsonArray
    }

    companion object {
        fun parse(jsonStr: String): SimpleJsonObject {
            val parser = SimpleJsonTokenizer(jsonStr)
            val result = parser.parseValue()
            return result as? SimpleJsonObject
                ?: throw IllegalArgumentException("Expected root JSON object")
        }
    }
}

class SimpleJsonArray(
    private val list: MutableList<Any?> = ArrayList()
) {
    fun length(): Int = list.size

    fun opt(index: Int): Any? = list.getOrNull(index)

    fun optString(index: Int, fallback: String = ""): String {
        val v = list.getOrNull(index) ?: return fallback
        return when (v) {
            is String -> v
            is Number, is Boolean -> v.toString()
            else -> fallback
        }
    }

    fun optJSONObject(index: Int): SimpleJsonObject? {
        return list.getOrNull(index) as? SimpleJsonObject
    }
}

private class SimpleJsonTokenizer(private val src: String) {
    private var pos = 0
    private val len = src.length

    fun parseValue(): Any? {
        skipWhitespace()
        if (pos >= len) throw IllegalArgumentException("Unexpected end of JSON")
        return when (val ch = src[pos]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't', 'f' -> parseBoolean()
            'n' -> parseNull()
            '-', '+', in '0'..'9' -> parseNumber()
            else -> throw IllegalArgumentException("Unexpected char '$ch' at position $pos")
        }
    }

    private fun parseObject(): SimpleJsonObject {
        expect('{')
        val map = LinkedHashMap<String, Any?>()
        skipWhitespace()
        if (pos < len && src[pos] == '}') {
            pos++
            return SimpleJsonObject(map)
        }
        while (pos < len) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            expect(':')
            val value = parseValue()
            map[key] = value
            skipWhitespace()
            if (pos < len && src[pos] == ',') {
                pos++
            } else if (pos < len && src[pos] == '}') {
                pos++
                return SimpleJsonObject(map)
            } else {
                throw IllegalArgumentException("Expected ',' or '}' in object at position $pos")
            }
        }
        throw IllegalArgumentException("Unterminated JSON object")
    }

    private fun parseArray(): SimpleJsonArray {
        expect('[')
        val list = ArrayList<Any?>()
        skipWhitespace()
        if (pos < len && src[pos] == ']') {
            pos++
            return SimpleJsonArray(list)
        }
        while (pos < len) {
            val value = parseValue()
            list.add(value)
            skipWhitespace()
            if (pos < len && src[pos] == ',') {
                pos++
            } else if (pos < len && src[pos] == ']') {
                pos++
                return SimpleJsonArray(list)
            } else {
                throw IllegalArgumentException("Expected ',' or ']' in array at position $pos")
            }
        }
        throw IllegalArgumentException("Unterminated JSON array")
    }

    private fun parseString(): String {
        expect('"')
        val sb = StringBuilder()
        while (pos < len) {
            val ch = src[pos++]
            when (ch) {
                '"' -> return sb.toString()
                '\\' -> {
                    if (pos >= len) throw IllegalArgumentException("Unterminated escape sequence")
                    when (val esc = src[pos++]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (pos + 4 > len) throw IllegalArgumentException("Invalid unicode escape")
                            val hex = src.substring(pos, pos + 4)
                            sb.append(hex.toInt(16).toChar())
                            pos += 4
                        }
                        else -> sb.append(esc)
                    }
                }
                else -> sb.append(ch)
            }
        }
        throw IllegalArgumentException("Unterminated string literal")
    }

    private fun parseNumber(): Number {
        val start = pos
        while (pos < len && (src[pos].isDigit() || src[pos] in "+-.eE")) {
            pos++
        }
        val numStr = src.substring(start, pos)
        return if (numStr.contains('.') || numStr.contains('e') || numStr.contains('E')) {
            numStr.toDouble()
        } else {
            numStr.toLongOrNull() ?: numStr.toDouble()
        }
    }

    private fun parseBoolean(): Boolean {
        return if (src.startsWith("true", pos)) {
            pos += 4
            true
        } else if (src.startsWith("false", pos)) {
            pos += 5
            false
        } else {
            throw IllegalArgumentException("Invalid boolean token at $pos")
        }
    }

    private fun parseNull(): Any? {
        if (src.startsWith("null", pos)) {
            pos += 4
            return null
        }
        throw IllegalArgumentException("Invalid null token at $pos")
    }

    private fun skipWhitespace() {
        while (pos < len && src[pos].isWhitespace()) {
            pos++
        }
    }

    private fun expect(expected: Char) {
        skipWhitespace()
        if (pos >= len || src[pos] != expected) {
            throw IllegalArgumentException("Expected '$expected' at position $pos")
        }
        pos++
    }
}

package com.example.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import com.example.data.StructuredSymbol
import com.example.data.StructuredUnit
import java.util.Locale

/**
 * Shared rich-text formatting helper for superscripts, subscripts, scientific notation,
 * chemical formulas, and physical units using Jetpack Compose [AnnotatedString] and [SpanStyle]
 * rather than look-alike Unicode characters (Section 1.4).
 */
object RichTextHelper {

    val MathFontFamily: FontFamily = FontFamily.SansSerif

    private val SUPERSCRIPT_STYLE = SpanStyle(
        baselineShift = BaselineShift.Superscript,
        fontSize = 0.72.em,
        fontWeight = FontWeight.Bold
    )

    private val SUBSCRIPT_STYLE = SpanStyle(
        baselineShift = BaselineShift.Subscript,
        fontSize = 0.72.em,
        fontWeight = FontWeight.SemiBold
    )

    private val UNICODE_SUP_TO_ASCII = mapOf(
        '⁰' to '0', '¹' to '1', '²' to '2', '³' to '3', '⁴' to '4',
        '⁵' to '5', '⁶' to '6', '⁷' to '7', '⁸' to '8', '⁹' to '9',
        '⁺' to '+', '⁻' to '−', 'ⁿ' to 'n'
    )

    private val UNICODE_SUB_TO_ASCII = mapOf(
        '₀' to '0', '₁' to '1', '₂' to '2', '₃' to '3', '₄' to '4',
        '₅' to '5', '₆' to '6', '₇' to '7', '₈' to '8', '₉' to '9',
        '₊' to '+', '₋' to '−'
    )

    fun AnnotatedString.Builder.appendSuperscript(text: String) {
        if (text.isEmpty()) return
        withStyle(SUPERSCRIPT_STYLE) {
            append(text.replace("-", "−"))
        }
    }

    fun AnnotatedString.Builder.appendSubscript(text: String) {
        if (text.isEmpty()) return
        withStyle(SUBSCRIPT_STYLE) {
            append(text.replace("-", "−"))
        }
    }

    /**
     * Formats a mantissa and exponent as `mantissa × 10` with a rich superscript `exponent`.
     */
    fun formatScientific(mantissa: String, exponent: String?): AnnotatedString {
        return buildAnnotatedString {
            val cleanMantissa = mantissa.replace("-", "−")
            if (exponent == null) {
                append(cleanMantissa)
            } else {
                if (cleanMantissa.isNotEmpty()) {
                    append(cleanMantissa)
                    append(" × 10")
                } else {
                    append("10")
                }
                val expDisplay = exponent.ifEmpty { "□" }
                appendSuperscript(expDisplay)
            }
        }
    }

    /**
     * Formats numeric mantissa and integer exponent into an [AnnotatedString].
     */
    fun formatScientificNumber(
        mantissa: Double,
        exponent: Int,
        omitZeroExponent: Boolean = true
    ): AnnotatedString {
        val mStr = formatCleanDouble(mantissa)
        if (exponent == 0 && omitZeroExponent) {
            return AnnotatedString(mStr)
        }
        return formatScientific(mStr, exponent.toString())
    }

    /**
     * Formats a [StructuredSymbol] (e.g., k_e, ε_0, μ_0, N_A) with rich subscript/superscript spans.
     * Uses the short display symbol by default (`shortForm = true`), or the full symbol (`R = N_A k_B`)
     * when `shortForm = false` (e.g. in the Revision detail view).
     */
    fun formatSymbol(symbol: StructuredSymbol, shortForm: Boolean = true): AnnotatedString {
        if (!shortForm && !symbol.rawFullSymbol.isNullOrBlank()) {
            return formatRawSymbol(symbol.rawFullSymbol, shortForm = false)
        }
        val shortDisplay = symbol.shortDisplaySymbol
        if (!shortDisplay.isNullOrBlank() && (shortDisplay.contains('_') || shortDisplay.contains('^') ||
                shortDisplay.contains(' ') || shortDisplay.contains('/') ||
                shortDisplay.any { it in UNICODE_SUP_TO_ASCII || it in UNICODE_SUB_TO_ASCII })
        ) {
            return formatRawSymbol(shortDisplay, shortForm = true)
        }
        return buildAnnotatedString {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)) {
                append(formatMathText(symbol.base))
            }
            if (!symbol.subscript.isNullOrEmpty()) {
                appendSubscript(formatMathText(symbol.subscript).text)
            }
            if (!symbol.superscript.isNullOrEmpty()) {
                appendSuperscript(formatMathText(symbol.superscript).text)
            }
        }
    }

    /**
     * Normalizes a raw symbol string from `hsc_physics_chemistry_constants.json` into a rich
     * [AnnotatedString] with subscripts and superscripts (Section 3 & Phase 2).
     * When [shortForm] is `true`, extracts the short symbol before the first `=` or `(`.
     */
    fun formatRawSymbol(rawSymbol: String, shortForm: Boolean = true): AnnotatedString {
        val target = if (shortForm) {
            StructuredSymbol.extractShortSymbol(rawSymbol)
        } else {
            rawSymbol.trim()
        }
        return buildAnnotatedString {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)) {
                append(formatMathText(target))
            }
        }
    }

    /**
     * Formats a [StructuredUnit] (e.g. N·m^2·C^-2, m/s², (mol L^-1)^2, cal_th, or "no unit")
     * using rich superscript and subscript spans (Section 3 & Phase 2).
     */
    fun formatUnit(unit: StructuredUnit): AnnotatedString {
        if (unit.isDimensionless) return AnnotatedString("no unit")
        val raw = unit.rawUnitString?.trim()
        if (!raw.isNullOrEmpty()) {
            return formatRawUnit(raw)
        }
        if (unit.factors.isEmpty()) return AnnotatedString("no unit")
        return buildAnnotatedString {
            unit.factors.forEachIndexed { idx, factor ->
                if (idx > 0) append("·")
                append(formatMathText(factor.symbol))
                if (factor.exponent != 1) {
                    appendSuperscript(factor.exponent.toString())
                }
            }
        }
    }

    /**
     * Normalizes any raw unit string from `hsc_physics_chemistry_constants.json` into a rich
     * [AnnotatedString] with real superscript and subscript spans:
     * - `"dimensionless"`, `"1"`, `"no unit"`, or blank -> `"no unit"`
     * - Space-separated factors (`"m^3 kg^-1 s^-2"`, `"J mol^-1 K^-1"`) -> `"m³·kg⁻¹·s⁻²"` (joined with `·`)
     * - Bracketed exponents (`"(mol L^-1)^2"`) -> `"(mol·L⁻¹)²"`
     * - Slashes (`"MeV/c^2"`, `"W/m²"`, `"J/kg"`, `"m/s²"`) -> `"MeV/c²"`, `"W/m²"`
     * - Subscripts (`"cal_th"`, `"Hz (cR_∞)"`) -> `"cal"` with subscript `"th"`
     * - Special units (`"°C"`, `"°F"`, `"Å"`, `"Ω m"`) preserved with proper rich formatting.
     */
    fun formatRawUnit(rawUnit: String): AnnotatedString {
        val trimmed = rawUnit.trim()
        if (trimmed.isEmpty() ||
            trimmed.equals("dimensionless", ignoreCase = true) ||
            trimmed == "1" ||
            trimmed.equals("no unit", ignoreCase = true)
        ) {
            return AnnotatedString("no unit")
        }

        val normalized = normalizeUnitSpacingToMidDots(trimmed)
        return formatMathText(normalized)
    }

    private fun normalizeUnitSpacingToMidDots(unitStr: String): String {
        // Keep explanatory parenthetical notes separated by space (e.g. "Hz (cR_∞)", "statC (esu)")
        val parenNoteIdx = unitStr.indexOf(" (")
        if (parenNoteIdx > 0) {
            val left = normalizeUnitSpacingToMidDots(unitStr.substring(0, parenNoteIdx))
            val right = unitStr.substring(parenNoteIdx) // keep leading space before '('
            return left + right
        }

        // Convert spaces inside or between unit factors into middots '·'
        val sb = StringBuilder()
        var i = 0
        while (i < unitStr.length) {
            val ch = unitStr[i]
            if (ch == ' ') {
                while (i < unitStr.length && unitStr[i] == ' ') i++
                if (sb.isNotEmpty() && i < unitStr.length) {
                    val prev = sb.last()
                    val next = unitStr[i]
                    if (prev != '/' && next != '/') {
                        sb.append('·')
                    }
                }
            } else {
                sb.append(ch)
                i++
            }
        }
        return sb.toString()
    }

    /**
     * Formats a constant value together with its unit using rich spans.
     */
    fun formatValueWithUnit(
        mantissa: Double,
        exponent: Int,
        unit: StructuredUnit,
        omitZeroExponent: Boolean = true
    ): AnnotatedString {
        return buildAnnotatedString {
            append(formatScientificNumber(mantissa, exponent, omitZeroExponent))
            if (!unit.isDimensionless) {
                append(" ")
                append(formatUnit(unit))
            }
        }
    }

    /**
     * Formats the live user input line during gameplay, rendering `×10^` exponent entry
     * with a real superscript span.
     */
    fun formatInputDisplay(rawInput: String): AnnotatedString {
        if (rawInput.isEmpty()) return AnnotatedString("_")
        val marker = "×10^"
        val idx = rawInput.indexOf(marker)
        if (idx >= 0) {
            val mantissa = rawInput.substring(0, idx)
            val exp = rawInput.substring(idx + marker.length)
            return formatScientific(mantissa, exp)
        }
        return AnnotatedString(rawInput.replace("-", "−"))
    }

    /**
     * Parses a general math/science display string into an [AnnotatedString] with real
     * [BaselineShift.Superscript] and [BaselineShift.Subscript] spans.
     *
     * Supports:
     * - Explicit braces: `x^{2}`, `10^{-5}`, `log_{10}`, `k_{e}`
     * - Caret/underscore shorthand: `10^-5`, `x^2`, `log_10`, `H_2SO_4`
     * - Legacy Unicode superscript/subscript characters (converted to ASCII inside rich spans)
     */
    fun formatMathText(text: String): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")
        return buildAnnotatedString {
            var i = 0
            val n = text.length
            while (i < n) {
                val c = text[i]
                when {
                    c == '^' && i + 1 < n -> {
                        if (text[i + 1] == '{') {
                            val end = text.indexOf('}', i + 2)
                            if (end > i + 1) {
                                appendSuperscript(text.substring(i + 2, end))
                                i = end + 1
                            } else {
                                append(c)
                                i++
                            }
                        } else if (text[i + 1] == '(') {
                            val end = text.indexOf(')', i + 2)
                            if (end > i + 1) {
                                appendSuperscript(text.substring(i + 2, end))
                                i = end + 1
                            } else {
                                append(c)
                                i++
                            }
                        } else {
                            val start = i + 1
                            var end = start
                            if (end < n && (text[end] == '-' || text[end] == '+' || text[end] == '−')) {
                                end++
                            }
                            while (end < n && (text[end].isLetterOrDigit() || text[end] == '/' || text[end] == '.' || text[end] == '-' || text[end] == '+' || text[end] == '−')) {
                                end++
                            }
                            if (end > start) {
                                appendSuperscript(text.substring(start, end))
                                i = end
                            } else {
                                append(c)
                                i++
                            }
                        }
                    }

                    c == '_' && i + 1 < n && text[i + 1] != '_' -> {
                        if (text[i + 1] == '{') {
                            val end = text.indexOf('}', i + 2)
                            if (end > i + 1) {
                                appendSubscript(text.substring(i + 2, end))
                                i = end + 1
                            } else {
                                append(c)
                                i++
                            }
                        } else if (text[i + 1] == '(') {
                            val end = text.indexOf(')', i + 2)
                            if (end > i + 1) {
                                appendSubscript(text.substring(i + 2, end))
                                i = end + 1
                            } else {
                                append(c)
                                i++
                            }
                        } else {
                            val start = i + 1
                            var end = start
                            while (end < n && !text[end].isWhitespace() && text[end] !in "()[]{}=;:/^") {
                                end++
                            }
                            if (end > start) {
                                appendSubscript(text.substring(start, end).replace("_", " "))
                                i = end
                            } else {
                                append(c)
                                i++
                            }
                        }
                    }

                    c in UNICODE_SUP_TO_ASCII -> {
                        val sb = StringBuilder()
                        while (i < n && text[i] in UNICODE_SUP_TO_ASCII) {
                            sb.append(UNICODE_SUP_TO_ASCII[text[i]])
                            i++
                        }
                        appendSuperscript(sb.toString())
                    }

                    c in UNICODE_SUB_TO_ASCII -> {
                        val sb = StringBuilder()
                        while (i < n && text[i] in UNICODE_SUB_TO_ASCII) {
                            sb.append(UNICODE_SUB_TO_ASCII[text[i]])
                            i++
                        }
                        appendSubscript(sb.toString())
                    }

                    else -> {
                        append(c)
                        i++
                    }
                }
            }
        }
    }

    fun formatCleanDouble(value: Double, maxDecimals: Int = 4): String {
        val longVal = value.toLong()
        if (kotlin.math.abs(value - longVal.toDouble()) < 1e-9) {
            return longVal.toString()
        }
        return String.format(Locale.US, "%.${maxDecimals}f", value)
            .trimEnd('0')
            .trimEnd('.')
    }
}

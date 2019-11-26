package com.monnage.gradle.report

import com.monnage.gradle.extensions.logging.LoggingExtension
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.LogLevel.*
import org.gradle.api.logging.Logger

internal class FormattedOutput(private val logging: LoggingExtension, private val logger: Logger) {

    enum class Icons(val id: String, val code: String, val alternativeText: String) {
        ROCKET(":rocket:", "\uD83D\uDE80", "-"),
        SUCCESS(":success:", "\u2714 ", "[OK]"),
        FAILED(":failed:", "\uD83D\uDE21", "[FAIL]"),
        SLEEP(":sleep:", "\uD83D\uDCA4", "[SKIP]"),
        SAND_WATCH(":sand_watch:", "\u23F3", "[RUN]")
    }

    fun format(string: String, code: Int, bold: Boolean, italic: Boolean): String {
        var format = "m"
        if (bold && textStylesEnabled()) format = ";1m"
        if (italic && textStylesEnabled()) format = ";3m"
        return if (colorsEnabled()) 27.toChar() + "[$code$format$string" + 27.toChar() + "[0m" else string
    }

    fun display(str: String, indentLevel: Int = 0, logLevel: LogLevel = QUIET) {
        val padding = if (indentLevel > 0) " ".repeat(indentLevel) else ""
        var value = padding + str
        Icons.values().forEach {
            value = if (iconsEnabled()) value.replace(it.id, it.code) else value.replace(it.id, it.alternativeText)
        }
        when (logLevel) {
            ERROR -> logger.error(value)
            QUIET -> logger.quiet(value)
            LIFECYCLE -> logger.lifecycle(value)
            WARN -> logger.warn(value)
            INFO -> logger.info(value)
            DEBUG -> logger.debug(value)
        }
    }

    private fun iconsEnabled() = !logging.noIcon
    private fun colorsEnabled() = !logging.noColor
    private fun textStylesEnabled() = !logging.noFontStyling
}


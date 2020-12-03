package com.monnage.gradle.report

internal fun String.green(
    output: FormattedOutput,
    bold: Boolean = false,
    italic: Boolean = false
): String = output.format(this, 92, bold, italic)

internal fun String.red(
    output: FormattedOutput,
    bold: Boolean = false,
    italic: Boolean = false
): String = output.format(this, 91, bold, italic)

internal fun String.gray(
    output: FormattedOutput,
    bold: Boolean = false,
    italic: Boolean = false
): String = output.format(this, 90, bold, italic)

internal fun String.blue(
    output: FormattedOutput,
    bold: Boolean = false,
    italic: Boolean = false
): String = output.format(this, 94, bold, italic)

internal fun String.yellow(
    output: FormattedOutput,
    bold: Boolean = false,
    italic: Boolean = false
): String = output.format(this, 93, bold, italic)

package com.monnage.gradle.report

internal fun String.green(
    output: FormattedOutput,
    bold: Boolean = false,
    italic: Boolean = false
): String = output.format(this, 32, bold, italic)

internal fun String.red(
    output: FormattedOutput,
    bold: Boolean = false,
    italic: Boolean = false
): String = output.format(this, 31, bold, italic)

internal fun String.gray(
    output: FormattedOutput,
    bold: Boolean = false,
    italic: Boolean = false
): String = output.format(this, 90, bold, italic)

package com.monnage.gradle.extensions.logging

import com.monnage.gradle.extensions.framework.SpringBootExtension
import org.gradle.api.Action
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

class LoggingExtension {

    internal val level = LoggingLevelExtension()
    internal val spring = SpringBootExtension()

    internal var plain = false
    internal var noColor = false
    internal var noIcon = false
    internal var noFontStyling = false

    var exceptionFormat = TestExceptionFormat.SHORT

    fun noColor() = run { noColor = true }
    fun noIcon() = run { noIcon = true }
    fun noFontStyling() = run { noFontStyling = true }
    fun plain() = run { noColor = true; noIcon = true; noFontStyling = true }

    fun level(action: Action<in LoggingLevelExtension>) {
        action.execute(level)
    }

    fun springBoot(action: Action<in SpringBootExtension>) {
        action.execute(spring)
    }
}
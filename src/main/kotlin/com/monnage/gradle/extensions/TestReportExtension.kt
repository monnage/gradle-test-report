package com.monnage.gradle.extensions

import com.monnage.gradle.extensions.framework.FrameworkMode
import com.monnage.gradle.extensions.logging.LoggingExtension
import org.gradle.api.Action

open class TestReportExtension {

    var enabled = true
    var framework = FrameworkMode.SPRING_BOOT

    internal val logging = LoggingExtension()

    fun logging(action: Action<in LoggingExtension>) {
        action.execute(logging)
    }
}

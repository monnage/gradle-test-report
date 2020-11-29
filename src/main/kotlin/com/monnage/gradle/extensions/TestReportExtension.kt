package com.monnage.gradle.extensions

import com.monnage.gradle.extensions.framework.FrameworkMode
import com.monnage.gradle.extensions.logging.LoggingExtension
import org.gradle.api.Action
import org.gradle.api.Project

open class TestReportExtension(val project: Project) {

    var enabled = true
    var parallel = false
    var projectName = project.name
    var framework = FrameworkMode.SPRING_BOOT

    internal val logging = LoggingExtension()

    fun logging(action: Action<in LoggingExtension>) {
        action.execute(logging)
    }
}

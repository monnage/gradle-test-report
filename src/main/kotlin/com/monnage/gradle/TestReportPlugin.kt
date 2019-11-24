package com.monnage.gradle

import com.monnage.gradle.actions.JavaPluginAction
import com.monnage.gradle.extensions.TestReportExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.closureOf

class TestReportPlugin : Plugin<Project> {

    companion object {
        val PLUGIN_ACTIONS = listOf(
                JavaPluginAction
        )
    }

    override fun apply(project: Project) {
        project.run {
            // Register extension
            val monnage = extensions.create("monnage", TestReportExtension::class.java).apply {
            }
            // Configure plugins if present
            PLUGIN_ACTIONS.forEach { pluginAction ->
                pluginAction.getPluginClass().let {
                    plugins.withType(it.java, closureOf<Plugin<out Project>> {
                        pluginAction.execute(project)
                        logger.lifecycle("[Monnage plugin] ${this.javaClass.simpleName} pre-configured")
                    })
                }
            }
        }

    }
}

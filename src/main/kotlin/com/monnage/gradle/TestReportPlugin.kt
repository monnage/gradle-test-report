package com.monnage.gradle

import com.monnage.gradle.actions.JavaPluginAction
import com.monnage.gradle.extensions.TestReportExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.closureOf

class TestReportPlugin : Plugin<Project> {

    companion object {
        val PLUGIN_ACTIONS = listOf(JavaPluginAction)
    }

    override fun apply(project: Project) {
        project.run {
            // Register extension DSL
            val testReport = extensions.create("testReport", TestReportExtension::class.java, project)

            // Configure plugins if present
            PLUGIN_ACTIONS.forEach { pluginAction ->
                pluginAction.getPluginClass().let {
                    plugins.withType(it.java, closureOf<Plugin<out Project>> {
                        pluginAction.execute(project)
//                        logger.lifecycle("[TestReport plugin] ${this.javaClass.simpleName} pre-configured")
                    })
                }
            }

            gradle.projectsEvaluated {
                configureExtensions(testReport)
            }
        }
    }

    /**
     * Override some properties with values from cmd if present.
     */
    private fun Project.configureExtensions(testReport: TestReportExtension) {
        findProperty("testReport.disabled")?.let {
            testReport.enabled = false
        }
        findProperty("test.logging.plain")?.let {
            testReport.logging {
                noIcon()
                noColor()
                noFontStyling()
            }
        }
        findProperty("test.logging.nocolor")?.let {
            testReport.logging {
                noColor()
            }
        }
        findProperty("test.logging.noicon")?.let {
            testReport.logging {
                noIcon()
            }
        }
        findProperty("test.logging.nostyling")?.let {
            testReport.logging {
                noFontStyling()
            }
        }
        findProperty("test.logging.spring.banner")?.let {
            testReport.logging {
                springBoot {
                    banner()
                }
            }
        }
        fun validateLogLevel(level: String): Boolean {
            val res = level.matches("(?:INFO|DEBUG|ERROR|WARN|TRACE)".toRegex())
            if (!res) {
                logger.error("'$level' is invalid log level")
            }
            return res
        }
        findProperty("test.logging.level.root")?.let {
            testReport.logging {
                level {
                    if (validateLogLevel(it.toString())) {
                        root(it.toString())
                    }
                }
            }
        }
        findProperty("test.logging.level")?.let {
            testReport.logging {
                level {
                    val loggingLevel: List<String>? = it.toString().split(",")
                    loggingLevel?.let {
                        it.forEach { log ->
                            val logPair = log.split(":")
                            if (logPair.size == 2 && validateLogLevel(logPair[1])) {
                                logger(logPair[0], logPair[1])
                            }
                        }
                    }
                }
            }
        }
    }
}

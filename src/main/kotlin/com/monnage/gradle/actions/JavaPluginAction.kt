package com.monnage.gradle.actions

import com.monnage.gradle.extensions.TestReportExtension
import com.monnage.gradle.report.TestReport
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import kotlin.reflect.KClass

object JavaPluginAction : PluginAction {

    override fun execute(project: Project) {

        val extension = project.extensions.getByType(TestReportExtension::class.java)

        project.run {
            tasks {
                named<Test>("test") {
                    val report = TestReport(this, extension)
                    doFirst {
                        report.apply()
                    }
                }
            }
        }
    }

    override fun getPluginClass(): KClass<out Plugin<out Project>> = JavaPlugin::class
}

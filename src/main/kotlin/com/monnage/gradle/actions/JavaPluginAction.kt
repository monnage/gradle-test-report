package com.monnage.gradle.actions

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.tasks.testing.DecoratingTestDescriptor
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestOutputEvent
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.kotlin.dsl.KotlinClosure1
import org.gradle.kotlin.dsl.KotlinClosure2
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import kotlin.reflect.KClass

object JavaPluginAction : PluginAction {

    override fun execute(project: Project) {
        project.run {
            tasks {
                named<Test>("test") {
                    useJUnitPlatform()
                    // ext props
                    val colored: Boolean = project.findProperty("test.logging.plain") == null
                    val loggingRootLevel: String = project.findProperty("test.logging.level.root")?.toString() ?: "WARN"
                    val loggingLevel: List<String>? = project.findProperty("test.logging.level")?.toString()?.split(",")
                    val logBanner: Boolean = project.findProperty("test.logging.banner") != null
                    // default logging config
                    loggingLevel?.let {
                        it.forEach { log ->
                            val logPair = log.split(":")
                            if (logPair.size == 2) {
                                systemProperty("logging.level.${logPair[0]}", logPair[1])
                            }
                        }
                    }
                    systemProperty("logging.level.root", loggingRootLevel)
                    systemProperty("spring.main.banner-mode", if (logBanner) "console" else "off")
                    testLogging {
                        logging.captureStandardOutput(LogLevel.LIFECYCLE)
                        exceptionFormat = TestExceptionFormat.SHORT
                    }
                    // helpers
                    fun format(string: String, code: Int, bold: Boolean, italic: Boolean): String {
                        var format = "m"
                        if (bold) format = ";1m"
                        if (italic) format = ";3m"
                        return if (colored) 27.toChar() + "[$code$format$string" + 27.toChar() + "[0m" else string
                    }

                    fun String.green(bold: Boolean = false, italic: Boolean = false): String = format(this, 32, bold, italic)
                    fun String.red(bold: Boolean = false, italic: Boolean = false): String = format(this, 31, bold, italic)
                    fun String.gray(bold: Boolean = false, italic: Boolean = false): String = format(this, 90, bold, italic)
                    fun evalTestResult(result: TestResult): String {
                        return when (result.resultType!!) {
                            TestResult.ResultType.SUCCESS -> if (colored) "\u2714 ".green() else "[SUCCESS]"
                            TestResult.ResultType.FAILURE -> if (colored) "\uD83D\uDE21".red() else "[FAILED]"
                            TestResult.ResultType.SKIPPED -> if (colored) "\uD83D\uDCA4".gray() else "[SKIPPED]"
                        }
                    }
                    // events
                    var firstLine = false
                    beforeSuite(KotlinClosure1<TestDescriptor, Unit>({
                        if (!firstLine) {
                            logger.quiet("")
                            firstLine = true
                        }
                        val desc = this as DecoratingTestDescriptor
                        if (parent == null) {
                            logger.quiet("${if (colored) "\uD83D\uDE80" else "-"} ${project.name}")
                        }
                        className?.let {
                            logger.quiet("    ${desc.classDisplayName} ${"($it)".gray(italic = true)}")
                        }
                    }))
                    beforeTest(KotlinClosure1<TestDescriptor, Unit>({
                        logger.lifecycle("     ${if (colored) "\u23F3" else "[RUNNING]"} ${if (name.endsWith("()")) name.substringBefore("()") else name}")
                    }))
                    fun elapsed(result: TestResult): String {
                        val elapsedMilis = result.endTime - result.startTime
                        var elapsedTime = "${elapsedMilis}ms"
                        if (elapsedMilis > 1000) {
                            elapsedTime = "${elapsedMilis / 1000}s"
                        }
                        if (elapsedMilis / 1000 > 60) {
                            val elapsedMinutes = elapsedMilis / 1000 / 60
                            val secondsRest = elapsedMilis / 1000 % 60
                            elapsedTime = "${elapsedMinutes}m ${secondsRest}s"
                        }
                        return elapsedTime
                    }
                    afterTest(KotlinClosure2<TestDescriptor, TestResult, Unit>({ desc, result ->
                        desc as DecoratingTestDescriptor
                        val name = if (desc.displayName.endsWith("()")) desc.displayName.substringBefore("()") else desc.displayName
                        logger.quiet("     ${evalTestResult(result)} $name " +
                                "[${elapsed(result)}]".gray(italic = true))
                    }))
                    afterSuite(KotlinClosure2<TestDescriptor, TestResult, Unit>({ desc, result ->
                        if (desc.parent == null) {
                            logger.quiet(("   [${result.testCount} total, ${result.successfulTestCount} succeeded, " +
                                    "${result.failedTestCount} failed, ${result.skippedTestCount} skipped, ${elapsed(result)}]\n").gray(italic = true))
                        }
                        Unit
                    }))
                    // filter and forward all messages from stdout to gradle
                    val noiseRegex = "\\[Test worker\\]\\s(?:DEBUG|INFO)".toRegex()
                    onOutput(KotlinClosure2<TestDescriptor, TestOutputEvent, Unit>(event@{ _, e ->
                        if (e.message.length > 32) {
                            val noise = e.message.substring(13, 32)
                            if (noise.matches(noiseRegex)) {
                                return@event
                            }
                        }
                        logger.lifecycle("\t   " + e.message.substring(0, e.message.length - 1))
                    }))

                }
            }
        }
    }

    override fun getPluginClass(): KClass<out Plugin<out Project>> = JavaPlugin::class
}

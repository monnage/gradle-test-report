package com.monnage.gradle.report

import com.monnage.gradle.extensions.TestReportExtension
import com.monnage.gradle.extensions.framework.FrameworkMode
import org.gradle.api.internal.tasks.testing.DecoratingTestDescriptor
import org.gradle.api.logging.LogLevel.LIFECYCLE
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestOutputEvent
import org.gradle.api.tasks.testing.TestResult
import org.gradle.kotlin.dsl.KotlinClosure1
import org.gradle.kotlin.dsl.KotlinClosure2

internal class TestReport(private val test: Test, private val config: TestReportExtension) {

    private val out = FormattedOutput(config.logging, test.logger)

    fun apply() {
        with(test) {
            if (config.enabled) {
                registerTestReport()
            }
        }
    }

    private fun Test.registerTestReport() {
        testLogging {
            logging.captureStandardOutput(LIFECYCLE)
            exceptionFormat = config.logging.exceptionFormat
        }
        // technology specific
        applyFrameworkSpecificSettings(config.framework)
        // events
        beforeSuiteReport()
        beforeTestReport()
        afterTestReport()
        afterSuiteReport()
    }

    private fun Test.beforeSuiteReport() {
        var firstEmptyLine = false
        beforeSuite(KotlinClosure1<DecoratingTestDescriptor, Unit>({
            if (!firstEmptyLine) {
                out.display("")
                firstEmptyLine = true
            }
            if (parent == null) {
                out.display(":rocket: ${project.name}")
            }
            className?.let {
                val formattedClassName = "($it)".gray(out, italic = true)
                out.display("$classDisplayName $formattedClassName", 4)
            }
        }))
    }

    private fun Test.beforeTestReport() {
        beforeTest(KotlinClosure1<DecoratingTestDescriptor, Unit>({
            val name = if (displayName.endsWith("()")) displayName.substringBefore("()") else displayName
            out.display(":sand_watch: $name", 5, LIFECYCLE)
        }))
    }

    private fun Test.afterTestReport() {
        afterTest(KotlinClosure2<DecoratingTestDescriptor, TestResult, Unit>({ desc, result ->
            val name = with(desc) {
                if (displayName.endsWith("()")) displayName.substringBefore("()") else displayName
            }
            val testResult = evalTestResult(result)
            val elapsed = "[${elapsed(result)}]".gray(out, italic = true)
            out.display("$testResult $name $elapsed", 5)
        }))
    }

    private fun Test.afterSuiteReport() {
        afterSuite(KotlinClosure2<TestDescriptor, TestResult, Unit>({ desc, result ->
            if (desc.parent == null) {
                with(result) {
                    out.display(("[$testCount total, $successfulTestCount succeeded, $failedTestCount failed, " +
                        "$skippedTestCount skipped, ${elapsed(this)}]\n").gray(out, italic = true), 3)
                }
            }
        }))
    }

    private fun Test.applyFrameworkSpecificSettings(framework: FrameworkMode) {
        when (framework) {
            FrameworkMode.SPRING_BOOT -> springBootSettings()
            else -> defaultSettings()
        }
    }

    private fun Test.defaultSettings() {
        onOutput(KotlinClosure2<TestDescriptor, TestOutputEvent, Unit>(event@{ _, e ->
            out.display(e.message.substring(0, e.message.length - 1), 7, LIFECYCLE)
        }))
    }

    private fun Test.springBootSettings() {
        systemProperty("logging.level.root", config.logging.level.root)
        systemProperty("spring.main.banner-mode", if (config.logging.spring.banner) "console" else "off")
        config.logging.level.loggers.forEach {
            systemProperty("logging.level.${it.key}", it.value)
        }

        // filter and forward all messages from stdout to gradle
        val noiseRegex = "\\[Test worker\\]\\s(?:DEBUG|INFO)".toRegex()
        onOutput(KotlinClosure2<TestDescriptor, TestOutputEvent, Unit>(event@{ _, e ->
            if (e.message.length > 32) {
                val noise = e.message.substring(13, 32)
                if (noise.matches(noiseRegex)) {
                    return@event
                }
            }
            out.display(e.message.substring(0, e.message.length - 1), 7, LIFECYCLE)
        }))
    }

    private fun evalTestResult(result: TestResult): String {
        return when (result.resultType!!) {
            TestResult.ResultType.SUCCESS -> ":success:".green(out)
            TestResult.ResultType.FAILURE -> ":failed:".red(out)
            TestResult.ResultType.SKIPPED -> ":sleep:".gray(out)
        }
    }

    private fun elapsed(result: TestResult): String {
        val elapsedMillis = result.endTime - result.startTime
        return TimeFormatter.formatMillis(elapsedMillis)
    }
}

package com.monnage.gradle.report

import com.monnage.gradle.extensions.TestReportExtension
import com.monnage.gradle.extensions.framework.FrameworkMode
import org.gradle.api.internal.tasks.testing.DecoratingTestDescriptor
import org.gradle.api.internal.tasks.testing.results.DefaultTestResult
import org.gradle.api.logging.LogLevel.*
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestOutputEvent
import org.gradle.api.tasks.testing.TestResult
import org.gradle.kotlin.dsl.KotlinClosure1
import org.gradle.kotlin.dsl.KotlinClosure2
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

internal object GlobalTestCounter {
    val successTestsCount = AtomicInteger(0)
    val failedTestsCount = AtomicInteger(0)
    val skippedTestsCount = AtomicInteger(0)
    val startTime = AtomicLong(0)
    val hookRegistered = AtomicBoolean(false)

    fun reset() {
        successTestsCount.set(0)
        failedTestsCount.set(0)
        skippedTestsCount.set(0)
        hookRegistered.set(false)
        startTime.set(0)
    }

    fun startTime() = startTime.set(System.currentTimeMillis())
}

internal class TestReport(private val test: Test, private val config: TestReportExtension) {

    private val out = FormattedOutput(config.logging, test.logger)
    private val logBuffer: MutableMap<String, MutableList<String>> = ConcurrentHashMap()

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
        if (!config.parallel) {
            beforeSuiteReport()
            beforeTestReport()
            afterTestReport()
            afterSuiteReport()
        } else {
            afterParallelTestReport()
        }

        val sync = GlobalTestCounter.hookRegistered.compareAndSet(false, true)
        if (sync) {
            GlobalTestCounter.startTime()
            out.display("")
            project.gradle.buildFinished {
                val stopTime = System.currentTimeMillis()
                val elapsed = TimeFormatter.formatMillis(stopTime - GlobalTestCounter.startTime.get())
                out.display(("\n[${sumTestCount()} total, ${GlobalTestCounter.successTestsCount.get()} succeeded, ${GlobalTestCounter.failedTestsCount.get()} failed, " +
                    "${GlobalTestCounter.skippedTestsCount.get()} skipped, ${elapsed}]\n").blue(out, italic = true), 3)
                GlobalTestCounter.hookRegistered.compareAndSet(true, false)
                GlobalTestCounter.reset()
            }
        }
    }

    private fun Test.beforeSuiteReport() {
        beforeSuite(KotlinClosure1<DecoratingTestDescriptor, Unit>({
            if (parent == null) {
                out.display(":rocket:  ${config.projectName.blue(out)}")
            }
            className?.let {
                val formattedClassName = "($it)".gray(out, italic = true)
                out.display("${classDisplayName.yellow(out)} $formattedClassName", 4)
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
            incrementTestCount(result)
            val name = with(desc) {
                if (displayName.endsWith("()")) displayName.substringBefore("()") else displayName
            }
            val testResult = evalTestResult(result)
            val elapsed = evalElapsed(result)
            out.display("$testResult $name $elapsed", 5)
            logFromBufferIfRequired(desc, result)
        }))
    }

    private fun Test.afterParallelTestReport() {
        afterTest(KotlinClosure2<DecoratingTestDescriptor, TestResult, Unit>({ desc, result ->
            incrementTestCount(result)
            val displayClassname = desc.classDisplayName
            val classname = desc.className?.let { "($it)" }?.gray(out, italic = true)
            val name = with(desc) {
                if (displayName.endsWith("()")) displayName.substringBefore("()") else displayName
            }
            val testResult = evalTestResult(result)
            val elapsed = evalElapsed(result)
            val level1 = "${config.projectName} >".blue(out)
            val level2 = "$displayClassname >".yellow(out)
            out.display("$testResult $level1 $level2 $name $elapsed " + (classname ?: ""), 0)
            logFromBufferIfRequired(desc, result)
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
        onOutput(KotlinClosure2<DecoratingTestDescriptor, TestOutputEvent, Unit>(event@{ d, e ->
            logEvent(d, e)
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
        onOutput(KotlinClosure2<DecoratingTestDescriptor, TestOutputEvent, Unit>(event@{ d, e ->
            if (e.message.length > 32) {
                val noise = e.message.substring(13, 32)
                if (noise.matches(noiseRegex)) {
                    return@event
                }
            }
            logEvent(d, e)
        }))
    }

    private fun evalTestResult(result: TestResult): String {
        return when (result.resultType!!) {
            TestResult.ResultType.SUCCESS -> ":success:".green(out)
            TestResult.ResultType.FAILURE -> ":failed: ".red(out)
            TestResult.ResultType.SKIPPED -> ":sleep:".gray(out)
        }
    }

    private fun incrementTestCount(result: TestResult) {
        when (result.resultType) {
            TestResult.ResultType.SUCCESS -> GlobalTestCounter.successTestsCount.incrementAndGet()
            TestResult.ResultType.FAILURE -> GlobalTestCounter.failedTestsCount.incrementAndGet()
            TestResult.ResultType.SKIPPED -> GlobalTestCounter.skippedTestsCount.incrementAndGet()
            null -> {
            }
        }
    }

    private fun sumTestCount(): Int {
        return GlobalTestCounter.successTestsCount.get() + GlobalTestCounter.failedTestsCount.get() + GlobalTestCounter.skippedTestsCount.get()
    }

    private fun evalElapsed(result: TestResult): String {
        return "[${elapsed(result)}]".let {
            if (elapsedMillis(result) > config.logging.slowThreshold) {
                return@let it.red(out, italic = true)
            } else {
                return@let it.gray(out, italic = true)
            }
        }
    }

    private fun logFromBufferIfRequired(desc: DecoratingTestDescriptor, result: TestResult) {
        val testKey = getTestId(desc)
        if (result.resultType == TestResult.ResultType.FAILURE && config.logging.showLogsFailedOnly) {
            if (desc.parent != null && !config.logging.showOnlyCauseException) {
                val id = desc.parent?.id.toString()
                printBuffer(id)
            }
            if (result is DefaultTestResult && config.logging.showOnlyCauseException) {
                val writer = StringWriter()
                result.exception?.printStackTrace(PrintWriter(writer))
                out.display(
                    writer.toString().red(out),
                    if (config.parallel) 2 else 7,
                    if (config.logging.showErrorsInQuietMode) ERROR else LIFECYCLE
                )
            }
            if (!config.logging.showOnlyCauseException) {
                printBuffer(testKey)
            }
        }
        logBuffer[testKey]?.clear()
    }

    private fun printBuffer(key: String) {
        logBuffer[key]?.forEach {
            out.display(
                it,
                if (config.parallel) 2 else 7,
                if (config.logging.showErrorsInQuietMode) ERROR else LIFECYCLE
            )
        }
    }

    private fun logEvent(d: DecoratingTestDescriptor, e: TestOutputEvent) {
        val log = e.message.substring(0, e.message.length - 1).gray(out)
        val testKey = getTestId(d)
        logBuffer.putIfAbsent(testKey, mutableListOf())
        logBuffer[testKey]?.add(log)
        if (!config.logging.showLogsFailedOnly) {
            out.display(log, 7, LIFECYCLE)
        }
    }

    private fun getTestId(desc: DecoratingTestDescriptor): String {
        return desc.id.toString()
    }

    private fun elapsed(result: TestResult): String {
        return TimeFormatter.formatMillis(elapsedMillis(result))
    }

    private fun elapsedMillis(result: TestResult): Long {
        return result.endTime - result.startTime
    }
}

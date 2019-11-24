plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "0.10.1"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
}

gradlePlugin {
    plugins {
        create("testReport") {
            id = "com.monnage.test-report"
            displayName = "Test Report"
            description = "This plugin pre-configures java test task which provides awesome test report output."
            implementationClass = "com.monnage.gradle.TestReportPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/monnage/gradle-test-report"
    vcsUrl = "https://github.com/monnage/gradle-test-report"
    tags = listOf("test", "java", "report", "pretty")
}
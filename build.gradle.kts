plugins {
    `kotlin-dsl`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.12.0"
    id("io.gitlab.arturbosch.detekt").version("1.1.1")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.1.1")
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

detekt {
    toolVersion = "1.1.1"
    config = files("$rootDir/.detekt-linter.yml")
    autoCorrect = project.findProperty("detekt.ac") != null
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "test-report"
            version = project.version.toString()
            from(components["java"])
        }
    }
}

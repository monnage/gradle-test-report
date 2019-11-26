package com.monnage.gradle.extensions.framework

class SpringBootExtension {

    internal var banner: Boolean = false

    fun banner() = run { banner = true }
}
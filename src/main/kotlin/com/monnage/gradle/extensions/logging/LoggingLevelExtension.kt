package com.monnage.gradle.extensions.logging

class LoggingLevelExtension {

    internal var root: String = "INFO"
    internal var loggers: MutableMap<String, String> = mutableMapOf()

    fun root(level: String) {
        root = level
    }

    fun logger(name: String, level: String) = loggers.putIfAbsent(name, level)
}
package com.monnage.gradle.actions

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import kotlin.reflect.KClass

interface PluginAction : Action<Project> {

    fun getPluginClass(): KClass<out Plugin<out Project>>?

    fun runtime(className: String): KClass<out Plugin<out Project>>? {
        return try {
            @Suppress("UNCHECKED_CAST")
            Class.forName(className).kotlin as KClass<out Plugin<out Project>>
        } catch (e: Throwable) {
            null
        }
    }
}

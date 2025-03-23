package com.timomcgrath.rtp.plugin

import org.bukkit.Location

interface PluginHookProvider {

    companion object {
        private var providers: List<PluginHookProvider> = listOf()

        fun init() {
            providers = PluginHooks.createAll()
        }

        fun dispatchAll(loc: Location) : Boolean {
            for (provider in providers) {
                if (!provider.isValidLocation(loc)) {
                    return false
                }
            }
            return true
        }
    }

    fun isValidLocation(loc: Location) : Boolean
}
package com.timomcgrath.rtp.plugin

import org.bukkit.Bukkit

enum class PluginHooks(val pluginName: String) {
    TOWNY("Towny");

    companion object {
        fun createAll() : List<PluginHookProvider> {
            val providers: MutableList<PluginHookProvider> = mutableListOf()
            val plugMan = Bukkit.getPluginManager()

            for (hook in entries) {
                if (!plugMan.isPluginEnabled(hook.pluginName)) {
                    continue
                }

                val provider = PluginHooks.create(hook)
                providers.add(provider)
            }

            return providers
        }

        fun create(hook: PluginHooks) : PluginHookProvider {
            when (hook) {
                TOWNY -> return Towny()
            }
        }
    }
}
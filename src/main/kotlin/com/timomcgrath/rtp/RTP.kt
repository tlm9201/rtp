package com.timomcgrath.rtp

import com.timomcgrath.rtp.plugin.PluginHookProvider
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

@Suppress("UnstableApiUsage")
class RTP : JavaPlugin() {
    companion object {
        lateinit var instance: RTP
    }

    override fun onEnable() {
        instance = this

        registerListeners()

        saveAndWriteConfig()

        Settings.load(config)
        PluginHookProvider.init()

        loadMessaging()

        this.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) {
            it.registrar().register(RTPCommand.get())
        }
    }

    override fun onDisable() {
        saveAndWriteConfig()
    }

    override fun reloadConfig() {
        super.reloadConfig()
        Settings.load(config)
    }

    private fun saveAndWriteConfig() {
        Settings.save(config)
        saveConfig()
    }

    private fun registerListeners() {
        val pluginManager = server.pluginManager
        pluginManager.registerEvents(RTPListener(), this)
    }

    private fun loadMessaging() {
        saveResource("messages.yml", false)
        val path = File(dataFolder, "messages.yml")
        val config = YamlConfiguration.loadConfiguration(path)
        Messaging.load(config)
    }
}

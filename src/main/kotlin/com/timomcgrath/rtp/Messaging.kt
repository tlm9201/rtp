package com.timomcgrath.rtp

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.TitlePart
import org.bukkit.configuration.Configuration
import org.bukkit.entity.Player

object Messaging {
    private val messagesMap = mutableMapOf<String, Component>()

    fun load(config: Configuration) {
        for (key in config.getKeys(false)) {
            messagesMap[key] = MiniMessage.miniMessage().deserialize(config.getString(key, key)!!)
        }
    }

    fun save(config: Configuration) {
        for ((key, value) in messagesMap) {
            config.set(key, MiniMessage.miniMessage().serialize(value))
        }
    }

    fun send(audience: Audience, key: String) {
        val message: Component = messagesMap[key] ?: Component.text(key)
        audience.sendMessage(message)
    }

    fun send(audience: Audience, key: String, vararg placeholders: Pair<String, String>) {
        var message: Component = messagesMap[key] ?: Component.text(key)
        for ((k, value) in placeholders) {
            message = message.replaceText(TextReplacementConfig.builder().match(k).replacement(Component.text(value)).build())
        }
        audience.sendMessage(message)
    }

    fun title(player: Player, key: String) {
        val message: Component = messagesMap[key] ?: Component.text(key)
        player.sendTitlePart(TitlePart.TITLE, message)
    }
}
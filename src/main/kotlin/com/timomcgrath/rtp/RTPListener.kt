package com.timomcgrath.rtp

import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent

class RTPListener : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (player.hasPlayedBefore()) {
            return
        }

        RTPHandler.rtpOnFirstJoin(player)
    }

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        RTPHandler.rtpOnRespawn(player)
    }

    @EventHandler
    fun onPlayerDamage(event: EntityDamageEvent) {
        if (event.entity.type != EntityType.PLAYER) {
            return
        }

        RTPHandler.cancelRTP(event.entity.uniqueId)
    }
}
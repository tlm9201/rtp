package com.timomcgrath.rtp

import org.bukkit.configuration.Configuration

object Settings {
    const val version: Int = 1

    var rtpOnFirstJoin: Boolean = false
    var rtpOnRespawn: Boolean = false
    var cancelOnMove: Boolean = true

    var corner1: IntArray = intArrayOf(0, 0)
    var corner2: IntArray = intArrayOf(0, 0)

    var cooldown: Int = 0
    var maxRolls: Int = 100

    var delay: Long = 5 // secs

    fun load(config: Configuration) {
        rtpOnFirstJoin = config.getBoolean("rtp-on-first-join", false)
        rtpOnRespawn = config.getBoolean("rtp-on-respawn", false)
        cancelOnMove = config.getBoolean("cancel-on-move", true)
        corner1 = intArrayOf(config.getInt("corner.a.x", 0), config.getInt("corner.a.z", 0))
        corner2 = intArrayOf(config.getInt("corner.b.x", 0), config.getInt("corner.b.z", 0))
        cooldown = config.getInt("cooldown", 0)
        maxRolls = config.getInt("max-rolls", 100)
        delay = config.getLong("delay", 5)
    }

    fun save(config: Configuration) {
        config.set("version", version)
        config.set("rtp-on-first-join", rtpOnFirstJoin)
        config.set("rtp-on-respawn", rtpOnRespawn)
        config.set("cancel-on-move", cancelOnMove)
        config.set("corner.a.x", corner1[0])
        config.set("corner.a.z", corner1[1])
        config.set("corner.b.x", corner2[0])
        config.set("corner.b.z", corner2[1])
        config.set("cooldown", cooldown)
        config.set("max-rolls", maxRolls)
        config.set("delay", delay)
    }
}
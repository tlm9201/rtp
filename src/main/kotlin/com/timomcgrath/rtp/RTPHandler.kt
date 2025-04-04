package com.timomcgrath.rtp

import com.palmergames.bukkit.towny.TownyAPI
import com.timomcgrath.rtp.plugin.PluginHookProvider
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.kyori.adventure.sound.Sound
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import java.util.UUID
import java.util.concurrent.TimeUnit

object RTPHandler {
    val cooldowns = mutableSetOf<UUID>()
    val rtping = mutableSetOf<UUID>()

    fun rtpNow(player: Player, useCooldown: Boolean = true) {
        if (useCooldown && cooldowns.contains(player.uniqueId)) {
            Messaging.send(player, "cooldown")
            return
        }

        for (i in 0 until Settings.maxRolls) {
            val location = getRandomLocation(player.world)
            if (PluginHookProvider.dispatchAll(location)) {
                player.teleportAsync(location).thenRun {
                    if (useCooldown) applyCooldown(player.uniqueId)
                    playTeleportEffects(player)
                }
                return
            }
        }

        Messaging.send(player, "no-valid-location")
    }

    fun rtp(player: Player) {
        if (rtping.contains(player.uniqueId)) {
            Messaging.send(player, "already-teleporting")
            return
        }

        rtping.add(player.uniqueId)

        var cancelOnMove: ScheduledTask? = null
        if (Settings.cancelOnMove) cancelOnMove = cancelOnMove(player.uniqueId, player.location)

        Bukkit.getAsyncScheduler().runDelayed(RTP.instance, {
            cancelOnMove?.cancel()
            if (!rtping.contains(player.uniqueId)) {
                return@runDelayed
            }

            rtping.remove(player.uniqueId)

            rtpNow(player)
        }, Settings.delay, TimeUnit.SECONDS)

        playPreRtpEffects(player)
    }

    fun getSafeLocation(world: World) : Location? {
        for (i in 0 until Settings.maxRolls) {
            val location = getRandomLocation(world)
            if (!isSafeBlock(location.block)) {
                continue
            }

            if (PluginHookProvider.dispatchAll(location.add(0.5, 1.0, 0.5))) {
                return location
            }
        }

        return null
    }

    fun isSafeBlock(block: Block) : Boolean {
        return block.isSolid
    }

    fun rtpOnFirstJoin(player: Player) {
        if (Settings.rtpOnFirstJoin) {
            rtp(player)
        }
    }


    fun rtpOnRespawn(player: Player) : Boolean {
        if (!Settings.rtpOnRespawn) {
            return false
        }

        if (Bukkit.getPluginManager().isPluginEnabled("Towny")) {
            return TownyAPI.getInstance().getTown(player) == null
        }

        return true
    }

    fun getRandomLocation(world: World) : Location {
        val corner1 = Settings.corner1
        val corner2 = Settings.corner2

        val x: Int?
        val z : Int?

        if (corner1[0] == corner2[0] && corner1[1] == corner2[1]) {
            // default to worldborder bounds
            val border = world.worldBorder
            val size = border.size / 2

            x = (border.center.x - size + Math.random() * border.size).toInt()
            z = (border.center.z - size + Math.random() * border.size).toInt()
        } else {
            x = (corner1[0] + Math.random() * (corner2[0] - corner1[0])).toInt()
            z = (corner1[1] + Math.random() * (corner2[1] - corner1[1])).toInt()
        }

        val chunk = world.getChunkAtAsync(x shr 4, z shr 4).thenApply { it.getChunkSnapshot(true, false, false) }.join()
        val y = chunk.getHighestBlockYAt(x and 0xF, z and 0xF)
        return Location(world, x.toDouble(), y.toDouble(), z.toDouble())
    }

    fun playTeleportEffects(player: Player) {
        player.playSound(Sound.sound(org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, Sound.Source.PLAYER, 1.0f, 1.0f))
        player.addPotionEffect(PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 20, 1))
        Messaging.title(player, "teleported")
    }

    fun playPreRtpEffects(player: Player) {
        player.playSound(Sound.sound(org.bukkit.Sound.ENTITY_CREEPER_PRIMED, Sound.Source.PLAYER, 1.0f, 1.0f))
        Messaging.send(player, "teleporting")
    }

    fun applyCooldown(uuid: UUID) {
        cooldowns.add(uuid)
        Bukkit.getAsyncScheduler().runDelayed(RTP.instance, {
            cooldowns.remove(uuid)
        }, Settings.cooldown.toLong(), TimeUnit.MINUTES)
    }

    fun cancelRTP(uuid: UUID) {
        rtping.remove(uuid)
    }

    fun cancelOnMove(uuid: UUID, origin: Location) : ScheduledTask {
        return Bukkit.getAsyncScheduler().runAtFixedRate(RTP.instance, {
            val player = Bukkit.getPlayer(uuid) ?: return@runAtFixedRate
            if (!rtping.contains(uuid)) {
                it.cancel()
                return@runAtFixedRate
            }

            if (player.location.distanceSquared(origin) > 1) {
                cancelRTP(uuid)
                Messaging.send(player, "cancelled")
                it.cancel()
            }
        }, 250, 250, TimeUnit.MILLISECONDS)
    }
}
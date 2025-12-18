package com.timomcgrath.rtp

import com.palmergames.bukkit.towny.TownyAPI
import com.timomcgrath.rtp.plugin.PluginHookProvider
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.keys.tags.BiomeTagKeys
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.kyori.adventure.sound.Sound
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object RTPHandler {
    val cooldowns = mutableSetOf<UUID>()
    val rtping = mutableSetOf<UUID>()
    val isOcean = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME)
        .getTag(BiomeTagKeys.create(NamespacedKey("minecraft", "is_ocean")))

    fun rtpNow(player: Player, useCooldown: Boolean = true) {
        if (useCooldown && player.gameMode != GameMode.CREATIVE && player.gameMode != GameMode.SPECTATOR && cooldowns.contains(
                player.uniqueId
            )
        ) {
            Messaging.send(player, "cooldown")
            return
        }

        player.scheduler.run(RTP.instance, {
            findValidLocation(player.world).thenAccept { loc ->
                if (loc != null) {
                    player.teleportAsync(loc).thenAccept {
                        if (useCooldown) applyCooldown(player.uniqueId)
                        player.scheduler.run(RTP.instance, {
                            playTeleportEffects(player)
                        }, null)
                    }
                } else {
                    player.scheduler.run(RTP.instance, {
                        Messaging.send(player, "no-valid-location")
                    }, null)
                }
            }
        }, null)
    }

    fun rtp(player: Player) {
        rtp(player, false)
    }

    fun rtp(player: Player, ignoreMove: Boolean) {
        if (rtping.contains(player.uniqueId)) {
            Messaging.send(player, "already-teleporting")
            return
        }

        rtping.add(player.uniqueId)

        var cancelOnMove: ScheduledTask? = null
        if (Settings.cancelOnMove && !ignoreMove) cancelOnMove = cancelOnMove(player.uniqueId, player.location)

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

    fun rtpOnFirstJoin(player: Player) {
        if (Settings.rtpOnFirstJoin) {
            rtp(player, true)
        }
    }


    fun rtpOnRespawn(player: Player): Boolean {
        if (!Settings.rtpOnRespawn) {
            return false
        }

        if (Bukkit.getPluginManager().isPluginEnabled("Towny")) {
            return TownyAPI.getInstance().getTown(player) == null
        }

        return true
    }

    private fun getRandomX(world: World): Int {
        if (Settings.corner1[0] == Settings.corner2[0] && Settings.corner1[1] == Settings.corner2[1]) {
            val border = world.worldBorder
            val size = border.size / 2
            return (border.center.x - size + Math.random() * border.size).toInt()
        } else {
            return (Settings.corner1[0] + Math.random() * (Settings.corner2[0] - Settings.corner1[0])).toInt()
        }
    }

    private fun getRandomZ(world: World): Int {
        if (Settings.corner1[0] == Settings.corner2[0] && Settings.corner1[1] == Settings.corner2[1]) {
            val border = world.worldBorder
            val size = border.size / 2
            return (border.center.z - size + Math.random() * border.size).toInt()
        } else {
            return (Settings.corner1[1] + Math.random() * (Settings.corner2[1] - Settings.corner1[1])).toInt()
        }
    }

    private fun findCandidateLocation(world: World, attempt: Int = 0): CompletableFuture<Location> {
        val minHeight = world.minHeight
        if (attempt >= Settings.maxRolls) {
            // fallback
            val x = getRandomX(world)
            val z = getRandomZ(world)
            return world.getChunkAtAsync(x shr 4, z shr 4).thenApply { chunk ->
                val snap = chunk.getChunkSnapshot(true, false, false)
                val localX = x and 0xF
                val localZ = z and 0xF
                val y = snap.getHighestBlockYAt(localX, localZ)
                var adjustedY = y + 1
                if (y < minHeight) adjustedY = minHeight + 1
                Location(world, x.toDouble(), adjustedY.toDouble(), z.toDouble())
            }
        }

        val x = getRandomX(world)
        val z = getRandomZ(world)
        return world.getChunkAtAsync(x shr 4, z shr 4).thenCompose { chunk ->
            val snap = chunk.getChunkSnapshot(true, true, false)
            val localX = x and 0xF
            val localZ = z and 0xF
            val y = snap.getHighestBlockYAt(localX, localZ)
            if (y < minHeight) {
                findCandidateLocation(world, attempt + 1)
            } else {
                val biomeKey = snap.getBiome(localX, y, localZ).key()
                if (!isOcean.contains(RegistryKey.BIOME.typedKey(biomeKey)) && snap.getBlockType(
                        localX,
                        y,
                        localZ
                    ).isSolid
                ) {
                    CompletableFuture.completedFuture(Location(world, x.toDouble(), (y + 1).toDouble(), z.toDouble()))
                } else {
                    findCandidateLocation(world, attempt + 1)
                }
            }
        }
    }

    private fun findValidLocation(world: World, attempt: Int = 0): CompletableFuture<Location?> {
        if (attempt >= Settings.maxRolls) {
            return CompletableFuture.completedFuture(null)
        }

        return findCandidateLocation(world).thenCompose { location ->
            if (PluginHookProvider.dispatchAll(location)) {
                CompletableFuture.completedFuture(location)
            } else {
                findValidLocation(world, attempt + 1)
            }
        }
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

    fun cancelOnMove(uuid: UUID, origin: Location): ScheduledTask {
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
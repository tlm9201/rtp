package com.timomcgrath.rtp

import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player

@Suppress("UnstableApiUsage")
object RTPCommand {

    fun get() : LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("rtp")
            .then(Commands.literal("reload")
                .requires { it.sender.hasPermission(Permissions.RELOAD.permission) }
                .executes {
                    try {
                        RTP.instance.reloadConfig()
                        Messaging.send(it.source.sender, "reload-success")
                    } catch (e: Exception) {
                        Messaging.send(it.source.sender, "reload-failed")
                        e.printStackTrace()
                    }
                    1
                })
            .then(Commands.argument("target", ArgumentTypes.player())
                .requires { it.sender.hasPermission(Permissions.OTHER.permission) }
                .executes {
                    val resolver = it.getArgument("target", PlayerSelectorArgumentResolver::class.java)
                    val target = resolver.resolve(it.source).first()
                    RTPHandler.rtp(target)
                    Messaging.send(it.source.sender, "rtp-other", Pair("target", target.name))
                    1
                }
            )
            .requires { it.sender.hasPermission(Permissions.SELF.permission) }
            .executes {
                val player = it.source.sender as? Player ?: return@executes 0
                val event = PreRTPEvent(player)
                Bukkit.getPluginManager().callEvent(event)
                if (!event.isCancelled || player.gameMode == GameMode.CREATIVE) {
                    RTPHandler.rtp(player)
                } else if (!event.cancelMessage.isEmpty()) {
                    player.sendMessage(event.cancelMessage)
                }
                1
            }
            .build()
    }
}
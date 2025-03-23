package com.timomcgrath.rtp.plugin

import com.palmergames.bukkit.towny.TownyAPI
import org.bukkit.Location

class Towny : PluginHookProvider {

    override fun isValidLocation(loc: Location) : Boolean {
        return TownyAPI.getInstance().isWilderness(loc)
    }
}
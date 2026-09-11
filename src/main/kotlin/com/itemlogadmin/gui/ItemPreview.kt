package com.itemlogadmin.gui

import com.google.gson.JsonParser
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object ItemPreview {
    fun fromJsonOrFallback(json: String?, fallbackMaterial: String?): ItemStack {
        if (json != null) {
            try {
                // Try to deserialize via ItemLog's ItemSerializer if available
                val clazz = Class.forName("com.itemlog.serialization.ItemSerializer")
                val serializer = clazz.getDeclaredConstructor().newInstance()
                val method = clazz.getMethod("deserialize", String::class.java)
                val item = method.invoke(serializer, json) as ItemStack?
                if (item != null && item.type != Material.AIR) return item
            } catch (_: Exception) {
            }
            // Fallback: try to parse material from json
            try {
                val obj = JsonParser.parseString(json).asJsonObject
                if (obj.has("type")) {
                    val mat = Material.getMaterial(obj.get("type").asString) ?: Material.PAPER
                    return ItemStack(mat)
                }
            } catch (_: Exception) {
            }
        }
        val mat = try {
            Material.valueOf(fallbackMaterial ?: "PAPER")
        } catch (_: Exception) {
            Material.PAPER
        }
        return ItemStack(mat)
    }
}

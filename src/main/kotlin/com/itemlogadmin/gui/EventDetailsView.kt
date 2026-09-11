package com.itemlogadmin.gui

import com.itemlogadmin.repository.ItemLogQueryRepository
import java.nio.ByteBuffer
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class EventDetailsView(
    private val queryRepo: ItemLogQueryRepository,
) {
    fun open(player: Player, eventId: UUID) {
        if (!player.hasPermission("itemlog.admin") && !player.hasPermission("itemlog.view")) {
            player.sendMessage("§c✗ No permission: §7itemlog.view")
            return
        }
        val inv = Bukkit.createInventory(null, 54, "§8Event §7» §e${eventId.toString().take(8)}")

        // Add decorative glass panes
        val glassPane = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val glassMeta = glassPane.itemMeta!!
        glassMeta.setDisplayName(" ")
        glassPane.itemMeta = glassMeta
        for (slot in listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 50, 51, 52, 53)) {
            inv.setItem(slot, glassPane)
        }
        Bukkit.getScheduler().runTaskAsynchronously(
            Bukkit.getPluginManager().getPlugin("ItemLogAdmin")!!,
            Runnable {
                var hasFailed = false
                var material: String? = null
                var type: String? = null
                var timestamp: Long? = null
                var world: String? = null
                var beforeJson: String? = null
                var afterJson: String? = null
                var restored = false
                try {
                    val field = queryRepo.javaClass.getDeclaredField("ds")
                    field.isAccessible = true
                    val ds = field.get(queryRepo) as javax.sql.DataSource
                    ds.connection.use { c ->
                        c.prepareStatement("SELECT event_type, timestamp, world, x, y, z, material, before_json, after_json FROM item_events WHERE event_id = ?").use { ps ->
                            ps.setBytes(1, uuidToBytes(eventId))
                            ps.executeQuery().use { rs ->
                                if (rs.next()) {
                                    type = rs.getString("event_type")
                                    timestamp = rs.getLong("timestamp")
                                    world = rs.getString("world")
                                    material = rs.getString("material")
                                    beforeJson = rs.getString("before_json")
                                    afterJson = rs.getString("after_json")
                                } else {
                                    hasFailed = true
                                }
                            }
                        }
                        if (!hasFailed) {
                            c.prepareStatement("SELECT 1 FROM restorations WHERE event_id = ? LIMIT 1").use { ps ->
                                ps.setBytes(1, uuidToBytes(eventId))
                                ps.executeQuery().use { rs -> restored = rs.next() }
                            }
                        }
                    }
                } catch (e: Exception) {
                    hasFailed = true
                    Bukkit.getLogger().warning("EventDetails fetch failed: ${e.message}")
                }
                Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugin("ItemLogAdmin")!!,
                    Runnable {
                        if (hasFailed) {
                            player.sendMessage("§c✗ Event not found or database error")
                            return@Runnable
                        }
                        val json = beforeJson ?: afterJson
                        val preview = ItemPreview.fromJsonOrFallback(json, material)
                        val pMeta = preview.itemMeta!!

                        val typeColor = when (type) {
                            "PICKUP" -> "§a"
                            "DROP" -> "§e"
                            "DEATH_DROP" -> "§c"
                            "CRAFT" -> "§b"
                            "SMELT" -> "§6"
                            else -> "§7"
                        }

                        pMeta.setDisplayName("$typeColor⚑ ${type ?: "UNKNOWN"}")
                        pMeta.setLore(
                            listOf(
                                "§8━━━━━━━━━━━━━━━━━━━━",
                                "§7📝 Event ID: §f${eventId.toString().take(16)}...",
                                "§7🕒 Time: §f${timestamp?.let { java.time.Instant.ofEpochMilli(it) } ?: "unknown"}",
                                "§7🌍 World: §f${world ?: "?"}",
                                "§7📦 Material: §f${material ?: "?"}",
                                "",
                                if (restored) "§c✗ Already Restored" else "§a✓ Available for Restore",
                                "§8━━━━━━━━━━━━━━━━━━━━",
                            ),
                        )
                        preview.itemMeta = pMeta
                        inv.setItem(13, preview)

                        if (restored) {
                            val disabled = ItemStack(Material.BARRIER)
                            disabled.itemMeta?.let {
                                it.setDisplayName("§c✗ Already Restored")
                                it.setLore(listOf("§7This event has already", "§7been restored previously", "", "§8Cannot restore again"))
                                disabled.itemMeta = it
                            }
                            inv.setItem(29, disabled)
                        } else {
                            val restore = ItemStack(Material.LIME_CONCRETE)
                            restore.itemMeta?.let {
                                it.setDisplayName("§a✓ Restore Item")
                                it.setLore(
                                    listOf(
                                        "§7Restore this item to",
                                        "§7the target player's inventory",
                                        "",
                                        "§eClick for confirmation",
                                    ),
                                )
                                restore.itemMeta = it
                            }
                            inv.setItem(29, restore)
                        }

                        val back = ItemStack(Material.ARROW)
                        back.itemMeta?.let {
                            it.setDisplayName("§7⬅ Back to List")
                            it.setLore(listOf("§8Return to events"))
                            back.itemMeta = it
                        }
                        inv.setItem(49, back)
                        player.openInventory(inv)
                    },
                )
            },
        )
    }

    private fun uuidToBytes(uuid: UUID): ByteArray {
        val bb = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        return bb.array()
    }
}

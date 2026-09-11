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
            player.sendMessage("§cNo permission: itemlog.view")
            return
        }
        val inv = Bukkit.createInventory(null, 54, "Event ${eventId.toString().take(8)}")
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
                            player.sendMessage("§cEvent not found or DB error")
                            return@Runnable
                        }
                        val json = beforeJson ?: afterJson
                        val preview = ItemPreview.fromJsonOrFallback(json, material)
                        val pMeta = preview.itemMeta!!
                        pMeta.setDisplayName("§e${type ?: "UNKNOWN"} §7${eventId.toString().take(8)}")
                        pMeta.setLore(
                            listOf(
                                "§7Time: ${timestamp?.let { java.time.Instant.ofEpochMilli(it) } ?: "unknown"}",
                                "§7World: ${world ?: "?"}",
                                "§7Material: ${material ?: "?"}",
                                "§7Restored: ${if (restored) "§cYes" else "§aNo"}",
                                "§7Click restore to confirm",
                            ),
                        )
                        preview.itemMeta = pMeta
                        inv.setItem(13, preview)

                        if (restored) {
                            val disabled = ItemStack(Material.BARRIER)
                            disabled.itemMeta?.let {
                                it.setDisplayName("§cAlready restored")
                                it.setLore(listOf("§7Already restored"))
                                disabled.itemMeta = it
                            }
                            inv.setItem(29, disabled)
                        } else {
                            val restore = ItemStack(Material.LIME_CONCRETE)
                            restore.itemMeta?.let {
                                it.setDisplayName("§aRestore")
                                it.setLore(listOf("§7Restore item to inventory", "§7Will ask for confirmation"))
                                restore.itemMeta = it
                            }
                            inv.setItem(29, restore)
                        }

                        val back = ItemStack(Material.ARROW)
                        back.itemMeta?.let {
                            it.setDisplayName("§7Back")
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

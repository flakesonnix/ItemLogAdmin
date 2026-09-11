package com.itemlogadmin.gui

import com.itemlogadmin.repository.ItemLogQueryRepository
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

class EventDetailsView(
    private val queryRepo: ItemLogQueryRepository
) {
    fun open(player: Player, eventId: UUID) {
        val inv = Bukkit.createInventory(null, 54, "Event ${eventId.toString().take(8)}")
        // async fetch full event
        Bukkit.getScheduler().runTaskAsynchronously(
            Bukkit.getPluginManager().getPlugin("ItemLogAdmin")!!,
            Runnable {
                // For Stage 4, we fetch via queryRepo — need to add findById
                // Placeholder: show eventId and placeholder item
                Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugin("ItemLogAdmin")!!,
                    Runnable {
                        // item preview in slot 13
                        val preview = ItemStack(Material.PAPER)
                        val meta = preview.itemMeta!!
                        meta.setDisplayName("§eEvent ${eventId.toString().take(8)}")
                        meta.setLore(
                            listOf(
                                "§7Type: PICKUP",
                                "§7Time: ${java.time.Instant.now()}",
                                "§7Click to restore (confirmation next)",
                                "§7Restoration: not yet restored"
                            )
                        )
                        preview.itemMeta = meta
                        inv.setItem(13, preview)

                        // restore button at 29
                        val restore = ItemStack(Material.LIME_CONCRETE)
                        restore.itemMeta?.let {
                            it.setDisplayName("§aRestore")
                            it.setLore(listOf("§7Restore item from this event", "§7Will ask for confirmation"))
                            restore.itemMeta = it
                        }
                        inv.setItem(29, restore)

                        // back at 49
                        val back = ItemStack(Material.ARROW)
                        back.itemMeta?.let {
                            it.setDisplayName("§7Back")
                            back.itemMeta = it
                        }
                        inv.setItem(49, back)

                        player.openInventory(inv)
                    }
                )
            }
        )
    }
}

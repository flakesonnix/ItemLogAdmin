package com.itemlogadmin.gui

import com.itemlogadmin.model.GuiState
import com.itemlogadmin.service.QueryService
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

class EventListView(
    private val queryService: QueryService
) {
    fun open(player: Player, targetId: UUID?, page: Int, filterType: String?, onOpen: (org.bukkit.inventory.Inventory) -> Unit) {
        val title = "Events — ${targetId?.toString()?.take(8) ?: "All"} p${page + 1}" + if (filterType != null) " [$filterType]" else ""
        val inv = Bukkit.createInventory(null, 54, title)
        // filter paper at slot 45
        val filterItem = ItemStack(Material.HOPPER)
        val fm = filterItem.itemMeta!!
        fm.setDisplayName("§bFilter: ${filterType ?: "ALL"}")
        fm.setLore(listOf("§7Click to cycle", "§7Types: PICKUP, DROP, DEATH_DROP, etc."))
        filterItem.itemMeta = fm
        inv.setItem(45, filterItem)

        // async query
        Bukkit.getScheduler().runTaskAsynchronously(
            Bukkit.getPluginManager().getPlugin("ItemLogAdmin")!!,
            Runnable {
                val (events, total) = queryService.getEvents(targetId, filterType, page, 45)
                Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugin("ItemLogAdmin")!!,
                    Runnable {
                        for ((i, ev) in events.withIndex()) {
                            val mat = try { Material.valueOf(ev.material ?: "STONE") } catch (_: Exception) { Material.PAPER }
                            val icon = ItemStack(mat)
                            val meta = icon.itemMeta!!
                            meta.setDisplayName("§e${ev.type} §7${ev.eventId.toString().take(8)}")
                            meta.setLore(
                                listOf(
                                    "§7${java.time.Instant.ofEpochMilli(ev.timestamp)}",
                                    "§7${ev.world} ${ev.x.toInt()},${ev.y.toInt()},${ev.z.toInt()}",
                                    "§7Material: ${ev.material}",
                                    "§7Click for details"
                                )
                            )
                            icon.itemMeta = meta
                            inv.setItem(i, icon)
                        }
                        // pagination
                        if (page > 0) {
                            val prev = ItemStack(Material.ARROW)
                            prev.itemMeta?.let { it.setDisplayName("§aPrev"); it.setLore(listOf("§7Page $page")); prev.itemMeta = it }
                            inv.setItem(48, prev)
                        }
                        if ((page + 1) * 45 < total) {
                            val next = ItemStack(Material.ARROW)
                            next.itemMeta?.let { it.setDisplayName("§aNext"); it.setLore(listOf("§7Page ${page + 2}")); next.itemMeta = it }
                            inv.setItem(50, next)
                        }
                        // time filter paper at 46
                        val timeItem = ItemStack(Material.CLOCK)
                        val tm = timeItem.itemMeta!!
                        tm.setDisplayName("§bTime filter")
                        tm.setLore(listOf("§7Not yet implemented", "§7Will filter by from/to"))
                        timeItem.itemMeta = tm
                        inv.setItem(46, timeItem)
                        onOpen(inv)
                    }
                )
            }
        )
    }
}

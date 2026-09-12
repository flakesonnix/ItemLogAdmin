package com.itemlogadmin.gui

import com.itemlogadmin.service.QueryService
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class EventListView(
    private val queryService: QueryService,
    private val cache: java.util.concurrent.ConcurrentHashMap<UUID, List<com.itemlogadmin.model.ItemEventView>>? = null,
) {
    fun open(
        player: Player,
        targetId: UUID?,
        page: Int,
        filterType: String?,
        materialFilter: String? = null,
        timeFilter: com.itemlogadmin.model.GuiState.TimeFilter? = null,
        onOpen: (org.bukkit.inventory.Inventory) -> Unit,
    ) {
        val targetName = targetId?.let { id ->
            Bukkit.getOfflinePlayer(id).name?.take(12) ?: id.toString().take(8)
        } ?: "All"

        val filters = buildList {
            if (filterType != null) add("§6$filterType")
            if (materialFilter != null) add("§e$materialFilter")
            if (timeFilter != null) add("§b${timeFilter.displayName}")
        }.joinToString(" ")

        val title = "§8Events §7» §e$targetName §8[§7p${page + 1}§8]${if (filters.isNotBlank()) " $filters" else ""}"
        val inv = Bukkit.createInventory(null, 54, title.take(32))

        // Add decorative glass panes
        val glassPane = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val glassMeta = glassPane.itemMeta!!
        glassMeta.setDisplayName(" ")
        glassPane.itemMeta = glassMeta
        for (slot in 45..53) {
            inv.setItem(slot, glassPane)
        }

        // Filter hopper at slot 45 (Event Type)
        val filterItem = ItemStack(Material.HOPPER)
        val fm = filterItem.itemMeta!!
        fm.setDisplayName("§b⚙ Event Type: §f${filterType ?: "ALL"}")
        fm.setLore(
            listOf(
                "§7Click to cycle through types",
                "",
                "§7Available filters:",
                "§8• §fALL",
                "§8• §ePICKUP",
                "§8• §cDROP",
                "§8• §4DEATH_DROP",
                "§8• §bCRAFT",
                "§8• §6SMELT",
                "",
                "§eClick to change filter",
            ),
        )
        filterItem.itemMeta = fm
        inv.setItem(45, filterItem)

        // Material filter at slot 47
        val matItem = ItemStack(Material.DIAMOND)
        val mm = matItem.itemMeta!!
        mm.setDisplayName("§b⚙ Material: §f${materialFilter ?: "ALL"}")
        mm.setLore(
            listOf(
                "§7Filter by item type",
                "§8Current: §f${materialFilter ?: "All materials"}",
                "",
                "§7Use command to filter:",
                "§f/itemlog material <type>",
                "",
                "§8Click to clear filter",
            ),
        )
        matItem.itemMeta = mm
        inv.setItem(47, matItem)

        // async query
        Bukkit.getScheduler().runTaskAsynchronously(
            Bukkit.getPluginManager().getPlugin("ItemLogAdmin")!!,
            Runnable {
                val fromTime = timeFilter?.let {
                    if (it.hours > 0) {
                        System.currentTimeMillis() - (it.hours * 60 * 60 * 1000)
                    } else {
                        null
                    }
                }
                val (events, total) = queryService.getEvents(targetId, filterType, page, 45, materialFilter, fromTime, null)
                if (cache != null) cache[player.uniqueId] = events
                Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugin("ItemLogAdmin")!!,
                    Runnable {
                        for ((i, ev) in events.withIndex()) {
                            val mat = try {
                                Material.valueOf(ev.material ?: "STONE")
                            } catch (_: Exception) {
                                Material.PAPER
                            }
                            val icon = ItemStack(mat)
                            val meta = icon.itemMeta!!

                            // Better color coding based on event type
                            val typeColor = when (ev.type) {
                                "PICKUP" -> "§a"
                                "DROP" -> "§e"
                                "DEATH_DROP" -> "§c"
                                "CRAFT" -> "§b"
                                "SMELT" -> "§6"
                                else -> "§7"
                            }

                            meta.setDisplayName("$typeColor${ev.type} §8[§7${ev.eventId.toString().take(8)}§8]")
                            meta.setLore(
                                listOf(
                                    "§7🕒 ${java.time.Instant.ofEpochMilli(ev.timestamp)}",
                                    "§7🌍 ${ev.world} §8(§f${ev.x.toInt()}§7,§f${ev.y.toInt()}§7,§f${ev.z.toInt()}§8)",
                                    "§7📦 Material: §f${ev.material}",
                                    if (ev.restored) "§c✗ Already restored" else "§a✓ Can be restored",
                                    "",
                                    "§e▶ Click for details",
                                ),
                            )
                            icon.itemMeta = meta
                            inv.setItem(i, icon)
                        }
                        // pagination
                        if (page > 0) {
                            val prev = ItemStack(Material.ARROW)
                            prev.itemMeta?.let {
                                it.setDisplayName("§a⬅ Previous Page")
                                it.setLore(listOf("§7Page §f$page", "", "§eClick to go back"))
                                prev.itemMeta = it
                            }
                            inv.setItem(48, prev)
                        }
                        if ((page + 1) * 45 < total) {
                            val next = ItemStack(Material.ARROW)
                            next.itemMeta?.let {
                                it.setDisplayName("§aNext Page ➡")
                                it.setLore(listOf("§7Page §f${page + 2}", "", "§eClick to continue"))
                                next.itemMeta = it
                            }
                            inv.setItem(50, next)
                        }

                        // Page info
                        val pageInfo = ItemStack(Material.BOOK)
                        pageInfo.itemMeta?.let {
                            it.setDisplayName("§e📖 Page ${page + 1}")
                            it.setLore(listOf("§7Total events: §f$total", "§7Showing: §f${events.size}", "§7Pages: §f${(total + 44) / 45}"))
                            pageInfo.itemMeta = it
                        }
                        inv.setItem(49, pageInfo)

                        // time filter clock at 46
                        val timeItem = ItemStack(Material.CLOCK)
                        val tm = timeItem.itemMeta!!
                        tm.setDisplayName("§b⏰ Time: §f${timeFilter?.displayName ?: "ALL"}")
                        tm.setLore(
                            listOf(
                                "§7Filter by time range",
                                "",
                                "§7Available:",
                                "§8• §fAll Time",
                                "§8• §eLast Hour",
                                "§8• §eLast 24h",
                                "§8• §eLast Week",
                                "§8• §eLast 30d",
                                "",
                                "§eClick to cycle",
                            ),
                        )
                        timeItem.itemMeta = tm
                        inv.setItem(46, timeItem)
                        onOpen(inv)
                    },
                )
            },
        )
    }
}

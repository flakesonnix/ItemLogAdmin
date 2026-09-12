package com.itemlogadmin.gui

import com.itemlogadmin.gui.EventDetailsView
import com.itemlogadmin.gui.EventListView
import com.itemlogadmin.gui.RestoreConfirmationView
import com.itemlogadmin.model.GuiState
import com.itemlogadmin.repository.PlayerRepository
import com.itemlogadmin.service.QueryService
import com.itemlogadmin.service.RestoreService
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.plugin.java.JavaPlugin

class GuiManager(
    private val plugin: JavaPlugin,
    private val queryService: QueryService,
    private val restoreService: RestoreService,
    private val playerRepo: PlayerRepository,
    private val queryRepo: com.itemlogadmin.repository.ItemLogQueryRepository,
) : Listener {

    private val state = ConcurrentHashMap<UUID, GuiState>()
    private val eventDetailsView = EventDetailsView(queryRepo)
    private val restoreConfirmView = RestoreConfirmationView(restoreService)
    private val eventCache = ConcurrentHashMap<UUID, List<com.itemlogadmin.model.ItemEventView>>()

    fun openPlayerList(player: Player, page: Int, query: String?) {
        state[player.uniqueId] = GuiState.PlayerList(page, query)
        val inv = Bukkit.createInventory(null, 54, "§8ItemLog §7» §ePlayers" + if (query != null) " §8[§6$query§8]" else "")
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val players = playerRepo.recentlyActive(45, page * 45, query)
                val total = playerRepo.countDistinct(query)
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        for ((i, info) in players.withIndex()) {
                            val offline = Bukkit.getOfflinePlayer(info.uuid)
                            val name = offline.name ?: info.uuid.toString().take(8)
                            if (!query.isNullOrBlank() && !name.lowercase().contains(query.lowercase()) && !info.uuid.toString().lowercase().startsWith(query.lowercase())) continue
                            val head = ItemStack(Material.PLAYER_HEAD)
                            val meta = head.itemMeta as SkullMeta
                            meta.setOwningPlayer(offline)
                            meta.setDisplayName("§6⚑ §e$name")
                            meta.setLore(
                                listOf(
                                    "§8${info.uuid.toString().take(16)}...",
                                    "",
                                    "§7📋 Events: §f${info.eventCount}",
                                    "§7🕒 Last seen: §f${java.time.Instant.ofEpochMilli(info.lastSeen)}",
                                    "",
                                    "§a▶ Click to view events",
                                ),
                            )
                            head.itemMeta = meta
                            inv.setItem(i, head)
                        }
                        // search name_tag at slot 45, prev/next arrows
                        val search = ItemStack(Material.NAME_TAG)
                        val sm = search.itemMeta!!
                        sm.setDisplayName("§b🔍 Search${if (query != null) ": §f$query" else ""}")
                        sm.setLore(listOf("§7Type §f/itemlog <name|uuid>", "§7to search for players", "", "§8Currently: §7${if (query != null) query else "No filter"}"))
                        search.itemMeta = sm
                        inv.setItem(45, search)
                        // Add glass panes as decoration
                        val glassPane = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
                        val glassMeta = glassPane.itemMeta!!
                        glassMeta.setDisplayName(" ")
                        glassPane.itemMeta = glassMeta
                        for (slot in 45..53) {
                            if (inv.getItem(slot) == null) {
                                inv.setItem(slot, glassPane)
                            }
                        }

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

                        // Add page indicator
                        val pageInfo = ItemStack(Material.BOOK)
                        pageInfo.itemMeta?.let {
                            it.setDisplayName("§e📖 Page Info")
                            it.setLore(listOf("§7Current: §f${page + 1}", "§7Total players: §f$total", "§7Pages: §f${(total + 44) / 45}"))
                            pageInfo.itemMeta = it
                        }
                        inv.setItem(49, pageInfo)

                        // Close button
                        val close = ItemStack(Material.BARRIER)
                        close.itemMeta?.let {
                            it.setDisplayName("§c✗ Close")
                            it.setLore(listOf("§7Close this menu"))
                            close.itemMeta = it
                        }
                        inv.setItem(53, close)
                        player.openInventory(inv)
                    },
                )
            },
        )
    }

    private val eventListView = EventListView(queryService, eventCache)

    fun openEventList(player: Player, targetId: UUID?, page: Int, filterType: String? = null, materialFilter: String? = null, timeFilter: GuiState.TimeFilter? = null) {
        state[player.uniqueId] = GuiState.EventList(targetId, page, filterType, materialFilter, timeFilter)
        eventListView.open(player, targetId, page, filterType, materialFilter, timeFilter) { inv ->
            player.openInventory(inv)
        }
    }

    @EventHandler
    fun onClick(e: InventoryClickEvent) {
        val player = e.whoClicked as? Player ?: return
        val s = state[player.uniqueId] ?: return
        e.isCancelled = true
        val slot = e.rawSlot
        if (s is GuiState.PlayerList) {
            val item = e.currentItem ?: return
            if (slot == 53) {
                // Close
                player.closeInventory()
                state.remove(player.uniqueId)
                return
            }
            if (slot == 45) {
                // TODO: open anvil GUI for search
                player.sendMessage("§7Type in chat: §b/itemlog <name|uuid prefix>")
                return
            }
            if (slot == 48 && s.page > 0) {
                openPlayerList(player, s.page - 1, s.query)
                return
            }
            if (slot == 50) {
                openPlayerList(player, s.page + 1, s.query)
                return
            }
            if (item.type == Material.PLAYER_HEAD) {
                val meta = item.itemMeta as? SkullMeta ?: return
                val offline = meta.owningPlayer ?: return
                openEventList(player, offline.uniqueId, 0)
            }
        } else if (s is GuiState.EventList) {
            val item = e.currentItem ?: return
            when (slot) {
                45 -> {
                    // cycle event type filter
                    val next = when (s.filter) {
                        null -> "PICKUP"
                        "PICKUP" -> "DROP"
                        "DROP" -> "DEATH_DROP"
                        "DEATH_DROP" -> "CRAFT_RESULT"
                        "CRAFT_RESULT" -> "SMELT_RESULT"
                        "SMELT_RESULT" -> null
                        else -> null
                    }
                    openEventList(player, s.playerId, 0, next, s.materialFilter, s.timeFilter)
                }
                46 -> {
                    // cycle time filter
                    val filters = GuiState.TimeFilter.values()
                    val currentIndex = s.timeFilter?.ordinal ?: -1
                    val nextFilter = if (currentIndex < filters.size - 1) filters[currentIndex + 1] else null
                    openEventList(player, s.playerId, 0, s.filter, s.materialFilter, nextFilter)
                }
                47 -> {
                    // clear material filter
                    openEventList(player, s.playerId, 0, s.filter, null, s.timeFilter)
                }
                48 -> if (s.page > 0) openEventList(player, s.playerId, s.page - 1, s.filter, s.materialFilter, s.timeFilter)
                50 -> openEventList(player, s.playerId, s.page + 1, s.filter, s.materialFilter, s.timeFilter)
                else -> {
                    if (item.type == Material.AIR) return
                    val events = eventCache[player.uniqueId] ?: return
                    val ev = events.getOrNull(slot) ?: return
                    // explicit state: store eventId
                    state[player.uniqueId] = GuiState.EventDetails(ev.eventId)
                    eventDetailsView.open(player, ev.eventId)
                }
            }
        } else if (s is GuiState.EventDetails) {
            val item = e.currentItem ?: return
            if (slot == 29) {
                // Restore -> confirmation
                val eventId = (s as? GuiState.EventDetails)?.eventId ?: return
                state[player.uniqueId] = GuiState.RestoreConfirm(eventId, player.uniqueId)
                restoreConfirmView.open(player, eventId, onConfirm = {
                    // will be handled via RestoreConfirm state click
                }, onCancel = {
                    openEventList(player, null, 0)
                })
            } else if (slot == 49) {
                openEventList(player, null, 0)
            }
        } else if (s is GuiState.RestoreConfirm) {
            val item = e.currentItem ?: return
            when (slot) {
                11 -> {
                    // Confirm
                    val res = restoreService.restore(s.eventId, player, player)
                    when (res) {
                        is RestoreService.Result.Success -> player.sendMessage("§a✓ Restored successfully §8[§7${res.restorationId.toString().take(8)}§8]")
                        is RestoreService.Result.AlreadyRestored -> player.sendMessage("§c✗ Already restored by §f${res.by}")
                        is RestoreService.Result.NotFound -> player.sendMessage("§c✗ Event not found")
                        is RestoreService.Result.Failed -> player.sendMessage("§c✗ Failed: §7${res.reason}")
                        is RestoreService.Result.NoPermission -> player.sendMessage("§c✗ No permission: §7${res.needed}")
                    }
                    player.closeInventory()
                    state.remove(player.uniqueId)
                }
                15 -> {
                    // Cancel -> back to details
                    state[player.uniqueId] = GuiState.EventDetails(s.eventId)
                    eventDetailsView.open(player, s.eventId)
                }
            }
        }
    }
}

package com.itemlogadmin.gui

import com.itemlogadmin.model.GuiState
import com.itemlogadmin.repository.PlayerRepository
import com.itemlogadmin.service.QueryService
import com.itemlogadmin.service.RestoreService
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class GuiManager(
    private val plugin: JavaPlugin,
    private val queryService: QueryService,
    private val restoreService: RestoreService,
    private val playerRepo: PlayerRepository
) : Listener {

    private val state = ConcurrentHashMap<UUID, GuiState>()

    fun openPlayerList(player: Player, page: Int, query: String?) {
        state[player.uniqueId] = GuiState.PlayerList(page, query)
        val inv = Bukkit.createInventory(null, 54, "ItemLog — Players" + if (query != null) " [$query]" else "")
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val players = playerRepo.recentlyActive(45, page * 45, query)
            val total = playerRepo.countDistinct(query)
            plugin.server.scheduler.runTask(plugin, Runnable {
                for ((i, info) in players.withIndex()) {
                    val offline = Bukkit.getOfflinePlayer(info.uuid)
                    val name = offline.name ?: info.uuid.toString().take(8)
                    if (!query.isNullOrBlank() && !name.lowercase().contains(query.lowercase()) && !info.uuid.toString().lowercase().startsWith(query.lowercase())) continue
                    val head = ItemStack(Material.PLAYER_HEAD)
                    val meta = head.itemMeta as SkullMeta
                    meta.setOwningPlayer(offline)
                    meta.setDisplayName("§e$name")
                    meta.setLore(listOf("§7${info.uuid}", "§7Events: ${info.eventCount}", "§7Last: ${java.time.Instant.ofEpochMilli(info.lastSeen)}", "§aClick to view"))
                    head.itemMeta = meta
                    inv.setItem(i, head)
                }
                // search paper at slot 45, next/prev at 53/45
                val search = ItemStack(Material.NAME_TAG)
                val sm = search.itemMeta!!
                sm.setDisplayName("§bSearch: ${query ?: ""}")
                sm.setLore(listOf("§7Anvil to search", "§7Query: name or UUID prefix"))
                search.itemMeta = sm
                inv.setItem(45, search)
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
                player.openInventory(inv)
            })
        })
    }

    fun openEventList(player: Player, targetId: UUID?, page: Int) {
        state[player.uniqueId] = GuiState.EventList(targetId, page, null)
        val inv = Bukkit.createInventory(null, 54, "Events — ${targetId?.toString()?.take(8) ?: "All"} p$page")
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val (events, total) = queryService.getEvents(targetId, null, page, 45)
            plugin.server.scheduler.runTask(plugin, Runnable {
                for ((i, ev) in events.withIndex()) {
                    val mat = try { Material.valueOf(ev.material ?: "STONE") } catch (_: Exception) { Material.PAPER }
                    val icon = ItemStack(mat)
                    val meta = icon.itemMeta!!
                    meta.setDisplayName("§e${ev.type} §7${ev.eventId.toString().take(8)}")
                    meta.setLore(listOf("§7${java.time.Instant.ofEpochMilli(ev.timestamp)}", "§7${ev.world} ${ev.x.toInt()},${ev.y.toInt()},${ev.z.toInt()}", "§7Click for details"))
                    icon.itemMeta = meta
                    inv.setItem(i, icon)
                }
                player.openInventory(inv)
            })
        })
    }

    @EventHandler
    fun onClick(e: InventoryClickEvent) {
        val player = e.whoClicked as? Player ?: return
        val s = state[player.uniqueId] ?: return
        e.isCancelled = true
        val slot = e.rawSlot
        if (s is GuiState.PlayerList) {
            val item = e.currentItem ?: return
            if (slot == 45) {
                // TODO: open anvil GUI for search
                player.sendMessage("§7Type in chat: /itemlog <name|uuid prefix>")
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
            // TODO: handle event click -> EventDetails
        }
    }
}

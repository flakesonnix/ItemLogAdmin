package com.itemlogadmin.gui

import com.itemlogadmin.model.GuiState
import com.itemlogadmin.service.QueryService
import com.itemlogadmin.service.RestoreService
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class GuiManager(
    private val plugin: JavaPlugin,
    private val queryService: QueryService,
    private val restoreService: RestoreService
) : Listener {

    private val state = ConcurrentHashMap<UUID, GuiState>()

    fun openPlayerList(player: Player, page: Int, query: String?) {
        state[player.uniqueId] = GuiState.PlayerList(page, query)
        val inv = Bukkit.createInventory(null, 54, "ItemLog — Players")
        // TODO: fill with player heads (recently active, search)
        // Placeholder: open empty
        player.openInventory(inv)
    }

    fun openEventList(player: Player, targetId: UUID?, page: Int) {
        state[player.uniqueId] = GuiState.EventList(targetId, page, null)
        val inv = Bukkit.createInventory(null, 54, "Events — ${targetId ?: "All"} p$page")
        // TODO: async query, pagination, filter
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val (events, total) = queryService.getEvents(targetId, null, page, 45)
            plugin.server.scheduler.runTask(plugin, Runnable {
                // TODO: fill inv with events (paper with material, lore with time/type)
                player.openInventory(inv)
            })
        })
    }

    @EventHandler
    fun onClick(e: InventoryClickEvent) {
        val player = e.whoClicked as? Player ?: return
        val s = state[player.uniqueId] ?: return
        e.isCancelled = true
        // TODO: map slot -> action via GuiState, never trust slot contents
        // Example: if (s is GuiState.PlayerList && e.slot == 0) openEventList(...)
    }
}

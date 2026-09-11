package com.itemlogadmin.gui

import com.itemlogadmin.service.RestoreService
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

class RestoreConfirmationView(
    private val restoreService: RestoreService
) {
    fun open(player: Player, eventId: UUID, onConfirm: () -> Unit, onCancel: () -> Unit) {
        val inv = Bukkit.createInventory(null, 27, "Restore? ${eventId.toString().take(8)}")
        // confirm at 11
        val confirm = ItemStack(Material.LIME_CONCRETE)
        confirm.itemMeta?.let {
            it.setDisplayName("§aConfirm Restore")
            it.setLore(listOf("§7Restore item to inventory", "§7Will be logged as ${player.name}", "§cIrreversible — check first!"))
            confirm.itemMeta = it
        }
        inv.setItem(11, confirm)
        // cancel at 15
        val cancel = ItemStack(Material.RED_CONCRETE)
        cancel.itemMeta?.let {
            it.setDisplayName("§cCancel")
            it.setLore(listOf("§7Back to details"))
            cancel.itemMeta = it
        }
        inv.setItem(15, cancel)
        // info at 13
        val info = ItemStack(Material.PAPER)
        info.itemMeta?.let {
            it.setDisplayName("§eEvent ${eventId.toString().take(8)}")
            it.setLore(listOf("§7You are about to restore", "§7This will be audited"))
            info.itemMeta = it
        }
        inv.setItem(13, info)
        player.openInventory(inv)
        // GuiManager will handle click via state RestoreConfirm
    }
}

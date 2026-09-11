package com.itemlogadmin.gui

import com.itemlogadmin.service.RestoreService
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class RestoreConfirmationView(
    private val restoreService: RestoreService,
) {
    fun open(player: Player, eventId: UUID, onConfirm: () -> Unit, onCancel: () -> Unit) {
        val inv = Bukkit.createInventory(null, 27, "§8Confirm §7» §e${eventId.toString().take(8)}")

        // Add decorative glass panes
        val glassPane = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val glassMeta = glassPane.itemMeta!!
        glassMeta.setDisplayName(" ")
        glassPane.itemMeta = glassMeta
        for (slot in listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26)) {
            inv.setItem(slot, glassPane)
        }

        // confirm at 11 - GREEN
        val confirm = ItemStack(Material.LIME_CONCRETE)
        confirm.itemMeta?.let {
            it.setDisplayName("§a✓ CONFIRM RESTORE")
            it.setLore(
                listOf(
                    "§7Restore item to inventory",
                    "",
                    "§7Performed by: §f${player.name}",
                    "§7Event: §f${eventId.toString().take(16)}...",
                    "",
                    "§c⚠ This action is logged",
                    "§c⚠ Cannot be undone",
                    "",
                    "§a▶ Click to confirm",
                ),
            )
            confirm.itemMeta = it
        }
        inv.setItem(11, confirm)

        // cancel at 15 - RED
        val cancel = ItemStack(Material.RED_CONCRETE)
        cancel.itemMeta?.let {
            it.setDisplayName("§c✗ CANCEL")
            it.setLore(listOf("§7Go back to event details", "", "§e▶ Click to cancel"))
            cancel.itemMeta = it
        }
        inv.setItem(15, cancel)

        // info at 13 - WARNING
        val info = ItemStack(Material.PAPER)
        info.itemMeta?.let {
            it.setDisplayName("§6⚠ Warning")
            it.setLore(
                listOf(
                    "§7You are about to restore",
                    "§7an item from the log.",
                    "",
                    "§7This action will:",
                    "§8• §fRestore the item",
                    "§8• §fLog your action",
                    "§8• §fBe permanent",
                    "",
                    "§cDouble-check before confirming!",
                ),
            )
            info.itemMeta = it
        }
        inv.setItem(13, info)

        player.openInventory(inv)
        // GuiManager will handle click via state RestoreConfirm
    }
}

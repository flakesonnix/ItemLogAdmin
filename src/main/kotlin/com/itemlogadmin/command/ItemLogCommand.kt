package com.itemlogadmin.command

import com.itemlogadmin.ItemLogAdminPlugin
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class ItemLogCommand(private val plugin: ItemLogAdminPlugin) :
    CommandExecutor,
    TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("itemlog.admin")) {
            sender.sendMessage("§cNo permission: itemlog.admin")
            return true
        }

        if (args.isEmpty()) {
            if (sender is Player) {
                plugin.guiManager.openPlayerList(sender, 0, null)
            } else {
                sendHelp(sender)
            }
            return true
        }

        when (args[0].lowercase()) {
            "help", "?" -> {
                sendHelp(sender)
            }
            "player", "p" -> {
                if (sender !is Player) {
                    sender.sendMessage("§cOnly players can use this command")
                    return true
                }
                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /itemlog player <name|uuid>")
                    return true
                }
                val target = findPlayer(args[1])
                if (target == null) {
                    sender.sendMessage("§cPlayer not found: ${args[1]}")
                    return true
                }
                plugin.guiManager.openEventList(sender, target, 0, null)
            }
            "search", "s" -> {
                if (sender !is Player) {
                    sender.sendMessage("§cOnly players can use this command")
                    return true
                }
                val query = args.drop(1).joinToString(" ")
                plugin.guiManager.openPlayerList(sender, 0, query.ifBlank { null })
            }
            "stats" -> {
                handleStats(sender, args)
            }
            "purge" -> {
                handlePurge(sender, args)
            }
            "export" -> {
                handleExport(sender, args)
            }
            "restore" -> {
                if (sender !is Player) {
                    sender.sendMessage("§cOnly players can use this command")
                    return true
                }
                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /itemlog restore <eventId>")
                    return true
                }
                handleRestore(sender, args[1])
            }
            else -> {
                // Try as player name search
                if (sender is Player) {
                    val query = args.joinToString(" ")
                    plugin.guiManager.openPlayerList(sender, 0, query)
                } else {
                    sendHelp(sender)
                }
            }
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (!sender.hasPermission("itemlog.admin")) return emptyList()

        return when (args.size) {
            1 -> listOf("help", "player", "search", "stats", "purge", "export", "restore").filter { it.startsWith(args[0].lowercase()) }
            2 -> when (args[0].lowercase()) {
                "player", "p" -> Bukkit.getOnlinePlayers().map { it.name }.filter { it.lowercase().startsWith(args[1].lowercase()) }
                "stats" -> Bukkit.getOnlinePlayers().map { it.name }.filter { it.lowercase().startsWith(args[1].lowercase()) }
                else -> null
            }
            else -> null
        }
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage(
            """
            §8§m                    §r §e§lItemLog Admin §8§m
            §7/itemlog §8- §fOpen player list GUI
            §7/itemlog player <name> §8- §fView player events
            §7/itemlog search <query> §8- §fSearch players
            §7/itemlog stats [player] §8- §fShow statistics
            §7/itemlog restore <eventId> §8- §fRestore an event
            §7/itemlog export [player] [days] §8- §fExport to CSV
            §7/itemlog purge <days> §8- §fDelete events older than X days
            §8§m
            """.trimIndent(),
        )
    }

    private fun findPlayer(nameOrUuid: String): UUID? {
        // Try UUID first
        return try {
            UUID.fromString(nameOrUuid)
        } catch (e: IllegalArgumentException) {
            // Try player name
            val online = Bukkit.getPlayerExact(nameOrUuid)
            if (online != null) return online.uniqueId

            // Try offline player
            @Suppress("DEPRECATION")
            val offline = Bukkit.getOfflinePlayer(nameOrUuid)
            if (offline.hasPlayedBefore()) offline.uniqueId else null
        }
    }

    private fun handleStats(sender: CommandSender, args: Array<out String>) {
        Bukkit.getScheduler().runTaskAsynchronously(
            plugin,
            Runnable {
                val targetId = if (args.size > 1) findPlayer(args[1]) else null
                val events = plugin.queryService.query(targetId, null, 0, 1)
                val total = plugin.queryService.count(targetId, null)

                Bukkit.getScheduler().runTask(
                    plugin,
                    Runnable {
                        sender.sendMessage(
                            """
                    §8§m                    §r §e§lItemLog Stats §8§m
                    ${if (targetId != null) "§7Player: §f${Bukkit.getOfflinePlayer(targetId).name}" else "§7Global Statistics"}
                    §7Total Events: §f$total
                    §7Tracked Players: §f${plugin.playerRepo.countDistinct(null)}
                    §8§m
                            """.trimIndent(),
                        )
                    },
                )
            },
        )
    }

    private fun handlePurge(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("itemlog.admin.purge")) {
            sender.sendMessage("§cNo permission: itemlog.admin.purge")
            return
        }

        if (args.size < 2) {
            sender.sendMessage("§cUsage: /itemlog purge <days>")
            sender.sendMessage("§7Example: /itemlog purge 30 §8(delete events older than 30 days)")
            return
        }

        val days = args[1].toIntOrNull()
        if (days == null || days < 1) {
            sender.sendMessage("§cInvalid number of days: ${args[1]}")
            return
        }

        sender.sendMessage("§e⚠ Purging events older than $days days...")

        Bukkit.getScheduler().runTaskAsynchronously(
            plugin,
            Runnable {
                val cutoff = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
                try {
                    plugin.dataSource.connection.use { c ->
                        c.prepareStatement("DELETE FROM item_events WHERE timestamp < ?").use { ps ->
                            ps.setLong(1, cutoff)
                            val deleted = ps.executeUpdate()

                            Bukkit.getScheduler().runTask(
                                plugin,
                                Runnable {
                                    sender.sendMessage("§a✓ Purged $deleted events older than $days days")
                                },
                            )
                        }
                    }
                } catch (e: Exception) {
                    Bukkit.getScheduler().runTask(
                        plugin,
                        Runnable {
                            sender.sendMessage("§c✗ Purge failed: ${e.message}")
                        },
                    )
                }
            },
        )
    }

    private fun handleRestore(sender: Player, eventIdStr: String) {
        val eventId = try {
            UUID.fromString(eventIdStr)
        } catch (e: IllegalArgumentException) {
            sender.sendMessage("§cInvalid event ID format")
            return
        }

        val result = plugin.restoreService.restore(eventId, sender, sender)
        when (result) {
            is com.itemlogadmin.service.RestoreService.Result.Success ->
                sender.sendMessage("§a✓ Restored successfully §8[§7${result.restorationId.toString().take(8)}§8]")
            is com.itemlogadmin.service.RestoreService.Result.AlreadyRestored ->
                sender.sendMessage("§c✗ Already restored by §f${result.by}")
            is com.itemlogadmin.service.RestoreService.Result.NotFound ->
                sender.sendMessage("§c✗ Event not found")
            is com.itemlogadmin.service.RestoreService.Result.Failed ->
                sender.sendMessage("§c✗ Failed: §7${result.reason}")
            is com.itemlogadmin.service.RestoreService.Result.NoPermission ->
                sender.sendMessage("§c✗ No permission: §7${result.needed}")
        }
    }

    private fun handleExport(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("itemlog.admin.export")) {
            sender.sendMessage("§cNo permission: itemlog.admin.export")
            return
        }

        val targetId = if (args.size > 1) findPlayer(args[1]) else null
        val days = if (args.size > 2) args[2].toIntOrNull() else null

        val fromTime = days?.let { System.currentTimeMillis() - (it * 24 * 60 * 60 * 1000L) }

        sender.sendMessage("§e⏳ Exporting events to CSV...")

        Bukkit.getScheduler().runTaskAsynchronously(
            plugin,
            Runnable {
                val file = plugin.exportService.exportToCsv(targetId, fromTime, null, 50000)

                Bukkit.getScheduler().runTask(
                    plugin,
                    Runnable {
                        if (file != null) {
                            sender.sendMessage("§a✓ Exported to: §f${file.name}")
                            sender.sendMessage("§7Location: §f${file.absolutePath}")
                        } else {
                            sender.sendMessage("§c✗ Export failed - check console")
                        }
                    },
                )
            },
        )
    }
}

package com.itemlogadmin

import com.itemlogadmin.db.DataSourceProvider
import com.itemlogadmin.gui.GuiManager
import com.itemlogadmin.repository.ItemLogQueryRepository
import com.itemlogadmin.repository.PlayerRepository
import com.itemlogadmin.service.QueryService
import com.itemlogadmin.service.RestoreService
import javax.sql.DataSource
import org.bukkit.plugin.java.JavaPlugin

class ItemLogAdminPlugin : JavaPlugin() {
    lateinit var dataSource: DataSource
    lateinit var queryService: QueryService
    lateinit var restoreService: RestoreService
    lateinit var guiManager: GuiManager
    lateinit var playerRepo: PlayerRepository

    override fun onEnable() {
        saveDefaultConfig()
        val provider = DataSourceProvider(this)
        dataSource = provider.getDataSource()
        val queryRepo = ItemLogQueryRepository(dataSource)
        playerRepo = PlayerRepository(dataSource)
        queryService = QueryService(queryRepo)
        restoreService = RestoreService(this, dataSource, queryRepo)
        guiManager = GuiManager(this, queryService, restoreService, playerRepo, queryRepo)

        getCommand("itemlog")?.setExecutor { sender, _, _, args ->
            if (!sender.hasPermission("itemlog.admin")) {
                sender.sendMessage("§cNo permission")
                return@setExecutor true
            }
            if (sender !is org.bukkit.entity.Player) {
                sender.sendMessage("§cOnly players")
                return@setExecutor true
            }
            guiManager.openPlayerList(sender, 0, args.firstOrNull())
            true
        }
        server.pluginManager.registerEvents(guiManager, this)
        logger.info("ItemLogAdmin ready — GUI admin for ItemLog DB")
    }
}

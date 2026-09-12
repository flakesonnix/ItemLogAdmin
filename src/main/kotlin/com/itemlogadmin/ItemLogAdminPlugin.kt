package com.itemlogadmin

import com.itemlogadmin.command.ItemLogCommand
import com.itemlogadmin.db.DataSourceProvider
import com.itemlogadmin.gui.GuiManager
import com.itemlogadmin.repository.ItemLogQueryRepository
import com.itemlogadmin.repository.PlayerRepository
import com.itemlogadmin.service.ExportService
import com.itemlogadmin.service.QueryService
import com.itemlogadmin.service.RestoreService
import javax.sql.DataSource
import org.bukkit.plugin.java.JavaPlugin

class ItemLogAdminPlugin : JavaPlugin() {
    lateinit var dataSource: DataSource
    lateinit var queryService: QueryService
    lateinit var restoreService: RestoreService
    lateinit var exportService: ExportService
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
        exportService = ExportService(this, queryRepo)
        guiManager = GuiManager(this, queryService, restoreService, playerRepo, queryRepo)

        // Register command with full command handler
        val commandHandler = ItemLogCommand(this)
        getCommand("itemlog")?.setExecutor(commandHandler)
        getCommand("itemlog")?.tabCompleter = commandHandler

        server.pluginManager.registerEvents(guiManager, this)
        logger.info("ItemLogAdmin ready — GUI admin + commands for ItemLog DB")
    }
}

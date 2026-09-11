package com.itemlogadmin.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import javax.sql.DataSource

class DataSourceProvider(private val plugin: JavaPlugin) {
    private var ds: HikariDataSource? = null

    fun getDataSource(): DataSource {
        if (ds != null && !ds!!.isClosed) return ds!!
        val type = plugin.config.getString("database.type") ?: "sqlite"
        val cfg = HikariConfig().apply {
            poolName = "ItemLogAdmin-Hikari"
            maximumPoolSize = plugin.config.getInt("database.pool.maximum-pool-size", 10)
            minimumIdle = plugin.config.getInt("database.pool.minimum-idle", 2)
        }

        when (type.lowercase()) {
            "mysql" -> {
                val host = plugin.config.getString("database.mysql.host", "localhost")!!
                val port = plugin.config.getInt("database.mysql.port", 3306)
                val db = plugin.config.getString("database.mysql.database", "itemlog")!!
                val user = plugin.config.getString("database.mysql.user", "root")!!
                val pass = plugin.config.getString("database.mysql.password", "")!!
                val params = plugin.config.getString("database.mysql.params", "useSSL=false&allowPublicKeyRetrieval=true")!!
                cfg.jdbcUrl = "jdbc:mysql://$host:$port/$db?$params"
                cfg.username = user
                cfg.password = pass
                cfg.driverClassName = "com.mysql.cj.jdbc.Driver"
            }
            else -> {
                val fileName = plugin.config.getString("database.sqlite.file", "database.db")!!
                val file = File(plugin.dataFolder, fileName)
                file.parentFile?.mkdirs()
                cfg.jdbcUrl = "jdbc:sqlite:${file.absolutePath}"
                cfg.driverClassName = "org.sqlite.JDBC"
                cfg.maximumPoolSize = 1
                cfg.minimumIdle = 1
                cfg.connectionTestQuery = "SELECT 1"
            }
        }

        cfg.addDataSourceProperty("cachePrepStmts", "true")
        ds = HikariDataSource(cfg)
        return ds!!
    }

    fun close() {
        ds?.let { if (!it.isClosed) it.close() }
    }
}
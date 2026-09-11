package com.itemlogadmin.db

import io.mockk.every
import io.mockk.mockk
import java.io.File
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DataSourceProviderTest {

    @TempDir lateinit var tempDir: File

    @Test
    fun `sqlite datasource`() {
        val plugin = mockk<JavaPlugin>(relaxed = true)
        val c = YamlConfiguration()
        c.set("database.type", "sqlite")
        c.set("database.sqlite.file", "admin_test.db")
        every { plugin.config } returns c
        every { plugin.dataFolder } returns tempDir
        every { plugin.logger } returns mockk(relaxed = true)
        val provider = DataSourceProvider(plugin)
        val ds = provider.getDataSource()
        assertNotNull(ds)
        ds.connection.use { conn ->
            conn.createStatement().use { st ->
                st.execute("SELECT 1")
            }
        }
        provider.close()
        assertTrue(File(tempDir, "admin_test.db").exists())
    }

    @Test
    fun `same instance`() {
        val plugin = mockk<JavaPlugin>(relaxed = true)
        val c = YamlConfiguration()
        c.set("database.type", "sqlite")
        c.set("database.sqlite.file", "admin2.db")
        every { plugin.config } returns c
        every { plugin.dataFolder } returns tempDir
        every { plugin.logger } returns mockk(relaxed = true)
        val provider = DataSourceProvider(plugin)
        val a = provider.getDataSource()
        val b = provider.getDataSource()
        assertTrue(a === b)
        provider.close()
    }
}

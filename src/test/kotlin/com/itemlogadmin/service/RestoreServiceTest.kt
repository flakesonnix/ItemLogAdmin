package com.itemlogadmin.service

import com.itemlogadmin.repository.ItemLogQueryRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import javax.sql.DataSource

class RestoreServiceTest {

    private lateinit var plugin: org.bukkit.plugin.java.JavaPlugin
    private lateinit var ds: DataSource
    private lateinit var queryRepo: ItemLogQueryRepository
    private lateinit var service: RestoreService

    @BeforeEach
    fun setup() {
        plugin = mockk(relaxed = true)
        ds = mockk(relaxed = true)
        queryRepo = mockk(relaxed = true)
        
        every { plugin.logger } returns mockk(relaxed = true)
        
        service = RestoreService(plugin, ds, queryRepo)
    }

    @Test
    fun `restore returns NoPermission when admin lacks permission`() {
        val admin = mockk<Player>(relaxed = true)
        every { admin.hasPermission("itemlog.admin") } returns false
        every { admin.hasPermission("itemlog.restore") } returns false
        
        val result = service.restore(UUID.randomUUID(), admin)
        
        assertTrue(result is RestoreService.Result.NoPermission)
        assertEquals("itemlog.restore", (result as RestoreService.Result.NoPermission).needed)
    }

    @Test
    fun `restore allows admin with itemlog-admin permission`() {
        val admin = mockk<Player>(relaxed = true)
        val eventId = UUID.randomUUID()
        
        every { admin.hasPermission("itemlog.admin") } returns true
        every { admin.uniqueId } returns UUID.randomUUID()
        
        // Mock database to return no existing restoration
        val connection = mockk<java.sql.Connection>(relaxed = true)
        val preparedStatement = mockk<java.sql.PreparedStatement>(relaxed = true)
        val resultSet = mockk<java.sql.ResultSet>(relaxed = true)
        
        every { ds.connection } returns connection
        every { connection.prepareStatement(any<String>()) } returns preparedStatement
        every { preparedStatement.executeQuery() } returns resultSet
        every { resultSet.next() } returns false
        every { resultSet.getString(any<String>()) } returns null
        
        val result = service.restore(eventId, admin)
        
        // Should not be NoPermission
        assertFalse(result is RestoreService.Result.NoPermission)
    }

    @Test
    fun `restore allows admin with itemlog-restore permission`() {
        val admin = mockk<Player>(relaxed = true)
        val eventId = UUID.randomUUID()
        
        every { admin.hasPermission("itemlog.admin") } returns false
        every { admin.hasPermission("itemlog.restore") } returns true
        every { admin.uniqueId } returns UUID.randomUUID()
        
        // Mock database to return no existing restoration
        val connection = mockk<java.sql.Connection>(relaxed = true)
        val preparedStatement = mockk<java.sql.PreparedStatement>(relaxed = true)
        val resultSet = mockk<java.sql.ResultSet>(relaxed = true)
        
        every { ds.connection } returns connection
        every { connection.prepareStatement(any<String>()) } returns preparedStatement
        every { preparedStatement.executeQuery() } returns resultSet
        every { resultSet.next() } returns false
        
        val result = service.restore(eventId, admin)
        
        assertFalse(result is RestoreService.Result.NoPermission)
    }
}

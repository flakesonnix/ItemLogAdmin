package com.itemlogadmin.service

import com.itemlogadmin.model.ItemEventView
import com.itemlogadmin.repository.ItemLogQueryRepository
import io.mockk.*
import java.io.File
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.Server
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExportServiceTest {

    private lateinit var plugin: JavaPlugin
    private lateinit var queryRepo: ItemLogQueryRepository
    private lateinit var service: ExportService
    private lateinit var testDir: File

    @BeforeEach
    fun setup() {
        plugin = mockk(relaxed = true)
        queryRepo = mockk(relaxed = true)

        testDir = File(System.getProperty("java.io.tmpdir"), "itemlog-test-${System.currentTimeMillis()}")
        testDir.mkdirs()

        every { plugin.dataFolder } returns testDir
        every { plugin.logger } returns mockk(relaxed = true)

        // Mock Bukkit static methods
        mockkStatic(Bukkit::class)
        val server = mockk<Server>(relaxed = true)
        every { Bukkit.getServer() } returns server
        every { Bukkit.getOfflinePlayer(any<UUID>()) } returns mockk<OfflinePlayer>(relaxed = true) {
            every { name } returns "TestPlayer"
        }

        service = ExportService(plugin, queryRepo)
    }

    @AfterEach
    fun cleanup() {
        testDir.deleteRecursively()
        unmockkStatic(Bukkit::class)
    }

    @Test
    fun `exportToCsv creates file with header`() {
        every { queryRepo.findEvents(any(), any(), any(), any(), any(), any(), any()) } returns emptyList()

        val file = service.exportToCsv(null, null, null, 100)

        assertNotNull(file)
        assertTrue(file!!.exists())
        assertTrue(file.readText().contains("EventID,Type,Timestamp,DateTime,PlayerID"))
    }

    @Test
    fun `exportToCsv writes event data`() {
        val playerId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val event = ItemEventView(
            eventId = eventId,
            type = "PICKUP",
            timestamp = 1700000000000L,
            playerId = playerId,
            playerName = "TestPlayer",
            world = "world",
            x = 100.5,
            y = 64.0,
            z = 200.7,
            material = "DIAMOND",
            amount = 5,
            source = "PLAYER_PICKUP",
            hasBefore = false,
            hasAfter = true,
            restored = false,
        )

        every { queryRepo.findEvents(any(), any(), any(), any(), any(), any(), any()) } returns listOf(event)

        val file = service.exportToCsv(playerId, null, null, 100)

        assertNotNull(file)
        val content = file!!.readText()
        assertTrue(content.contains(eventId.toString()))
        assertTrue(content.contains("PICKUP"))
        assertTrue(content.contains("DIAMOND"))
        assertTrue(content.contains("100,64,200"))
    }

    @Test
    fun `exportToCsv handles pagination correctly`() {
        val events = (1..2500).map {
            ItemEventView(
                eventId = UUID.randomUUID(),
                type = "PICKUP",
                timestamp = System.currentTimeMillis(),
                playerId = UUID.randomUUID(),
                playerName = "Player$it",
                world = "world",
                x = 0.0, y = 0.0, z = 0.0,
                material = "DIAMOND",
                amount = 1,
                source = "TEST",
                hasBefore = false,
                hasAfter = true,
                restored = false,
            )
        }

        // Return batches of 1000
        every { queryRepo.findEvents(any(), any(), any(), any(), any(), 1000, 0) } returns events.take(1000)
        every { queryRepo.findEvents(any(), any(), any(), any(), any(), 1000, 1000) } returns events.drop(1000).take(1000)
        every { queryRepo.findEvents(any(), any(), any(), any(), any(), 1000, 2000) } returns events.drop(2000).take(500)

        val file = service.exportToCsv(null, null, null, 10000)

        assertNotNull(file)
        val lines = file!!.readLines()
        assertEquals(2501, lines.size) // 2500 events + 1 header

        verify(exactly = 3) { queryRepo.findEvents(any(), any(), any(), any(), any(), 1000, any()) }
    }

    @Test
    fun `exportToCsv escapes CSV special characters`() {
        val event = ItemEventView(
            eventId = UUID.randomUUID(),
            type = "PICKUP",
            timestamp = System.currentTimeMillis(),
            playerId = UUID.randomUUID(),
            playerName = "Player,WithComma",
            world = "world",
            x = 0.0, y = 0.0, z = 0.0,
            material = "\"DIAMOND\"",
            amount = 1,
            source = "TEST,SOURCE",
            hasBefore = false,
            hasAfter = true,
            restored = false,
        )

        every { queryRepo.findEvents(any(), any(), any(), any(), any(), any(), any()) } returns listOf(event)

        val file = service.exportToCsv(null, null, null, 100)

        assertNotNull(file)
        val content = file!!.readText()
        assertTrue(content.contains("\"TEST,SOURCE\""))
        assertTrue(content.contains("\"\"\"DIAMOND\"\"\""))
    }

    @Test
    fun `exportToCsv filters by playerId`() {
        val playerId = UUID.randomUUID()

        every { queryRepo.findEvents(playerId, any(), any(), any(), any(), any(), any()) } returns emptyList()

        service.exportToCsv(playerId, null, null, 100)

        verify { queryRepo.findEvents(playerId, any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `exportToCsv filters by time range`() {
        val fromTime = 1700000000000L
        val toTime = 1700001000000L

        every { queryRepo.findEvents(any(), any(), any(), fromTime, toTime, any(), any()) } returns emptyList()

        service.exportToCsv(null, fromTime, toTime, 100)

        verify { queryRepo.findEvents(null, null, null, fromTime, toTime, any(), any()) }
    }

    @Test
    fun `exportToCsv returns null on error`() {
        every { queryRepo.findEvents(any(), any(), any(), any(), any(), any(), any()) } throws RuntimeException("DB error")

        val file = service.exportToCsv(null, null, null, 100)

        assertNull(file)
    }

    @Test
    fun `exportToCsv creates export directory if not exists`() {
        val exportDir = File(testDir, "exports")
        assertFalse(exportDir.exists())

        every { queryRepo.findEvents(any(), any(), any(), any(), any(), any(), any()) } returns emptyList()

        service.exportToCsv(null, null, null, 100)

        assertTrue(exportDir.exists())
        assertTrue(exportDir.isDirectory)
    }

    @Test
    fun `exportToCsv includes player name in filename`() {
        val playerId = UUID.randomUUID()
        every { queryRepo.findEvents(any(), any(), any(), any(), any(), any(), any()) } returns emptyList()
        every { Bukkit.getOfflinePlayer(playerId) } returns mockk {
            every { name } returns "Steve"
        }

        val file = service.exportToCsv(playerId, null, null, 100)

        assertNotNull(file)
        assertTrue(file!!.name.contains("Steve"))
    }

    @Test
    fun `exportToCsv respects limit parameter`() {
        val events = (1..100).map {
            ItemEventView(
                eventId = UUID.randomUUID(),
                type = "PICKUP",
                timestamp = System.currentTimeMillis(),
                playerId = null,
                playerName = null,
                world = "world",
                x = 0.0, y = 0.0, z = 0.0,
                material = "DIAMOND",
                amount = 1,
                source = "TEST",
                hasBefore = false,
                hasAfter = true,
                restored = false,
            )
        }

        every { queryRepo.findEvents(any(), any(), any(), any(), any(), 1000, 0) } returns events
        every { queryRepo.findEvents(any(), any(), any(), any(), any(), 1000, 1000) } returns emptyList()

        val file = service.exportToCsv(null, null, null, 50)

        assertNotNull(file)
        // Should stop after hitting limit even though there are more events
        verify(atMost = 1) { queryRepo.findEvents(any(), any(), any(), any(), any(), 1000, 0) }
    }

    @Test
    fun `getPlayerStats returns correct statistics`() {
        val playerId = UUID.randomUUID()

        every { queryRepo.countEvents(playerId, null, null, null, null) } returns 1000L
        every { queryRepo.countEvents(playerId, "PICKUP", null, null, null) } returns 300L
        every { queryRepo.countEvents(playerId, "DROP", null, null, null) } returns 200L
        every { queryRepo.countEvents(playerId, "DEATH_DROP", null, null, null) } returns 50L
        every { queryRepo.countEvents(playerId, "CRAFT_RESULT", null, null, null) } returns 150L

        val stats = service.getPlayerStats(playerId)

        assertEquals(1000L, stats.totalEvents)
        assertEquals(300L, stats.pickups)
        assertEquals(200L, stats.drops)
        assertEquals(50L, stats.deaths)
        assertEquals(150L, stats.crafts)
    }

    @Test
    fun `getPlayerStats works with null playerId for global stats`() {
        every { queryRepo.countEvents(null, null, null, null, null) } returns 5000L
        every { queryRepo.countEvents(null, "PICKUP", null, null, null) } returns 1500L
        every { queryRepo.countEvents(null, "DROP", null, null, null) } returns 1000L
        every { queryRepo.countEvents(null, "DEATH_DROP", null, null, null) } returns 500L
        every { queryRepo.countEvents(null, "CRAFT_RESULT", null, null, null) } returns 800L

        val stats = service.getPlayerStats(null)

        assertEquals(5000L, stats.totalEvents)
        assertEquals(1500L, stats.pickups)
    }
}

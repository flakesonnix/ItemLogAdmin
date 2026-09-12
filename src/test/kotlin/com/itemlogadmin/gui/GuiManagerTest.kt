package com.itemlogadmin.gui

import com.itemlogadmin.model.ItemEventView
import com.itemlogadmin.repository.ItemLogQueryRepository
import com.itemlogadmin.repository.PlayerRepository
import com.itemlogadmin.service.QueryService
import com.itemlogadmin.service.RestoreService
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

class GuiManagerTest {

    private lateinit var plugin: JavaPlugin
    private lateinit var queryService: QueryService
    private lateinit var restoreService: RestoreService
    private lateinit var playerRepo: PlayerRepository
    private lateinit var queryRepo: ItemLogQueryRepository
    private lateinit var guiManager: GuiManager

    @BeforeEach
    fun setup() {
        plugin = mockk(relaxed = true)
        queryService = mockk(relaxed = true)
        restoreService = mockk(relaxed = true)
        playerRepo = mockk(relaxed = true)
        queryRepo = mockk(relaxed = true)
        
        val server = mockk<org.bukkit.Server>(relaxed = true)
        val scheduler = mockk<org.bukkit.scheduler.BukkitScheduler>(relaxed = true)
        
        every { plugin.server } returns server
        every { server.scheduler } returns scheduler
        every { scheduler.runTaskAsynchronously(any<JavaPlugin>(), any<Runnable>()) } answers {
            val runnable = arg<Runnable>(1)
            runnable.run()
            mockk(relaxed = true)
        }
        every { scheduler.runTask(any<JavaPlugin>(), any<Runnable>()) } answers {
            val runnable = arg<Runnable>(1)
            runnable.run()
            mockk(relaxed = true)
        }
        
        mockkStatic(Bukkit::class)
        every { Bukkit.createInventory(any(), any<Int>(), any<String>()) } returns mockk(relaxed = true)
        every { Bukkit.getOfflinePlayer(any<UUID>()) } returns mockk(relaxed = true) {
            every { name } returns "TestPlayer"
        }
        
        guiManager = GuiManager(plugin, queryService, restoreService, playerRepo, queryRepo)
    }

    @AfterEach
    fun cleanup() {
        unmockkStatic(Bukkit::class)
    }

    @Test
    fun `openPlayerList creates inventory with correct title`() {
        val player = mockk<Player>(relaxed = true)
        every { player.uniqueId } returns UUID.randomUUID()
        every { player.openInventory(any()) } returns mockk(relaxed = true)
        
        every { playerRepo.recentlyActive(any(), any(), any()) } returns emptyList()
        every { playerRepo.countDistinct(any()) } returns 0L
        
        guiManager.openPlayerList(player, 0, null)
        
        verify { Bukkit.createInventory(null, 54, match { it.contains("ItemLog") && it.contains("Players") }) }
    }

    @Test
    fun `openPlayerList with query filters players`() {
        val player = mockk<Player>(relaxed = true)
        every { player.uniqueId } returns UUID.randomUUID()
        every { player.openInventory(any()) } returns mockk(relaxed = true)
        
        val query = "Steve"
        every { playerRepo.recentlyActive(any(), any(), query) } returns emptyList()
        every { playerRepo.countDistinct(query) } returns 1L
        
        guiManager.openPlayerList(player, 0, query)
        
        verify { playerRepo.recentlyActive(45, 0, query) }
        verify { Bukkit.createInventory(null, 54, match { it.contains(query) }) }
    }

    @Test
    fun `openPlayerList calculates correct offset for pagination`() {
        val player = mockk<Player>(relaxed = true)
        every { player.uniqueId } returns UUID.randomUUID()
        every { player.openInventory(any()) } returns mockk(relaxed = true)
        
        every { playerRepo.recentlyActive(any(), any(), any()) } returns emptyList()
        every { playerRepo.countDistinct(any()) } returns 0L
        
        guiManager.openPlayerList(player, 2, null)
        
        verify { playerRepo.recentlyActive(45, 90, null) } // page 2 * 45 = 90
    }

    @Test
    fun `openEventList stores correct state`() {
        val player = mockk<Player>(relaxed = true)
        val playerId = UUID.randomUUID()
        val targetId = UUID.randomUUID()
        
        every { player.uniqueId } returns playerId
        every { player.openInventory(any()) } returns mockk(relaxed = true)
        
        every { queryService.getEvents(any(), any(), any(), any(), any(), any(), any()) } returns (emptyList<ItemEventView>() to 0L)
        
        guiManager.openEventList(player, targetId, 0, null, null, null)
        
        // State should be stored
        val clickEvent = mockk<InventoryClickEvent>(relaxed = true)
        every { clickEvent.whoClicked } returns player
        every { clickEvent.isCancelled = any() } just Runs
        every { clickEvent.rawSlot } returns 53 // Close button
        every { clickEvent.currentItem } returns ItemStack(Material.BARRIER)
        
        // Trigger click to check state was stored
        guiManager.onClick(clickEvent)
        
        verify { player.closeInventory() }
    }

    @Test
    fun `onClick handles PlayerList close button`() {
        val player = mockk<Player>(relaxed = true)
        val playerId = UUID.randomUUID()
        every { player.uniqueId } returns playerId
        every { player.closeInventory() } just Runs
        every { player.openInventory(any()) } returns mockk(relaxed = true)
        
        every { playerRepo.recentlyActive(any(), any(), any()) } returns emptyList()
        every { playerRepo.countDistinct(any()) } returns 0L
        
        // Open player list to set state
        guiManager.openPlayerList(player, 0, null)
        
        val clickEvent = mockk<InventoryClickEvent>(relaxed = true)
        every { clickEvent.whoClicked } returns player
        every { clickEvent.isCancelled = any() } just Runs
        every { clickEvent.rawSlot } returns 53 // Close button
        every { clickEvent.currentItem } returns ItemStack(Material.BARRIER)
        
        guiManager.onClick(clickEvent)
        
        verify { player.closeInventory() }
    }

    @Test
    fun `onClick handles PlayerList pagination`() {
        val player = mockk<Player>(relaxed = true)
        val playerId = UUID.randomUUID()
        every { player.uniqueId } returns playerId
        every { player.openInventory(any()) } returns mockk(relaxed = true)
        
        every { playerRepo.recentlyActive(any(), any(), any()) } returns emptyList()
        every { playerRepo.countDistinct(any()) } returns 100L // More than one page
        
        // Open player list on page 1
        guiManager.openPlayerList(player, 1, null)
        
        val clickEvent = mockk<InventoryClickEvent>(relaxed = true)
        every { clickEvent.whoClicked } returns player
        every { clickEvent.isCancelled = any() } just Runs
        every { clickEvent.rawSlot } returns 48 // Previous button
        every { clickEvent.currentItem } returns ItemStack(Material.ARROW)
        
        guiManager.onClick(clickEvent)
        
        // Should open page 0
        verify(atLeast = 1) { playerRepo.recentlyActive(45, 0, null) }
    }

    @Test
    fun `onClick ignores clicks without state`() {
        val player = mockk<Player>(relaxed = true)
        every { player.uniqueId } returns UUID.randomUUID()
        
        val clickEvent = mockk<InventoryClickEvent>(relaxed = true)
        every { clickEvent.whoClicked } returns player
        every { clickEvent.isCancelled = any() } just Runs
        
        guiManager.onClick(clickEvent)
        
        // Should only set cancelled, no other actions
        verify { clickEvent.isCancelled = any() }
        verify(exactly = 0) { player.closeInventory() }
    }
}

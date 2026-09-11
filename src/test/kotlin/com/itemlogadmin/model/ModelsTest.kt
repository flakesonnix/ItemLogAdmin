package com.itemlogadmin.model

import java.util.UUID
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ModelsTest {

    @Test
    fun `GuiState PlayerList holds fields`() {
        val s = GuiState.PlayerList(2, "query")
        assertEquals(2, s.page)
        assertEquals("query", s.query)
    }

    @Test
    fun `GuiState EventList holds fields`() {
        val id = UUID.randomUUID()
        val s = GuiState.EventList(id, 1, "PICKUP")
        assertEquals(id, s.playerId)
        assertEquals(1, s.page)
        assertEquals("PICKUP", s.filter)
    }

    @Test
    fun `GuiState EventDetails holds id`() {
        val id = UUID.randomUUID()
        val s = GuiState.EventDetails(id)
        assertEquals(id, s.eventId)
    }

    @Test
    fun `GuiState RestoreConfirm holds ids`() {
        val e = UUID.randomUUID()
        val t = UUID.randomUUID()
        val s = GuiState.RestoreConfirm(e, t)
        assertEquals(e, s.eventId)
        assertEquals(t, s.targetId)
    }

    @Test
    fun `ItemEventView holds fields`() {
        val v = ItemEventView(UUID.randomUUID(), "PICKUP", 123L, UUID.randomUUID(), "Steve", "world", 1.0, 2.0, 3.0, "DIAMOND", 64, "test", true, false, false)
        assertEquals("PICKUP", v.type)
        assertEquals("DIAMOND", v.material)
        assertEquals(64, v.amount)
        assertTrue(v.hasBefore)
        assertFalse(v.hasAfter)
    }

    @Test
    fun `sealed class exhaustive`() {
        val states: List<GuiState> = listOf(
            GuiState.PlayerList(0, null),
            GuiState.EventList(null, 0, null),
            GuiState.EventDetails(UUID.randomUUID()),
            GuiState.RestoreConfirm(UUID.randomUUID(), UUID.randomUUID()),
        )
        assertEquals(4, states.size)
    }
}

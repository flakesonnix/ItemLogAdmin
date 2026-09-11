package com.itemlogadmin.service

import com.itemlogadmin.model.ItemEventView
import com.itemlogadmin.repository.ItemLogQueryRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class QueryServiceTest {

    @Test
    fun `getEvents delegates to repo`() {
        val repo = mockk<ItemLogQueryRepository>(relaxed = true)
        val view = ItemEventView(UUID.randomUUID(), "PICKUP", 0L, null, "A", "world", 0.0, 0.0, 0.0, "DIAMOND", 1, null, true, false, false)
        every { repo.findEvents(any(), any(), any(), any()) } returns listOf(view)
        every { repo.countEvents(any(), any()) } returns 1L
        val service = QueryService(repo)
        val (list, total) = service.getEvents(UUID.randomUUID(), "PICKUP", 0, 45)
        assertEquals(1, list.size)
        assertEquals(1L, total)
        verify { repo.findEvents(any(), "PICKUP", 45, 0) }
        verify { repo.countEvents(any(), "PICKUP") }
    }

    @Test
    fun `getEvents pagination offset`() {
        val repo = mockk<ItemLogQueryRepository>(relaxed = true)
        every { repo.findEvents(any(), any(), any(), any()) } returns emptyList()
        every { repo.countEvents(any(), any()) } returns 100L
        val service = QueryService(repo)
        val (list, total) = service.getEvents(null, null, 2, 45)
        assertEquals(0, list.size)
        assertEquals(100L, total)
        verify { repo.findEvents(null, null, 45, 90) } // page 2 * 45 = 90 offset
    }

    @Test
    fun `getEvents with null filters`() {
        val repo = mockk<ItemLogQueryRepository>(relaxed = true)
        every { repo.findEvents(null, null, any(), any()) } returns emptyList()
        every { repo.countEvents(null, null) } returns 0L
        val service = QueryService(repo)
        val (list, total) = service.getEvents(null, null, 0)
        assertEquals(0, list.size)
        assertEquals(0L, total)
    }
}

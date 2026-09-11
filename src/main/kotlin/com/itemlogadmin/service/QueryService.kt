package com.itemlogadmin.service

import com.itemlogadmin.model.ItemEventView
import com.itemlogadmin.repository.ItemLogQueryRepository
import java.util.UUID

class QueryService(private val repo: ItemLogQueryRepository) {
    fun getEvents(playerId: UUID?, type: String?, page: Int, size: Int = 45): Pair<List<ItemEventView>, Long> {
        val offset = page * size
        val events = repo.findEvents(playerId, type, size, offset)
        val total = repo.countEvents(playerId, type)
        return events to total
    }
}

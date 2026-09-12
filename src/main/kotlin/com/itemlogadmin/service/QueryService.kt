package com.itemlogadmin.service

import com.itemlogadmin.model.ItemEventView
import com.itemlogadmin.repository.ItemLogQueryRepository
import java.util.UUID

class QueryService(private val repo: ItemLogQueryRepository) {
    fun getEvents(
        playerId: UUID?,
        type: String?,
        page: Int,
        size: Int = 45,
        material: String? = null,
        fromTime: Long? = null,
        toTime: Long? = null,
    ): Pair<List<ItemEventView>, Long> {
        val offset = page * size
        val events = repo.findEvents(playerId, type, material, fromTime, toTime, size, offset)
        val total = repo.countEvents(playerId, type, material, fromTime, toTime)
        return events to total
    }

    fun query(playerId: UUID?, type: String?, page: Int, size: Int): List<ItemEventView> {
        val offset = page * size
        return repo.findEvents(playerId, type, null, null, null, size, offset)
    }

    fun count(playerId: UUID?, type: String?): Long = repo.countEvents(playerId, type, null, null, null)
}

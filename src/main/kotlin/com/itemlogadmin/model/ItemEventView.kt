package com.itemlogadmin.model

import java.util.UUID

data class ItemEventView(
    val eventId: UUID,
    val type: String,
    val timestamp: Long,
    val playerId: UUID?,
    val playerName: String?,
    val world: String?,
    val x: Double, val y: Double, val z: Double,
    val material: String?,
    val amount: Int?,
    val source: String?,
    val hasBefore: Boolean,
    val hasAfter: Boolean,
    val restored: Boolean
)

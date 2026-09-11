package com.itemlogadmin.repository

import com.itemlogadmin.model.ItemEventView
import java.nio.ByteBuffer
import java.util.UUID
import javax.sql.DataSource

class ItemLogQueryRepository(private val ds: DataSource) {

    fun findEvents(playerId: UUID?, type: String?, limit: Int, offset: Int): List<ItemEventView> {
        val sql = StringBuilder("SELECT event_id, event_type, timestamp, player_uuid, world, x, y, z, material, before_json, after_json, source FROM item_events WHERE 1=1")
        val params = mutableListOf<Any?>()
        if (playerId != null) {
            sql.append(" AND player_uuid = ?")
            params.add(uuidToBytes(playerId))
        }
        if (type != null) {
            sql.append(" AND event_type = ?")
            params.add(type)
        }
        sql.append(" ORDER BY timestamp DESC LIMIT ? OFFSET ?")
        params.add(limit)
        params.add(offset)
        ds.connection.use { c ->
            c.prepareStatement(sql.toString()).use { ps ->
                for ((i, p) in params.withIndex()) {
                    when (p) {
                        is ByteArray -> ps.setBytes(i + 1, p)
                        is String -> ps.setString(i + 1, p)
                        is Int -> ps.setInt(i + 1, p)
                    }
                }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ItemEventView>()
                    while (rs.next()) {
                        val eventId = bytesToUuid(rs.getBytes("event_id"))
                        val playerBytes = rs.getBytes("player_uuid")
                        list.add(
                            ItemEventView(
                                eventId = eventId,
                                type = rs.getString("event_type"),
                                timestamp = rs.getLong("timestamp"),
                                playerId = playerBytes?.let { bytesToUuid(it) },
                                playerName = null,
                                world = rs.getString("world"),
                                x = rs.getDouble("x"),
                                y = rs.getDouble("y"),
                                z = rs.getDouble("z"),
                                material = rs.getString("material"),
                                amount = null,
                                source = rs.getString("source"),
                                hasBefore = rs.getString("before_json") != null,
                                hasAfter = rs.getString("after_json") != null,
                                restored = false, // TODO: check restorations table
                            ),
                        )
                    }
                    return list
                }
            }
        }
    }

    fun countEvents(playerId: UUID?, type: String?): Long {
        val sql = StringBuilder("SELECT COUNT(*) FROM item_events WHERE 1=1")
        val params = mutableListOf<Any?>()
        if (playerId != null) {
            sql.append(" AND player_uuid = ?")
            params.add(uuidToBytes(playerId))
        }
        if (type != null) {
            sql.append(" AND event_type = ?")
            params.add(type)
        }
        ds.connection.use { c ->
            c.prepareStatement(sql.toString()).use { ps ->
                for ((i, p) in params.withIndex()) {
                    when (p) {
                        is ByteArray -> ps.setBytes(i + 1, p)
                        is String -> ps.setString(i + 1, p)
                    }
                }
                ps.executeQuery().use { rs -> if (rs.next()) return rs.getLong(1) }
            }
        }
        return 0
    }

    private fun uuidToBytes(uuid: UUID): ByteArray {
        val bb = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        return bb.array()
    }

    private fun bytesToUuid(bytes: ByteArray): UUID {
        val bb = ByteBuffer.wrap(bytes)
        return UUID(bb.long, bb.long)
    }
}

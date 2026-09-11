package com.itemlogadmin.repository

import com.itemlogadmin.model.ItemEventView
import java.nio.ByteBuffer
import java.util.UUID
import javax.sql.DataSource

class ItemLogQueryRepository(private val ds: DataSource) {

    fun findEvents(playerId: UUID?, type: String?, limit: Int, offset: Int): List<ItemEventView> {
        val sql = StringBuilder(
            """
            SELECT e.event_id, e.event_type, e.timestamp, e.player_uuid, e.world, e.x, e.y, e.z, e.material,
                   e.before_json, e.after_json, e.source,
                   CASE WHEN r.event_id IS NOT NULL THEN 1 ELSE 0 END as is_restored
            FROM item_events e
            LEFT JOIN restorations r ON e.event_id = r.event_id
            WHERE 1=1
            """.trimIndent(),
        )
        val params = mutableListOf<Any?>()
        if (playerId != null) {
            sql.append(" AND e.player_uuid = ?")
            params.add(uuidToBytes(playerId))
        }
        if (type != null) {
            sql.append(" AND e.event_type = ?")
            params.add(type)
        }
        sql.append(" ORDER BY e.timestamp DESC LIMIT ? OFFSET ?")
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
                                restored = rs.getInt("is_restored") == 1,
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

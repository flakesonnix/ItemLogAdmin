package com.itemlogadmin.repository

import java.nio.ByteBuffer
import java.util.UUID
import javax.sql.DataSource

data class PlayerInfo(
    val uuid: UUID,
    val name: String?,
    val lastSeen: Long,
    val eventCount: Long,
)

class PlayerRepository(private val ds: DataSource) {

    fun findByName(name: String): PlayerInfo? {
        // Try exact name from latest event, then fallback to UUID prefix
        ds.connection.use { c ->
            c.prepareStatement(
                """
                SELECT player_uuid, MAX(timestamp) as lastSeen, COUNT(*) as cnt
                FROM item_events WHERE player_uuid IS NOT NULL
                GROUP BY player_uuid HAVING MAX(CASE WHEN material = ? THEN 1 ELSE 0 END) = 1
                """.trimIndent(),
            ).use { _ -> }
        }
        // Simpler: search via Bukkit offline players, not DB, for Stage 2
        return null
    }

    fun recentlyActive(limit: Int = 45, offset: Int = 0, query: String? = null): List<PlayerInfo> {
        // For Stage 2, we query distinct player_uuid from item_events, ordered by lastSeen
        // query filters by UUID string prefix or by name via Bukkit lookup later
        val sql = StringBuilder(
            """
            SELECT player_uuid, MAX(timestamp) as lastSeen, COUNT(*) as cnt
            FROM item_events WHERE player_uuid IS NOT NULL
            """.trimIndent(),
        )
        val params = mutableListOf<Any?>()
        if (!query.isNullOrBlank()) {
            // query may be UUID prefix or name prefix — we filter in Kotlin after fetching
        }
        sql.append(" GROUP BY player_uuid ORDER BY lastSeen DESC LIMIT ? OFFSET ?")
        params.add(limit)
        params.add(offset)
        ds.connection.use { c ->
            c.prepareStatement(sql.toString()).use { ps ->
                for ((i, p) in params.withIndex()) {
                    when (p) {
                        is Int -> ps.setInt(i + 1, p)
                    }
                }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<PlayerInfo>()
                    while (rs.next()) {
                        val uuid = bytesToUuid(rs.getBytes("player_uuid"))
                        val lastSeen = rs.getLong("lastSeen")
                        val cnt = rs.getLong("cnt")
                        // name will be resolved via Bukkit.getOfflinePlayer
                        list.add(PlayerInfo(uuid, null, lastSeen, cnt))
                    }
                    return list
                }
            }
        }
    }

    fun countDistinct(query: String? = null): Long {
        ds.connection.use { c ->
            c.prepareStatement("SELECT COUNT(DISTINCT player_uuid) FROM item_events WHERE player_uuid IS NOT NULL").use { ps ->
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

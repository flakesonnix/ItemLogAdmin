package com.itemlogadmin.service

import com.itemlogadmin.model.ItemEventView
import com.itemlogadmin.repository.ItemLogQueryRepository
import java.io.File
import java.io.FileWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import org.bukkit.plugin.java.JavaPlugin

class ExportService(
    private val plugin: JavaPlugin,
    private val queryRepo: ItemLogQueryRepository,
) {

    private val dateFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    /**
     * Export events to CSV file
     * Returns the created file or null on error
     */
    fun exportToCsv(
        playerId: UUID?,
        fromTime: Long?,
        toTime: Long?,
        limit: Int = 10000,
    ): File? {
        try {
            val exportDir = File(plugin.dataFolder, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val timestamp = System.currentTimeMillis()
            val playerName = playerId?.let {
                org.bukkit.Bukkit.getOfflinePlayer(it).name ?: it.toString().take(8)
            } ?: "all"
            val filename = "itemlog_${playerName}_$timestamp.csv"
            val file = File(exportDir, filename)

            FileWriter(file).use { writer ->
                // CSV Header
                writer.append("EventID,Type,Timestamp,DateTime,PlayerID,PlayerName,World,X,Y,Z,Material,Source,HasBefore,HasAfter,Restored\n")

                // Fetch events in batches
                var offset = 0
                val batchSize = 1000

                while (offset < limit) {
                    val events = queryRepo.findEvents(
                        playerId,
                        null,
                        null,
                        fromTime,
                        toTime,
                        batchSize,
                        offset,
                    )

                    if (events.isEmpty()) break

                    for (event in events) {
                        writer.append(toCsvRow(event))
                    }

                    offset += batchSize

                    if (events.size < batchSize) break
                }
            }

            return file
        } catch (e: Exception) {
            plugin.logger.severe("CSV export failed: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    private fun toCsvRow(event: ItemEventView): String {
        val dateTime = dateFormatter.format(Instant.ofEpochMilli(event.timestamp))
        val playerName = event.playerId?.let {
            org.bukkit.Bukkit.getOfflinePlayer(it).name ?: "unknown"
        } ?: "N/A"

        return buildString {
            append(csvEscape(event.eventId.toString()))
            append(",")
            append(csvEscape(event.type))
            append(",")
            append(event.timestamp)
            append(",")
            append(csvEscape(dateTime))
            append(",")
            append(csvEscape(event.playerId?.toString() ?: ""))
            append(",")
            append(csvEscape(playerName))
            append(",")
            append(csvEscape(event.world ?: ""))
            append(",")
            append(event.x.toInt())
            append(",")
            append(event.y.toInt())
            append(",")
            append(event.z.toInt())
            append(",")
            append(csvEscape(event.material ?: ""))
            append(",")
            append(csvEscape(event.source ?: ""))
            append(",")
            append(if (event.hasBefore) "1" else "0")
            append(",")
            append(if (event.hasAfter) "1" else "0")
            append(",")
            append(if (event.restored) "1" else "0")
            append("\n")
        }
    }

    private fun csvEscape(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"${value.replace("\"", "\"\"")}\""
        }
        return value
    }

    /**
     * Get statistics for a player or global
     */
    data class PlayerStats(
        val totalEvents: Long,
        val pickups: Long,
        val drops: Long,
        val deaths: Long,
        val crafts: Long,
        val mostLostItem: String?,
        val mostGainedItem: String?,
    )

    fun getPlayerStats(playerId: UUID?): PlayerStats {
        val total = queryRepo.countEvents(playerId, null, null, null, null)
        val pickups = queryRepo.countEvents(playerId, "PICKUP", null, null, null)
        val drops = queryRepo.countEvents(playerId, "DROP", null, null, null)
        val deaths = queryRepo.countEvents(playerId, "DEATH_DROP", null, null, null)
        val crafts = queryRepo.countEvents(playerId, "CRAFT_RESULT", null, null, null)

        // Most lost/gained would need group by queries - leave null for now
        return PlayerStats(
            totalEvents = total,
            pickups = pickups,
            drops = drops,
            deaths = deaths,
            crafts = crafts,
            mostLostItem = null,
            mostGainedItem = null,
        )
    }
}

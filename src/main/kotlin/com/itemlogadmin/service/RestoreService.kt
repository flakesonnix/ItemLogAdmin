package com.itemlogadmin.service

import com.itemlogadmin.repository.ItemLogQueryRepository
import java.nio.ByteBuffer
import java.util.UUID
import javax.sql.DataSource
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class RestoreService(
    private val plugin: JavaPlugin,
    private val ds: DataSource,
    private val queryRepo: ItemLogQueryRepository,
) {
    sealed class Result {
        data class Success(val restorationId: UUID) : Result()
        data class AlreadyRestored(val by: String) : Result()
        data class NotFound(val eventId: UUID) : Result()
        data class Failed(val reason: String) : Result()
        data class NoPermission(val needed: String) : Result()
    }

    fun restore(eventId: UUID, admin: Player, target: Player? = null): Result {
        if (!admin.hasPermission("itemlog.admin") && !admin.hasPermission("itemlog.restore")) {
            return Result.NoPermission("itemlog.restore")
        }
        // check already restored
        ds.connection.use { c ->
            c.prepareStatement("SELECT admin_uuid FROM restorations WHERE event_id = ? LIMIT 1").use { ps ->
                ps.setBytes(1, uuidToBytes(eventId))
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        val adminBytes = rs.getBytes("admin_uuid")
                        val who = if (adminBytes != null) {
                            try {
                                Bukkit.getOfflinePlayer(bytesToUuid(adminBytes)).name ?: adminBytes.toString()
                            } catch (_: Exception) {
                                "unknown"
                            }
                        } else {
                            "unknown"
                        }
                        return Result.AlreadyRestored(who)
                    }
                }
            }
        }
        // fetch event
        val eventOpt = queryRepo.findEvents(null, null, null, null, null, 1, 0).find { it.eventId == eventId }
            ?: ds.connection.use { c ->
                c.prepareStatement("SELECT before_json, after_json, player_uuid FROM item_events WHERE event_id = ?").use { ps ->
                    ps.setBytes(1, uuidToBytes(eventId))
                    ps.executeQuery().use { rs ->
                        if (!rs.next()) return Result.NotFound(eventId)
                        // for restore, we need before or after json
                        null
                    }
                }
                null
            }
        // For Stage 5, we do a simple restore: fetch before_json/after_json and give to target
        var itemJson: String? = null
        var targetUuid: UUID? = null
        ds.connection.use { c ->
            c.prepareStatement("SELECT before_json, after_json, player_uuid FROM item_events WHERE event_id = ?").use { ps ->
                ps.setBytes(1, uuidToBytes(eventId))
                ps.executeQuery().use { rs ->
                    if (!rs.next()) return Result.NotFound(eventId)
                    val before = rs.getString("before_json")
                    val after = rs.getString("after_json")
                    itemJson = before ?: after
                    val playerBytes = rs.getBytes("player_uuid")
                    targetUuid = if (playerBytes != null) bytesToUuid(playerBytes) else null
                }
            }
        }
        if (itemJson == null) return Result.Failed("no snapshot")
        val actualTarget = target ?: targetUuid?.let { Bukkit.getPlayer(it) } ?: Bukkit.getPlayer(admin.uniqueId) ?: admin
        
        // Deserialize itemJson via ItemSerializer from ItemLog plugin
        val toGive = try {
            val itemLogPlugin = Bukkit.getPluginManager().getPlugin("ItemLog")
            if (itemLogPlugin != null) {
                val serializerClass = Class.forName("com.itemlog.serialization.ItemSerializer")
                val serializer = serializerClass.getDeclaredConstructor().newInstance()
                val method = serializerClass.getMethod("deserialize", String::class.java)
                method.invoke(serializer, itemJson) as ItemStack? ?: ItemStack(org.bukkit.Material.PAPER)
            } else {
                // Fallback if ItemLog not available
                ItemStack(org.bukkit.Material.PAPER).apply {
                    val meta = itemMeta!!
                    meta.setDisplayName("§eRestored Item ${eventId.toString().take(8)}")
                    meta.setLore(listOf("§7ItemLog plugin not loaded", "§7Cannot deserialize item data"))
                    itemMeta = meta
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to deserialize item ${eventId}: ${e.message}")
            ItemStack(org.bukkit.Material.PAPER).apply {
                val meta = itemMeta!!
                meta.setDisplayName("§cFailed to Restore ${eventId.toString().take(8)}")
                meta.setLore(listOf("§7Deserialization error", "§7${e.message}"))
                itemMeta = meta
            }
        }

        // Must run on main thread for inventory
        val given = try {
            val leftover = actualTarget.inventory.addItem(toGive)
            val success = leftover.isEmpty()
            if (!success) {
                // drop at location
                actualTarget.world.dropItemNaturally(actualTarget.location, toGive)
            }
            success
        } catch (e: Exception) {
            return Result.Failed(e.message ?: "inventory failed")
        }

        // audit log
        val restorationId = UUID.randomUUID()
        try {
            ds.connection.use { c ->
                c.prepareStatement(
                    """
                    INSERT INTO restorations
                    (restoration_id, event_id, admin_uuid, target_uuid, timestamp, world, x, y, z, yaw, pitch, result_json, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                ).use { ps ->
                    ps.setBytes(1, uuidToBytes(restorationId))
                    ps.setBytes(2, uuidToBytes(eventId))
                    ps.setBytes(3, uuidToBytes(admin.uniqueId))
                    ps.setBytes(4, uuidToBytes(actualTarget.uniqueId))
                    ps.setLong(5, System.currentTimeMillis())
                    ps.setString(6, actualTarget.world.name)
                    ps.setDouble(7, actualTarget.location.x)
                    ps.setDouble(8, actualTarget.location.y)
                    ps.setDouble(9, actualTarget.location.z)
                    ps.setFloat(10, actualTarget.location.yaw)
                    ps.setFloat(11, actualTarget.location.pitch)
                    ps.setString(12, itemJson)
                    ps.setString(13, if (given) "SUCCESS" else "PARTIAL")
                    ps.executeUpdate()
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to log restoration: ${e.message}")
            return Result.Failed("audit failed: ${e.message}")
        }
        return Result.Success(restorationId)
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

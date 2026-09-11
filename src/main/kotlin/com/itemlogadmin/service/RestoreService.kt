package com.itemlogadmin.service

import com.itemlogadmin.repository.ItemLogQueryRepository
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import javax.sql.DataSource

class RestoreService(
    private val plugin: JavaPlugin,
    private val ds: DataSource,
    private val queryRepo: ItemLogQueryRepository
) {
    fun restore(eventId: UUID, admin: Player, target: Player): Boolean {
        // TODO: implement restore via ItemLog's RestorationRepository (shared DB)
        // For now, just log
        plugin.logger.info("Restore requested: event=$eventId by ${admin.name} for ${target.name}")
        return true
    }
}

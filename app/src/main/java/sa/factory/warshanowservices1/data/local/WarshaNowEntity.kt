package sa.factory.warshanowservices1.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import sa.factory.warshanowservices1.model.WarshaNowItem

@Entity(tableName = "services_items_cache")
data class WarshaNowEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val priceSar: Double,
    val badge: String,
    val category: String,
    val status: String
) {
    fun toDomain(): WarshaNowItem = WarshaNowItem(
        id = id,
        title = title,
        description = description,
        priceSar = priceSar,
        badge = badge,
        category = category,
        status = status
    )
}

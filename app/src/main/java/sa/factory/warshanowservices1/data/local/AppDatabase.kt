package sa.factory.warshanowservices1.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WarshaNowEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun servicesDao(): WarshaNowDao
}

package com.aethernet.aethercontrol.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AccessEventEntity::class,
        SensorEventEntity::class,
        SecurityEventEntity::class,
        RoverTelemetryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accessEventDao(): AccessEventDao
    abstract fun sensorEventDao(): SensorEventDao
    abstract fun securityEventDao(): SecurityEventDao
    abstract fun roverTelemetryDao(): RoverTelemetryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aethernet-db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
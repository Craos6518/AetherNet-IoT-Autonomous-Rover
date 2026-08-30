package com.aethernet.aethercontrol

import android.app.Application
import androidx.room.Room
import com.aethernet.aethercontrol.data.local.AppDatabase

class AetherControlApplication : Application() {

    private var database: AppDatabase? = null
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "aethernet-db"
        ).fallbackToDestructiveMigration().build()
    }

    fun getDatabase(): AppDatabase {
        return database!!
    }

    companion object {
        @Suppress("UNUSED_PARAMETER")
        fun getInstance(application: Application): AetherControlApplication {
            return application as AetherControlApplication
        }
    }
}
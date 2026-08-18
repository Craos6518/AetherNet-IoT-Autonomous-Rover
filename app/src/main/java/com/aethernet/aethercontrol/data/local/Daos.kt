package com.aethernet.aethercontrol.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccessEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: AccessEventEntity): Long

    @Query("SELECT * FROM access_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<AccessEventEntity>>

    @Query("SELECT * FROM access_events WHERE userId = :userId ORDER BY timestamp DESC")
    fun getByUser(userId: String): Flow<List<AccessEventEntity>>
}

@Dao
interface SensorEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: SensorEventEntity): Long

    @Query("SELECT * FROM sensor_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<SensorEventEntity>>

    @Query("SELECT * FROM sensor_events WHERE sensorType = :type ORDER BY timestamp DESC LIMIT :limit")
    fun getByType(type: String, limit: Int): Flow<List<SensorEventEntity>>
}

@Dao
interface SecurityEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: SecurityEventEntity): Long

    @Query("SELECT * FROM security_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<SecurityEventEntity>>

    @Query("SELECT * FROM security_events WHERE eventType = :type ORDER BY timestamp DESC")
    fun getByType(type: String): Flow<List<SecurityEventEntity>>

    @Query("UPDATE security_events SET acknowledged = 1, acknowledgedAt = :ackAt, acknowledgedBy = :ackBy WHERE id = :id")
    suspend fun acknowledge(id: Long, ackAt: Long, ackBy: String): Int
}

@Dao
interface RoverTelemetryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: RoverTelemetryEntity): Long

    @Query("SELECT * FROM rover_telemetry WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getBySession(sessionId: String): Flow<List<RoverTelemetryEntity>>

    @Query("SELECT * FROM rover_telemetry ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<RoverTelemetryEntity>>
}
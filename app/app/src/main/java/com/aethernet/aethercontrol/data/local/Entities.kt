package com.aethernet.aethercontrol.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "access_events")
data class AccessEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val pinHash: String,
    val success: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "keypad"
)

@Entity(tableName = "sensor_events")
data class SensorEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sensorId: String,
    val sensorType: String,
    val value: Double,
    val filteredValue: Double?,
    val unit: String,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: String = "{}"
)

@Entity(tableName = "security_events")
data class SecurityEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,
    val severity: String = "medium",
    val description: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val acknowledged: Boolean = false,
    val acknowledgedAt: Long? = null,
    val acknowledgedBy: String? = null
)

@Entity(tableName = "rover_telemetry")
data class RoverTelemetryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val leftMotorPwm: Int,
    val rightMotorPwm: Int,
    val ultrasonicDistanceCm: Double?,
    val irLeft: Boolean?,
    val irCenter: Boolean?,
    val irRight: Boolean?,
    val rfRssi: Int?,
    val timestamp: Long = System.currentTimeMillis()
)
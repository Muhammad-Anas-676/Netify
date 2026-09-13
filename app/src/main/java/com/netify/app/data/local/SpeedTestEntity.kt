package com.netify.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speed_test_history")
data class SpeedTestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Double,
    val jitterMs: Double,
    val serverName: String,
    val timestamp: Long
)

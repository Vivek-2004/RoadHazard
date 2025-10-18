package com.drive.roadhazard.sensors

import android.util.Log
import com.drive.roadhazard.data.EventType
import com.drive.roadhazard.data.VehicleType
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class SensorProcessor {
    private val zAxisBuffer = mutableListOf<Float>()
    private val speedBuffer = mutableListOf<Float>()
    private var lastEventTime = 0L
    private val speedHistory = mutableListOf<Float>()

    companion object {
        private const val TAG = "SensorProcessor"
    }

    // Dynamic threshold calculation based on research
    fun calculateDynamicThreshold(speed: Float, vehicleType: VehicleType): Pair<Float, Float> {
        val baseThresholds = when (vehicleType) {
            VehicleType.TWO_WHEELER -> Pair(1.8f, 0.714f) // speed breaker, pothole
            VehicleType.THREE_WHEELER -> Pair(1.47f, 0.612f)
            VehicleType.FOUR_WHEELER -> Pair(1.08f, 0.41f)
        }

        speedHistory.add(speed)
        if (speedHistory.size > 10) { // Keep a window of the last 10 speed readings
            speedHistory.removeAt(0)
        }

        val movingAverageSpeed = speedHistory.average().toFloat()

        val L = 20f // Base speed from which to start adjusting the threshold
        val B = 20f // Base point
        val S = 0.05f // Scaling factor

        val dynamicSpeedBreakerThreshold = if (movingAverageSpeed > B) {
            baseThresholds.first + (movingAverageSpeed - L) * S
        } else {
            baseThresholds.first
        }

        val dynamicPotholeThreshold = if (movingAverageSpeed > B) {
            baseThresholds.second + (movingAverageSpeed - L) * S
        } else {
            baseThresholds.second
        }


        Log.d(
            TAG,
            "Dynamic Thresholds -> Speed: $speed km/h, Vehicle: $vehicleType, SB_Threshold: $dynamicSpeedBreakerThreshold, Pothole_Threshold: $dynamicPotholeThreshold"
        )

        return Pair(
            dynamicSpeedBreakerThreshold,
            dynamicPotholeThreshold
        )
    }

    // Auto-orientation based on research paper
    fun reorientAcceleration(ax: Float, ay: Float, az: Float): Triple<Float, Float, Float> {
        // Log raw sensor data
        Log.d(TAG, "Raw Accel -> X: $ax, Y: $ay, Z: $az")

        // Calculate Euler angles
        val pitch = atan2(ay, az)
        val roll = atan2(-ax, sqrt(ay * ay + az * az))

        // Reorient to vehicle reference frame
        val cosPitch = cos(pitch)
        val sinPitch = sin(pitch)
        val cosRoll = cos(roll)
        val sinRoll = sin(roll)

        val reorientedX = ax * cosRoll + ay * sinRoll * sinPitch + az * cosRoll * sinPitch
        val reorientedY = ay * cosPitch - az * sinPitch
        val reorientedZ = -ax * sinRoll + ay * cosRoll * sinPitch + az * cosRoll * cosPitch

        // Apply low-pass filter to the reoriented Z-axis value
        val alpha = 0.8f
        val filteredZ = reorientedZ * alpha


        // Log reoriented sensor data
        Log.d(TAG, "Reoriented Accel -> X: $reorientedX, Y: $reorientedY, Z: $filteredZ")

        return Triple(reorientedX, reorientedY, filteredZ)
    }

    fun detectEvent(
        zAccel: Float,
        speed: Float,
        vehicleType: VehicleType,
        timestamp: Long
    ): EventType? {
        // Prevent duplicate detections within 2 seconds
        if (timestamp - lastEventTime < 2000) return null

        val (speedBreakerThreshold, potholeThreshold) = calculateDynamicThreshold(
            speed,
            vehicleType
        )

        zAxisBuffer.add(zAccel)
        speedBuffer.add(speed)

        if (zAxisBuffer.size > 10) {
            zAxisBuffer.removeAt(0)
            speedBuffer.removeAt(0)
        }

        if (zAxisBuffer.size < 5) return null

        val maxZ = zAxisBuffer.maxOrNull() ?: 0f
        val minZ = zAxisBuffer.minOrNull() ?: 0f
        val avgSpeed = speedBuffer.average().toFloat()

        // Log buffer values for analysis
        Log.d(TAG, "Buffer Stats -> MaxZ: $maxZ, MinZ: $minZ, AvgSpeed: $avgSpeed")

        // Speed breaker detection (positive peak followed by negative)
        if (maxZ > speedBreakerThreshold && minZ < -speedBreakerThreshold * 0.5f) {
            Log.d(
                TAG,
                "EVENT DETECTED: SPEED_BREAKER -> Condition met: maxZ ($maxZ) > SB_Threshold ($speedBreakerThreshold) AND minZ ($minZ) < -SB_Threshold*0.5"
            )
            lastEventTime = timestamp
            zAxisBuffer.clear()
            speedBuffer.clear()
            return EventType.SPEED_BREAKER
        }

        // Pothole detection (negative peak)
        if (minZ < -potholeThreshold && avgSpeed > 10f) {
            Log.d(
                TAG,
                "EVENT DETECTED: POTHOLE -> Condition met: minZ ($minZ) < -Pothole_Threshold ($-potholeThreshold) AND avgSpeed ($avgSpeed) > 10"
            )
            lastEventTime = timestamp
            zAxisBuffer.clear()
            speedBuffer.clear()
            return EventType.POTHOLE
        }

        return null
    }
}
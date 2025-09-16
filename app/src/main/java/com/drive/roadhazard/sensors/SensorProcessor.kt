package com.drive.roadhazard.sensors

import com.drive.roadhazard.data.EventType
import com.drive.roadhazard.data.VehicleType
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class SensorProcessor {
    private val zAxisBuffer = mutableListOf<Float>()
    private val speedBuffer = mutableListOf<Float>()
    private var lastEventTime = 0L

    // Dynamic threshold calculation based on research
    fun calculateDynamicThreshold(speed: Float, vehicleType: VehicleType): Pair<Float, Float> {
        val baseThresholds = when (vehicleType) {
            VehicleType.TWO_WHEELER -> Pair(1.8f, 0.714f) // speed breaker, pothole
            VehicleType.THREE_WHEELER -> Pair(1.47f, 0.612f)
            VehicleType.FOUR_WHEELER -> Pair(1.08f, 0.41f)
            VehicleType.BUS -> Pair(1.08f, 0.41f)
        }

        val speedFactor = if (speed > 20f) {
            val scalingFactor = 0.05f
            1f + (speed - 20f) * scalingFactor / 20f
        } else 1f

        return Pair(
            baseThresholds.first * speedFactor,
            baseThresholds.second * speedFactor
        )
    }

    // Auto-orientation based on research paper
    fun reorientAcceleration(ax: Float, ay: Float, az: Float): Triple<Float, Float, Float> {
        // Apply low-pass filter
        val alpha = 0.8f
        val filteredAx = ax * alpha
        val filteredAy = ay * alpha
        val filteredAz = az * alpha

        // Calculate Euler angles
        val pitch = atan2(filteredAy, filteredAz)
        val roll = atan2(-filteredAx, sqrt(filteredAy * filteredAy + filteredAz * filteredAz))

        // Reorient to vehicle reference frame
        val cosPitch = cos(pitch)
        val sinPitch = sin(pitch)
        val cosRoll = cos(roll)
        val sinRoll = sin(roll)

        val reorientedX = ax * cosRoll + ay * sinRoll * sinPitch + az * cosRoll * sinPitch
        val reorientedY = ay * cosPitch - az * sinPitch
        val reorientedZ = -ax * sinRoll + ay * cosRoll * sinPitch + az * cosRoll * cosPitch

        return Triple(reorientedX, reorientedY, reorientedZ)
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

        // Speed breaker detection (positive peak followed by negative)
        if (maxZ > speedBreakerThreshold && minZ < -speedBreakerThreshold * 0.5f) {
            lastEventTime = timestamp
            return EventType.SPEED_BREAKER
        }

        // Pothole detection (negative peak)
        if (minZ < -potholeThreshold && avgSpeed > 10f) {
            lastEventTime = timestamp
            return EventType.POTHOLE
        }

        // Broken patch detection (sustained low acceleration with speed reduction)
        val speedVariation = speedBuffer.let { speeds ->
            if (speeds.size > 3) {
                speeds.takeLast(3).maxOrNull()!! - speeds.takeLast(3).minOrNull()!!
            } else 0f
        }

        if (speedVariation > 15f && avgSpeed < 20f && abs(zAccel) > 0.8f) {
            lastEventTime = timestamp
            return EventType.BROKEN_PATCH
        }

        return null
    }
}
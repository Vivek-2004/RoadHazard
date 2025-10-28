package com.drive.roadhazard.sensors

import com.drive.roadhazard.data.EventType
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class SensorProcessor {

    // --- RoADApp variables ---
    // From: royrivnam/roadapp/.../MainActivity.java (lines 103, 720-735)

    // ori[0] = pitch (a), ori[1] = roll (b)
    private val ori = DoubleArray(2)
    private var prev3: Long = 0 // Cooldown timestamp
    private var adjustment: Int = 0
    private var prev: Long = 0 // Timestamp for orientation recalibration

    // RoADApp constants from MainActivity.java (line 103)
    private val bump_thres = 10.6
    private val pot_thres = 4.0
    private val lowlimit = 5.0
    private val scaling = 5.0
    private val baselimit = 5.0
    // --- End RoADApp variables ---

    /**
     * This function replaces all previous detection logic.
     * It implements the full orientation, reorientation, and thresholding
     * logic from RoADApp's MainActivity.java (onSensorChanged method).
     */
    fun processNewSensorData(
        ax: Float,
        ay: Float,
        az: Float,
        speedInKmh: Float, // Speed from MainViewModel is already in km/h
        timestamp: Long
    ): EventType? {

        val acceleration = floatArrayOf(ax, ay, az)

        // 1. Calculate Orientation (from RoADApp onSensorChanged, lines 728-735)
        val sum =
            sqrt((acceleration[0] * acceleration[0]) + (acceleration[1] * acceleration[1]) + (acceleration[2] * acceleration[2])).toDouble()

        if ((timestamp - prev) > 850000) {
            adjustment = 0
            prev = timestamp
        }
        if (Math.abs(sum - 9.80) <= 0.4 && adjustment <= 10 && (timestamp - prev) <= 1000) {
            adjustment++
            // ori[0] = pitch (a)
            ori[0] = atan2(acceleration[1].toDouble(), acceleration[2].toDouble())
            // ori[1] = roll (b)
            ori[1] = atan2(
                -1 * acceleration[0].toDouble(),
                sqrt((acceleration[1] * acceleration[1]) + (acceleration[2] * acceleration[2])).toDouble()
            )
        }

        val cosa = cos(ori[0])
        val sina = sin(ori[0])
        val cosb = cos(ori[1])
        val sinb = sin(ori[1])

        // 2. Reorient Acceleration (from RoADApp onSensorChanged, lines 739-741)
        val acc_final = DoubleArray(3)
        // acc_final[0] = (cosb * acceleration[0]) + (sinb * sina * acceleration[1]) + (cosa * sinb * acceleration[2])
        // acc_final[1] = (cosa * acceleration[1]) - (sina * acceleration[2])
        acc_final[2] =
            (-1 * sinb * acceleration[0]) + (cosb * sina * acceleration[1]) + (cosb * cosa * acceleration[2])

        // 3. Calculate Dynamic Thresholds (from RoADApp onSensorChanged, lines 758-762)
        // RoADApp uses (speed * 3.6) because its `speed` is in m/s.
        // Our `speedInKmh` is already in km/h, so we just use it directly.
        val finb: Double
        val finp: Double
        if (speedInKmh < baselimit) {
            finb = bump_thres
            finp = -1 * pot_thres
        } else {
            // RoADApp scaling formula
            finb = bump_thres + ((speedInKmh - lowlimit) * (scaling / 10))
            finp = -1 * (pot_thres + ((speedInKmh - lowlimit) * (scaling / 10)))
        }

        // 4. Detect Event (from RoADApp onSensorChanged, line 763)
        // Check if Z-axis acceleration exceeds thresholds, with a 2-second cooldown
        if ((acc_final[2] > finb || acc_final[2] < finp) && (timestamp - prev3) > 2000) {
            prev3 = timestamp

            // RoADApp flags '1' for both. We'll differentiate them.
            return if (acc_final[2] > finb) {
                EventType.SPEED_BREAKER
            } else {
                EventType.POTHOLE
            }
        }

        // No event detected
        return null
    }
}
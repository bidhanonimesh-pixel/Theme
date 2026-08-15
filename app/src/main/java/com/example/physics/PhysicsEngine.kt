package com.example.physics

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.ui.geometry.Offset
import com.example.core.model.PhysicsMode
import com.example.launcher.model.AppItem
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class PhysicsBody(
    val id: String,
    val appItem: AppItem,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val radius: Float = 36f,
    val mass: Float = 1.0f,
    var rotationAngle: Float = 0f,
    var angularVelocity: Float = 0f,
    var isDragging: Boolean = false,
    var floatPhase: Float = Random.nextFloat() * 6.28f
)

class PhysicsEngine(
    private var gravityStrength: Float = 980f,
    private var restitution: Float = 0.75f
) : SensorEventListener {

    private var screenWidth: Float = 1080f
    private var screenHeight: Float = 1920f
    private var tiltX: Float = 0f
    private var tiltY: Float = 0f
    var currentMode: PhysicsMode = PhysicsMode.RAIN_GRAVITY

    fun updateScreenBounds(width: Float, height: Float) {
        screenWidth = width.coerceAtLeast(100f)
        screenHeight = height.coerceAtLeast(100f)
    }

    fun updateParameters(gravity: Float, elasticity: Float) {
        gravityStrength = gravity
        restitution = elasticity.coerceIn(0.1f, 0.95f)
    }

    fun initBodies(apps: List<AppItem>, width: Float, height: Float): List<PhysicsBody> {
        updateScreenBounds(width, height)
        val bodies = mutableListOf<PhysicsBody>()
        val cols = 4
        val colWidth = screenWidth / (cols + 1)

        apps.forEachIndexed { index, app ->
            val col = index % cols
            val row = index / cols
            val startX = (col + 1) * colWidth + (Random.nextFloat() - 0.5f) * 20f
            val startY = (row * 90f + 120f).coerceIn(60f, screenHeight - 200f)
            val body = PhysicsBody(
                id = app.packageName,
                appItem = app,
                x = startX,
                y = startY,
                vx = (Random.nextFloat() - 0.5f) * 150f,
                vy = if (currentMode == PhysicsMode.RAIN_GRAVITY) Random.nextFloat() * 200f else (Random.nextFloat() - 0.5f) * 50f,
                radius = 38f,
                rotationAngle = (Random.nextFloat() - 0.5f) * 20f,
                angularVelocity = (Random.nextFloat() - 0.5f) * 40f
            )
            bodies.add(body)
        }
        return bodies
    }

    fun stepSimulation(bodies: List<PhysicsBody>, dt: Float) {
        val clampedDt = dt.coerceIn(0.001f, 0.05f)

        for (i in bodies.indices) {
            val body = bodies[i]
            if (body.isDragging) continue

            when (currentMode) {
                PhysicsMode.RAIN_GRAVITY -> {
                    // Gravity force + Device tilt influence
                    val gx = tiltX * gravityStrength * 0.4f
                    val gy = gravityStrength + (tiltY * gravityStrength * 0.4f)

                    body.vx += gx * clampedDt
                    body.vy += gy * clampedDt

                    // Air friction / damping
                    body.vx *= 0.985f
                    body.vy *= 0.985f

                    // Position update
                    body.x += body.vx * clampedDt
                    body.y += body.vy * clampedDt

                    // Angular velocity decay
                    body.rotationAngle += body.angularVelocity * clampedDt
                    body.angularVelocity *= 0.96f
                }
                PhysicsMode.CLOUD_FLOATING -> {
                    // Anti-gravity floating with sinusoidal hovering + tilt drift
                    body.floatPhase += clampedDt * 2.0f
                    val hoverForceY = sin(body.floatPhase) * 60f
                    val hoverForceX = cos(body.floatPhase * 0.7f) * 40f

                    body.vx += (hoverForceX + tiltX * 300f) * clampedDt
                    body.vy += (hoverForceY + tiltY * 300f) * clampedDt

                    // Stronger damping for smooth cloud drift
                    body.vx *= 0.94f
                    body.vy *= 0.94f

                    body.x += body.vx * clampedDt
                    body.y += body.vy * clampedDt

                    body.rotationAngle += (body.vx * 0.05f)
                }
            }

            // Wall & Screen Boundary Collisions
            val minX = body.radius + 12f
            val maxX = screenWidth - body.radius - 12f
            val minY = body.radius + 50f
            val maxY = screenHeight - body.radius - 80f

            if (body.x < minX) {
                body.x = minX
                body.vx = -body.vx * restitution
                body.angularVelocity += body.vy * 0.2f
            } else if (body.x > maxX) {
                body.x = maxX
                body.vx = -body.vx * restitution
                body.angularVelocity -= body.vy * 0.2f
            }

            if (body.y < minY) {
                body.y = minY
                body.vy = -body.vy * restitution
            } else if (body.y > maxY) {
                body.y = maxY
                body.vy = -body.vy * restitution
                // Ground friction
                body.vx *= 0.85f
            }
        }

        // Circle-to-Circle Elastic Collisions
        for (i in 0 until bodies.size) {
            for (j in (i + 1) until bodies.size) {
                val b1 = bodies[i]
                val b2 = bodies[j]

                val dx = b2.x - b1.x
                val dy = b2.y - b1.y
                val distSq = dx * dx + dy * dy
                val minDist = b1.radius + b2.radius

                if (distSq < minDist * minDist && distSq > 0.0001f) {
                    val dist = sqrt(distSq)
                    val nx = dx / dist
                    val ny = dy / dist

                    // Positional separation to prevent sticking
                    val overlap = 0.5f * (minDist - dist)
                    if (!b1.isDragging) {
                        b1.x -= nx * overlap
                        b1.y -= ny * overlap
                    }
                    if (!b2.isDragging) {
                        b2.x += nx * overlap
                        b2.y += ny * overlap
                    }

                    // Relative velocity along collision normal
                    val kx = b1.vx - b2.vx
                    val ky = b1.vy - b2.vy
                    val p = 2f * (nx * kx + ny * ky) / (b1.mass + b2.mass)

                    if (!b1.isDragging) {
                        b1.vx -= p * b2.mass * nx * restitution
                        b1.vy -= p * b2.mass * ny * restitution
                    }
                    if (!b2.isDragging) {
                        b2.vx += p * b1.mass * nx * restitution
                        b2.vy += p * b1.mass * ny * restitution
                    }

                    // Angular spin impulse
                    val spin = (Random.nextFloat() - 0.5f) * 60f
                    b1.angularVelocity += spin
                    b2.angularVelocity -= spin
                }
            }
        }
    }

    fun explodeBurst(bodies: List<PhysicsBody>) {
        val centerX = screenWidth / 2f
        val centerY = screenHeight / 2f

        bodies.forEach { body ->
            val dx = body.x - centerX
            val dy = body.y - centerY
            val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            val blastForce = 1800f
            body.vx = (dx / dist) * blastForce + (Random.nextFloat() - 0.5f) * 400f
            body.vy = (dy / dist) * blastForce - 500f
            body.angularVelocity = (Random.nextFloat() - 0.5f) * 200f
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            // Smooth low-pass filter
            tiltX = -event.values[0] * 0.3f
            tiltY = event.values[1] * 0.3f
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

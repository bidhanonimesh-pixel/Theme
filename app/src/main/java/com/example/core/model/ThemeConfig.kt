package com.example.core.model

import androidx.compose.ui.graphics.Color

enum class CyberColorPalette(
    val title: String,
    val primaryCyan: Color,
    val secondaryAccent: Color,
    val backgroundDark: Color,
    val surfaceDark: Color,
    val glowColor: Color
) {
    NEON_CYAN(
        title = "Mark 50 Cyan",
        primaryCyan = Color(0xFF00E5FF),
        secondaryAccent = Color(0xFFFF6D00),
        backgroundDark = Color(0xFF040914),
        surfaceDark = Color(0xFF0A1626),
        glowColor = Color(0xFF00E5FF)
    ),
    ARC_ORANGE(
        title = "Arc Reactor Amber",
        primaryCyan = Color(0xFFFF6D00),
        secondaryAccent = Color(0xFF00E5FF),
        backgroundDark = Color(0xFF0D0804),
        surfaceDark = Color(0xFF1E1308),
        glowColor = Color(0xFFFF9100)
    ),
    STEALTH_EMERALD(
        title = "Matrix Emerald",
        primaryCyan = Color(0xFF00E676),
        secondaryAccent = Color(0xFF00B0FF),
        backgroundDark = Color(0xFF020E08),
        surfaceDark = Color(0xFF071E12),
        glowColor = Color(0xFF00E676)
    ),
    CRIMSON_WAR(
        title = "Hulkbuster Crimson",
        primaryCyan = Color(0xFFFF1744),
        secondaryAccent = Color(0xFFFFD600),
        backgroundDark = Color(0xFF120306),
        surfaceDark = Color(0xFF22080D),
        glowColor = Color(0xFFFF1744)
    ),
    QUANTUM_PURPLE(
        title = "Quantum Violet",
        primaryCyan = Color(0xFFD500F9),
        secondaryAccent = Color(0xFF00E5FF),
        backgroundDark = Color(0xFF0A0414),
        surfaceDark = Color(0xFF160A29),
        glowColor = Color(0xFFE040FB)
    )
}

enum class PhysicsMode(val title: String) {
    RAIN_GRAVITY("Rain Gravity (Bounce & Fall)"),
    CLOUD_FLOATING("Cloud Floating (Anti-Gravity Gyro)")
}

data class LauncherSettings(
    val palette: CyberColorPalette = CyberColorPalette.NEON_CYAN,
    val physicsMode: PhysicsMode = PhysicsMode.RAIN_GRAVITY,
    val glowIntensity: Float = 0.85f,
    val arcRotationSpeedMultiplier: Float = 1.0f,
    val soundEffectsEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val physicsBounciness: Float = 0.75f,
    val physicsGravityStrength: Float = 980f,
    val customApiKey: String = "",
    val adminPin: String = "0000",
    val isLockdownEnforced: Boolean = false,
    val disableDeviceSettingsAccess: Boolean = false
)

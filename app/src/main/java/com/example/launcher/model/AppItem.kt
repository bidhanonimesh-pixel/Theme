package com.example.launcher.model

import android.graphics.drawable.Drawable

data class AppItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val category: String = "General",
    val isHidden: Boolean = false,
    val isLocked: Boolean = false,
    val isFavorite: Boolean = false,
    val clickCount: Int = 0,
    val orbitSlotIndex: Int = -1
)

data class OrbitAppSlot(
    val slotIndex: Int,
    val label: String,
    val defaultIconSymbol: String,
    val assignedPackage: String? = null,
    val customIconRes: Int? = null,
    val angleDegrees: Float = 0f
)

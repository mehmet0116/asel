package com.aikodasistani.aikodasistani.models

/**
 * Represents a deep thinking level configuration
 * @param level The level number (1-5)
 * @param name Display name for the level
 * @param color Color resource for the level
 * @param description Description of what this level does
 * @param thinkingTime Time to spend thinking in milliseconds
 * @param detailMultiplier Multiplier for detail level
 */
data class ThinkingLevel(
    val level: Int,
    val name: String,
    val color: Int,
    val description: String,
    val thinkingTime: Long,
    val detailMultiplier: Double
)

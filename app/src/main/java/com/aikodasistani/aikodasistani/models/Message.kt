package com.aikodasistani.aikodasistani.models

/**
 * Represents a message in the chat interface
 * @param text The message content
 * @param isSentByUser Whether the message was sent by the user (true) or AI (false)
 * @param id Unique identifier for the message
 * @param isThinking Whether this is a thinking/processing message
 * @param thinkingSteps List of thinking steps for processing visualization
 */
data class Message(
    var text: String,
    val isSentByUser: Boolean,
    var id: Long = 0,
    val isThinking: Boolean = false,
    val thinkingSteps: MutableList<String> = mutableListOf()
)

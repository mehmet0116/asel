package com.aikodasistani.aikodasistani.data

import kotlinx.serialization.Serializable

@Serializable
data class ModelProvider(
    val provider: String,
    val models: List<String>
)

@Serializable
data class ModelConfig(
    val providers: List<ModelProvider>
)
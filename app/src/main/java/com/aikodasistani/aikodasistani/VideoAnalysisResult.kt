package com.aikodasistani.aikodasistani

sealed class VideoAnalysisResult {
    data class Success(val analysis: String) : VideoAnalysisResult()
    data class Error(val message: String) : VideoAnalysisResult()
}
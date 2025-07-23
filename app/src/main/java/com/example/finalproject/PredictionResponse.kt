package com.example.finalproject

import com.google.gson.annotations.SerializedName

data class PredictionResponse(
    @SerializedName("label")
    val label: String,

    @SerializedName("confidence")
    val confidence: Float,

    @SerializedName("suggestion")
    val suggestion: String
)
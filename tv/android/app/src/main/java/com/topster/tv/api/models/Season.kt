package com.topster.tv.api.models

import com.google.gson.annotations.SerializedName

data class Season(
    @SerializedName("id") val id: String,
    @SerializedName("number") val number: Int,
    @SerializedName("title") val title: String
)

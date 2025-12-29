package com.topster.tv.api.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("type") val type: String, // "movie" or "tv"
    @SerializedName("year") val year: String? = null,
    @SerializedName("rating") val rating: String? = null,
    @SerializedName("duration") val duration: String? = null,
    @SerializedName("quality") val quality: String? = null,
    @SerializedName("image") val image: String? = null,
    @SerializedName("url") val url: String
) : Parcelable

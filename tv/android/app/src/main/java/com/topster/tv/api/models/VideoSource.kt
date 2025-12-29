package com.topster.tv.api.models

import com.google.gson.annotations.SerializedName

data class Subtitle(
    @SerializedName("url") val url: String,
    @SerializedName("lang") val lang: String,
    @SerializedName("label") val label: String
)

data class VideoInfo(
    @SerializedName("url") val url: String,
    @SerializedName("subtitles") val subtitles: List<Subtitle>,
    @SerializedName("referer") val referer: String? = null,
    @SerializedName("quality") val quality: String? = null
)

data class VideoSource(
    @SerializedName("provider") val provider: String,
    @SerializedName("sources") val sources: List<VideoInfo>
)

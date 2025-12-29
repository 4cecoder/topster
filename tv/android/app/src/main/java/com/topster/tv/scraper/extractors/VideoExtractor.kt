package com.topster.tv.scraper.extractors

import com.topster.tv.api.models.VideoInfo

interface VideoExtractor {
    suspend fun extract(embedUrl: String, referer: String? = null): List<VideoInfo>
}

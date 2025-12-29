package com.topster.tv.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val rowId: Long = 0,

    val mediaId: String,
    val title: String,
    val type: String, // "movie" or "tv"
    val url: String,
    val posterImage: String? = null,

    // Episode info (for TV shows)
    val episodeId: String? = null,
    val episodeTitle: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,

    // Playback progress
    val position: Long = 0, // milliseconds
    val duration: Long = 0, // milliseconds
    val percentWatched: Float = 0f,
    val completed: Boolean = false,

    // Timestamps
    val lastWatched: Long = System.currentTimeMillis(),
    val firstWatched: Long = System.currentTimeMillis()
)

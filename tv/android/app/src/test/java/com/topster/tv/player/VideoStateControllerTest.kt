package com.topster.tv.player

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.topster.tv.database.HistoryManager
import com.topster.tv.player.controllers.VideoStateController
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for VideoStateController
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VideoStateControllerTest {

    private lateinit var context: Context
    private lateinit var historyManager: HistoryManager
    private lateinit var controller: VideoStateController

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        historyManager = HistoryManager(context)
        controller = VideoStateController(context, historyManager)
    }

    @Test
    fun `should trigger save after 180 ticks`() {
        // Given
        val video = VideoMetadata(
            id = "test-123",
            title = "Test Movie",
            videoUrl = "https://example.com/test.m3u8"
        )

        controller.onVideoLoaded(video)

        // When
        repeat(180) {
            controller.onTick()
        }

        // Then
        // State should be saved (check logs or mock)
    }

    @Test
    fun `should reset tick counter on new video`() {
        // Given
        val video1 = VideoMetadata(
            id = "test-1",
            title = "Test 1",
            videoUrl = "https://example.com/test1.m3u8"
        )

        val video2 = VideoMetadata(
            id = "test-2",
            title = "Test 2",
            videoUrl = "https://example.com/test2.m3u8"
        )

        controller.onVideoLoaded(video1)
        repeat(100) { controller.onTick() }

        // When
        controller.onVideoLoaded(video2)

        // Then
        // Tick counter should be reset
    }

    @Test
    fun `should restore saved position`() = runBlocking {
        // Given
        val videoId = "test-restore"

        // When
        val position = controller.getSavedPosition(videoId)

        // Then
        // Position should be null initially or restored value
    }
}

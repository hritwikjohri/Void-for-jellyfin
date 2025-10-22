package com.hritwik.avoid.websocket

import com.hritwik.avoid.data.remote.websocket.PlaybackEvent
import com.hritwik.avoid.data.repository.ContinueWatchingStore
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.library.UserData
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy

class ContinueWatchingStoreTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    private fun media(id: String, lastPlayed: String? = null) = MediaItem(
        id = id,
        name = id,
        type = "Video",
        overview = null,
        year = null,
        communityRating = null,
        runTimeTicks = 1000,
        primaryImageTag = null,
        logoImageTag = null,
        backdropImageTags = emptyList(),
        genres = emptyList(),
        isFolder = false,
        childCount = null,
        userData = UserData(lastPlayedDate = lastPlayed)
    )

    @Test
    fun `updates progress and resorts`() = scope.runTest {
        val store = ContinueWatchingStore(scope)
        val i1 = media("1", "2024-01-01")
        val i2 = media("2", "2024-02-01")
        store.setInitial(listOf(i1, i2))
        store.handle(PlaybackEvent.Progress("1", 900, 1000, "2024-05-01"))
        advanceTimeBy(1000)
        assertEquals("1", store.items.value.first().id)
    }

    @Test
    fun `removes completed items`() = scope.runTest {
        val store = ContinueWatchingStore(scope)
        val i1 = media("1", "2024-01-01")
        store.setInitial(listOf(i1))
        store.handle(PlaybackEvent.Stop("1", 1000, 1000, null))
        advanceTimeBy(1000)
        assertFalse(store.items.value.any { it.id == "1" })
    }
}

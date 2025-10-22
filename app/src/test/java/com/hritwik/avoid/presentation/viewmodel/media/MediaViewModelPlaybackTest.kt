package com.hritwik.avoid.presentation.viewmodel.media

import com.hritwik.avoid.MainDispatcherRule
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.repository.RelatedResources
import com.hritwik.avoid.domain.usecase.library.GetMediaItemDetailLocalUseCase
import com.hritwik.avoid.domain.usecase.media.GetEpisodesUseCase
import com.hritwik.avoid.domain.usecase.media.GetMediaItemDetailUseCase
import com.hritwik.avoid.domain.usecase.media.GetRelatedResourcesBatchUseCase
import com.hritwik.avoid.domain.usecase.media.GetSeasonsUseCase
import com.hritwik.avoid.domain.usecase.media.GetThemeSongIdsUseCase
import com.hritwik.avoid.domain.usecase.media.GetThemeSongsUseCase
import com.hritwik.avoid.presentation.ui.screen.player.ThemeSongController
import com.hritwik.avoid.utils.helpers.ConnectivityObserver
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.match
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaViewModelPlaybackTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getMediaItemDetailLocalUseCase = mockk<GetMediaItemDetailLocalUseCase>()
    private val getMediaItemDetailUseCase = mockk<GetMediaItemDetailUseCase>()
    private val getRelatedResourcesBatchUseCase = mockk<GetRelatedResourcesBatchUseCase>()
    private val getSeasonsUseCase = mockk<GetSeasonsUseCase>()
    private val getEpisodesUseCase = mockk<GetEpisodesUseCase>()
    private val getThemeSongsUseCase = mockk<GetThemeSongsUseCase>()
    private val getThemeSongIdsUseCase = mockk<GetThemeSongIdsUseCase>()
    private val preferencesManager = mockk<com.hritwik.avoid.data.local.PreferencesManager>()
    private val themeSongController = mockk<ThemeSongController>(relaxed = true)
    private val connectivityObserver = object : ConnectivityObserver {
        override val isConnected: StateFlow<Boolean> = MutableStateFlow(true)
        override val isOnWifi: StateFlow<Boolean> = MutableStateFlow(true)
    }

    private companion object {
        private const val USER_ID = "user-id"
        private const val ACCESS_TOKEN = "token"
    }

    @Before
    fun setup() {
        coEvery { getMediaItemDetailLocalUseCase(any()) } returns null
        coEvery { getRelatedResourcesBatchUseCase(any()) } returns NetworkResult.Success(RelatedResources(emptyList(), emptyList()))
        coEvery { getThemeSongIdsUseCase(any()) } returns NetworkResult.Success(emptyList())
        coEvery { getThemeSongsUseCase(any()) } returns NetworkResult.Success(emptyList())
        coEvery { getSeasonsUseCase(any()) } returns NetworkResult.Success(emptyList())
        coEvery { getEpisodesUseCase(any()) } returns NetworkResult.Success(emptyList())
        every { preferencesManager.getPlayThemeSongs() } returns flowOf(false)
        every { themeSongController.currentThemeSong() } returns null
        justRun { themeSongController.clear() }
    }

    @Test
    fun `season detail uses first episode as playback item`() = runTest {
        val seasonId = "season-1"
        val seasonItem = mediaItem(id = seasonId, type = "Season", isFolder = true)
        val firstEpisode = mediaItem(id = "episode-1", type = "Episode", isFolder = false, indexNumber = 1)
        val laterEpisode = mediaItem(id = "episode-2", type = "Episode", isFolder = false, indexNumber = 2)

        coEvery { getMediaItemDetailUseCase(any()) } returns NetworkResult.Success(seasonItem)
        coEvery { getEpisodesUseCase(match { it.seasonId == seasonId }) } returns NetworkResult.Success(listOf(laterEpisode, firstEpisode))

        val viewModel = createViewModel()

        viewModel.loadDetails(seasonId, USER_ID, ACCESS_TOKEN, MediaViewModel.DetailType.Season)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNotNull(state.playbackItem)
        assertEquals(firstEpisode.id, state.playbackItem?.id)
        assertEquals(firstEpisode.id, state.episodes?.first()?.id)
    }

    @Test
    fun `series detail selects first season first episode for playback`() = runTest {
        val seriesId = "series-1"
        val seriesItem = mediaItem(id = seriesId, type = "Series", isFolder = true)
        val seasonA = mediaItem(id = "season-a", type = "Season", isFolder = true, indexNumber = 2)
        val seasonB = mediaItem(id = "season-b", type = "Season", isFolder = true, indexNumber = 1)
        val firstEpisode = mediaItem(id = "episode-a", type = "Episode", isFolder = false, indexNumber = 1)
        val laterEpisode = mediaItem(id = "episode-b", type = "Episode", isFolder = false, indexNumber = 3)

        coEvery { getMediaItemDetailUseCase(any()) } returns NetworkResult.Success(seriesItem)
        coEvery { getSeasonsUseCase(any()) } returns NetworkResult.Success(listOf(seasonA, seasonB))
        coEvery { getEpisodesUseCase(match { it.seasonId == seasonB.id }) } returns NetworkResult.Success(listOf(laterEpisode, firstEpisode))

        val viewModel = createViewModel()

        viewModel.loadDetails(seriesId, USER_ID, ACCESS_TOKEN, MediaViewModel.DetailType.Series)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNotNull(state.playbackItem)
        assertEquals(firstEpisode.id, state.playbackItem?.id)
        assertEquals(listOf(seasonB.id, seasonA.id), state.seasons?.map { it.id })
    }

    private fun createViewModel(): MediaViewModel {
        return MediaViewModel(
            getMediaItemDetailLocalUseCase = getMediaItemDetailLocalUseCase,
            getMediaItemDetailUseCase = getMediaItemDetailUseCase,
            getRelatedResourcesBatchUseCase = getRelatedResourcesBatchUseCase,
            getSeasonsUseCase = getSeasonsUseCase,
            getEpisodesUseCase = getEpisodesUseCase,
            getThemeSongsUseCase = getThemeSongsUseCase,
            getThemeSongIdsUseCase = getThemeSongIdsUseCase,
            preferencesManager = preferencesManager,
            themeSongController = themeSongController,
            connectivityObserver = connectivityObserver
        )
    }

    private fun mediaItem(
        id: String,
        type: String,
        isFolder: Boolean,
        indexNumber: Int? = null,
        parentIndexNumber: Int? = null,
        runTimeTicks: Long? = null
    ): MediaItem {
        return MediaItem(
            id = id,
            name = id,
            title = null,
            type = type,
            overview = null,
            year = null,
            communityRating = null,
            runTimeTicks = runTimeTicks,
            primaryImageTag = null,
            thumbImageTag = null,
            logoImageTag = null,
            backdropImageTags = emptyList(),
            genres = emptyList(),
            isFolder = isFolder,
            childCount = null,
            userData = null,
            taglines = emptyList(),
            people = emptyList(),
            mediaSources = emptyList(),
            hasSubtitles = false,
            versionName = null,
            seriesName = null,
            seriesId = null,
            parentIndexNumber = parentIndexNumber,
            indexNumber = indexNumber
        )
    }
}

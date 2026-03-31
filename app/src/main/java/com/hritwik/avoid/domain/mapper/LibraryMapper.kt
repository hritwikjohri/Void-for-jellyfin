package com.hritwik.avoid.domain.mapper

import com.hritwik.avoid.data.remote.dto.library.BaseItemDto
import com.hritwik.avoid.data.remote.dto.library.UserDataDto
import com.hritwik.avoid.domain.model.library.Library
import com.hritwik.avoid.domain.model.library.LibraryType
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.library.Person
import com.hritwik.avoid.domain.model.library.UserData
import com.hritwik.avoid.utils.extensions.extractTvdbId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryMapper @Inject constructor(
    private val playbackMapper: PlaybackMapper
) {

    
    fun mapBaseItemToLibrary(dto: BaseItemDto): Library {
        return Library(
            id = dto.id,
            name = dto.name ?: "Unknown LibrarySection",
            type = LibraryType.fromString(dto.collectionType),
            itemCount = dto.childCount,
            primaryImageTag = dto.imageTags?.primary,
            isFolder = dto.isFolder
        )
    }

    
    fun mapBaseItemToMediaItem(dto: BaseItemDto): MediaItem {
        return MediaItem(
            id = dto.id,
            name = dto.name ?: "Unknown",
            title = dto.title,
            type = dto.type ?: "Unknown",
            overview = dto.overview,
            year = dto.productionYear,
            communityRating = dto.communityRating,
            runTimeTicks = dto.runTimeTicks,
            primaryImageTag = dto.imageTags?.primary,
            thumbImageTag = dto.imageTags?.thumb,
            logoImageTag = dto.imageTags?.logo, 
            backdropImageTags = dto.backdropImageTags,
            genres = dto.genres,
            isFolder = dto.isFolder,
            childCount = dto.childCount,
            userData = mapUserDataDtoToUserData(dto.userData),
            taglines = dto.taglines,
            people = dto.people.map { mapPersonDtoToPerson(it) },
            mediaSources = playbackMapper.mapMediaSourceDtoListToMediaSourceList(dto.mediaSources),
            hasSubtitles = dto.hasSubtitles,
            versionName = dto.versionName,
            seriesName = dto.seriesName,
            seriesId = dto.seriesId,
            parentIndexNumber = dto.parentIndexNumber,
            indexNumber = dto.indexNumber,
            tvdbId = dto.providerIds.extractTvdbId()
        )
    }

    
    fun mapUserDataDtoToUserData(dto: UserDataDto?): UserData? {
        return dto?.let {
            UserData(
                isFavorite = it.isFavorite,
                playbackPositionTicks = it.playbackPositionTicks,
                playCount = it.playCount,
                played = it.played,
                lastPlayedDate = it.lastPlayedDate,
                isWatchlist = false,
                pendingFavorite = false,
                pendingPlayed = false,
                pendingWatchlist = false
            )
        }
    }

    private fun mapPersonDtoToPerson(dto: com.hritwik.avoid.data.remote.dto.library.PersonDto): Person {
        return Person(
            id = dto.id,
            name = dto.name ?: "Unknown",
            role = dto.role,
            type = dto.type,
            primaryImageTag = dto.primaryImageTag
        )
    }

    
    fun mapBaseItemListToLibraryList(dtoList: List<BaseItemDto>): List<Library> {
        return dtoList.map { mapBaseItemToLibrary(it) }
    }

    
    fun mapBaseItemListToMediaItemList(dtoList: List<BaseItemDto>): List<MediaItem> {
        return dtoList.map { mapBaseItemToMediaItem(it) }
    }

    
    fun filterMediaItemsByType(items: List<MediaItem>, type: String): List<MediaItem> {
        return items.filter { it.type.equals(type, ignoreCase = true) }
    }

    
    fun sortMediaItemsByName(items: List<MediaItem>): List<MediaItem> {
        return items.sortedBy { it.name }
    }

    
    fun sortMediaItemsByYear(items: List<MediaItem>): List<MediaItem> {
        return items.sortedByDescending { it.year }
    }

    
    fun filterUnwatchedItems(items: List<MediaItem>): List<MediaItem> {
        return items.filter { it.userData?.played != true }
    }

    
    fun filterFavoriteItems(items: List<MediaItem>): List<MediaItem> {
        return items.filter { it.userData?.isFavorite == true }
    }

    
    fun filterResumeItems(items: List<MediaItem>): List<MediaItem> {
        return items.filter {
            val userData = it.userData
            userData != null &&
                    userData.playbackPositionTicks > 0 &&
                    !userData.played
        }
    }
}

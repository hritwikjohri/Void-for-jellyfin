package com.hritwik.avoid.data.remote.dto.library

import com.hritwik.avoid.data.remote.dto.media.MediaSourceDto
import com.hritwik.avoid.data.remote.dto.media.MediaStreamDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BaseItemDto(
    @SerialName("Id")
    val id: String,
    @SerialName("Name")
    val name: String? = null,
    @SerialName("Title")
    val title: String? = null,
    @SerialName("Type")
    val type: String? = null,
    @SerialName("CollectionType")
    val collectionType: String? = null,
    @SerialName("Overview")
    val overview: String? = null,
    @SerialName("ProductionYear")
    val productionYear: Int? = null,
    @SerialName("CommunityRating")
    val communityRating: Double? = null,
    @SerialName("RunTimeTicks")
    val runTimeTicks: Long? = null,
    @SerialName("PrimaryImageAspectRatio")
    val primaryImageAspectRatio: Double? = null,
    @SerialName("ImageTags")
    val imageTags: ImageTags? = null,
    @SerialName("BackdropImageTags")
    val backdropImageTags: List<String> = emptyList(),
    @SerialName("Genres")
    val genres: List<String> = emptyList(),
    @SerialName("IsFolder")
    val isFolder: Boolean = false,
    @SerialName("ChildCount")
    val childCount: Int? = null,
    @SerialName("UserData")
    val userData: UserDataDto? = null,
    @SerialName("MediaSources")
    val mediaSources: List<MediaSourceDto> = emptyList(),
    @SerialName("MediaStreams")
    val mediaStreams: List<MediaStreamDto> = emptyList(),
    @SerialName("Taglines")
    val taglines: List<String> = emptyList(),
    @SerialName("People")
    val people: List<PersonDto> = emptyList(),
    @SerialName("HasSubtitles")
    val hasSubtitles: Boolean = false,
    @SerialName("VersionName")
    val versionName: String? = null,
    @SerialName("SeriesId")
    val seriesId: String? = null,
    @SerialName("SeriesName")
    val seriesName: String? = null,
    @SerialName("SeriesPrimaryImageTag")
    val seriesPrimaryImageTag: String? = null,
    @SerialName("SeasonId")
    val seasonId: String? = null,
    @SerialName("SeasonName")
    val seasonName: String? = null,
    @SerialName("SeasonPrimaryImageTag")
    val seasonPrimaryImageTag: String? = null,
    @SerialName("ParentIndexNumber")
    val parentIndexNumber: Int? = null,
    @SerialName("IndexNumber")
    val indexNumber: Int? = null
)
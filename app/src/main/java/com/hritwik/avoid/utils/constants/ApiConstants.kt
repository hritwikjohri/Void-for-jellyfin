package com.hritwik.avoid.utils.constants

object ApiConstants {

    const val CLIENT_NAME = "Void Android"
    private const val DEFAULT_DEVICE_NAME = "Android Device"

    
    const val ENDPOINT_SERVER_INFO = "System/Info/Public"
    const val ENDPOINT_SERVER_PING = "System/Ping"
    const val ENDPOINT_AUTHENTICATE = "Users/AuthenticateByName"
    const val ENDPOINT_LOGOUT = "Sessions/Logout"
    const val ENDPOINT_CURRENT_USER = "Users/Me"
    const val ENDPOINT_USER_LIBRARIES = "Users/{userId}/Views"
    const val ENDPOINT_LIBRARY_ITEMS = "Users/{userId}/Items"
    const val ENDPOINT_LATEST_ITEMS = "Users/{userId}/Items/Latest"
    const val ENDPOINT_RESUME_ITEMS = "Users/{userId}/Items/Resume"
    const val ENDPOINT_ITEM_IMAGES = "Items/{itemId}/Images/{imageType}"

    const val DEFAULT_LIBRARY_IMAGE_TYPES = "Primary,Backdrop,Thumb"
    const val LEGACY_LIBRARY_IMAGE_TYPES = "Primary,Backdrop"

    
    const val IMAGE_TYPE_PRIMARY = "Primary"
    const val IMAGE_TYPE_BACKDROP = "Backdrop"
    const val IMAGE_TYPE_THUMB = "Thumb"
    const val IMAGE_TYPE_LOGO = "Logo"

    
    const val ITEM_TYPE_MOVIE = "Movie"
    const val ITEM_TYPE_SERIES = "Series"
    const val ITEM_TYPE_SEASON = "Season"
    const val ITEM_TYPE_EPISODE = "Episode"

    
    const val COLLECTION_TYPE_MOVIES = "movies"
    const val COLLECTION_TYPE_TVSHOWS = "tvshows"
    const val COLLECTION_TYPE_MUSIC = "music"
    const val COLLECTION_TYPE_PHOTOS = "photos"
    const val COLLECTION_TYPE_BOOKS = "books"
    const val COLLECTION_TYPE_BOXSETS = "boxsets"

    
    const val SORT_ORDER_ASCENDING = "Ascending"
    const val SORT_ORDER_DESCENDING = "Descending"

    
    const val SORT_BY_NAME = "SortName"
    const val SORT_BY_DATE_CREATED = "DateCreated"
    const val SORT_BY_DATE_PLAYED = "DatePlayed"
    const val SORT_BY_PREMIERE_DATE = "PremiereDate"
    const val SORT_BY_COMMUNITY_RATING = "CommunityRating"
    const val SORT_BY_PLAY_COUNT = "PlayCount"

    
    const val DEFAULT_IMAGE_QUALITY = 100
    const val THUMBNAIL_MAX_WIDTH = 800
    const val BACKDROP_MAX_WIDTH = 1920
    const val POSTER_MAX_WIDTH = 1080

    
    const val HEADER_AUTHORIZATION = "X-Emby-Authorization"
    const val HEADER_TOKEN = "X-Emby-Token"
    const val HEADER_USER_AGENT = "User-Agent"
    const val HEADER_CONTENT_TYPE = "Content-Type"

    
    const val AUTH_SCHEME = "MediaBrowser"
}

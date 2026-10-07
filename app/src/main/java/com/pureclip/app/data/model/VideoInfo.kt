package com.pureclip.app.data.model

import java.io.Serializable

data class VideoInfo(
    val title: String,
    val thumbnailUrl: String,
    val videoUrl: String,
    val platform: String,
    val duration: Int? = null,
    val authorUsername: String? = null,
    val authorAvatar: String? = null,
    val likes: Long? = null,
    val comments: Long? = null,
    val shares: Long? = null,
    val downloads: List<DownloadItem>? = null
) : Serializable

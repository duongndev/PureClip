package com.pureclip.app.data.model

data class ParseVideoRequest(
    val url: String
)

data class ParseVideoResponse(
    val success: Boolean,
    val data: ParsedData?,
    val error: ParseError?
)

data class ParsedData(
    val platform: String?,
    val id: String?,
    val title: String?,
    val thumbnail: String?,
    val duration: Int?,
    val author: Author?,
    val stats: Stats?,
    val downloads: List<DownloadItem>?
)

data class Author(
    val username: String?,
    val name: String?,
    val avatar: String?,
    val verified: Boolean?
) : java.io.Serializable

data class Stats(
    val likes: Long?,
    val comments: Long?,
    val shares: Long?
) : java.io.Serializable

data class DownloadItem(
    val url: String,
    val quality: String?,
    val label: String?,
    val type: String?,
    val format: String?,
    val size: Long?,
    val has_watermark: Boolean?
) : java.io.Serializable

data class ParseError(
    val code: String?,
    val message: String?
)

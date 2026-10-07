package com.pureclip.app.data.repository

import com.pureclip.app.data.model.VideoInfo

interface VideoRepository {
    suspend fun fetchVideoInfo(inputUrl: String): Result<VideoInfo>
}

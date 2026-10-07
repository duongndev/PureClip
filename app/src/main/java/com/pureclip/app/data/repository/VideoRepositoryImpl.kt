package com.pureclip.app.data.repository

import android.util.Log
import com.pureclip.app.data.api.PureClipApi
import com.pureclip.app.data.api.RetrofitClient
import com.pureclip.app.data.model.ParseVideoRequest
import com.pureclip.app.data.model.VideoInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoRepositoryImpl(
    private val api: PureClipApi = RetrofitClient.api,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : VideoRepository {

    override suspend fun fetchVideoInfo(inputUrl: String): Result<VideoInfo> =
        withContext(ioDispatcher) {
            try {
                val trimmedUrl = inputUrl.trim()
                Log.d("VideoRepository", "Parsing video URL: $trimmedUrl")

                val request = ParseVideoRequest(url = trimmedUrl)
                val response = api.parseVideo(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success && body.data != null) {
                        val data = body.data
                        val title = data.title ?: "PureClip Video"
                        val cover = data.thumbnail ?: ""
                        val platform = data.platform ?: "Unknown"

                        val authorUsername = data.author?.username ?: data.author?.name ?: ""
                        val authorAvatar = data.author?.avatar ?: ""
                        val likes = data.stats?.likes
                        val comments = data.stats?.comments
                        val shares = data.stats?.shares
                        val duration = data.duration

                        val downloads = data.downloads ?: emptyList()
                        val primaryDownload = downloads.firstOrNull { it.has_watermark == false && it.format == "mp4" }
                            ?: downloads.firstOrNull { it.type == "video_no_watermark" }
                            ?: downloads.firstOrNull { it.type == "video" || it.format == "mp4" }
                            ?: downloads.firstOrNull()

                        val playUrl = primaryDownload?.url ?: ""

                        if (playUrl.isNotEmpty() || downloads.isNotEmpty()) {
                            Log.d("VideoRepository", "Successfully extracted video: $playUrl with ${downloads.size} downloads")
                            Result.success(
                                VideoInfo(
                                    title = title,
                                    thumbnailUrl = cover,
                                    videoUrl = playUrl,
                                    platform = platform,
                                    duration = duration,
                                    authorUsername = authorUsername,
                                    authorAvatar = authorAvatar,
                                    likes = likes,
                                    comments = comments,
                                    shares = shares,
                                    downloads = downloads
                                )
                            )
                        } else {
                            Log.e("VideoRepository", "API returned success but no download URLs found")
                            Result.failure(Exception("Không tìm thấy link tải video."))
                        }
                    } else {
                        val errorMsg = body?.error?.message ?: "Lỗi không xác định từ server"
                        Log.e("VideoRepository", "API Error: $errorMsg")
                        Result.failure(Exception(errorMsg))
                    }
                } else {
                    Log.e("VideoRepository", "HTTP Error: ${response.code()}")
                    Result.failure(Exception("Lỗi kết nối máy chủ: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("VideoRepository", "Exception during video fetch", e)
                val resolvedException = when (e) {
                    is java.net.UnknownHostException,
                    is java.net.ConnectException -> com.pureclip.app.utils.NoInternetException("Không thể kết nối Internet. Vui lòng kiểm tra lại mạng.")
                    is java.net.SocketTimeoutException -> Exception("Quá thời gian kết nối đến máy chủ. Vui lòng thử lại sau.")
                    else -> e
                }
                Result.failure(resolvedException)
            }
        }
}

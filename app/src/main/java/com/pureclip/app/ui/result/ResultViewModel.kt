package com.pureclip.app.ui.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pureclip.app.BuildConfig
import com.pureclip.app.R
import com.pureclip.app.data.model.DownloadItem
import com.pureclip.app.data.model.VideoInfo
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.Locale

class ResultViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    private val _eventChannel = Channel<ResultEvent>(Channel.BUFFERED)
    val eventFlow = _eventChannel.receiveAsFlow()

    init {
        val initialVideoInfo = savedStateHandle.get<VideoInfo>(EXTRA_VIDEO_INFO)
        if (initialVideoInfo != null) {
            processVideoInfo(initialVideoInfo)
        }
    }

    fun setVideoInfo(videoInfo: VideoInfo?) {
        if (videoInfo == null) {
            _uiState.value = ResultUiState.Error("Không tìm thấy thông tin video")
            return
        }
        savedStateHandle[EXTRA_VIDEO_INFO] = videoInfo
        processVideoInfo(videoInfo)
    }

    fun onDownloadOptionClicked(option: DownloadOptionUiModel) {
        viewModelScope.launch {
            _eventChannel.send(ResultEvent.StartDownload(option))
        }
    }

    private fun processVideoInfo(videoInfo: VideoInfo) {
        val downloadOptions = buildDownloadOptions(videoInfo)

        _uiState.value = ResultUiState.Success(
            videoInfo = videoInfo,
            title = videoInfo.title,
            thumbnailUrl = resolveUrl(videoInfo.thumbnailUrl),
            downloadOptions = downloadOptions
        )
    }

    private fun buildDownloadOptions(videoInfo: VideoInfo): List<DownloadOptionUiModel> {
        val downloads = videoInfo.downloads ?: emptyList()
        val rawList = if (downloads.isEmpty()) {
            listOf(
                DownloadItem(
                    url = videoInfo.videoUrl,
                    quality = "HD",
                    label = "Tải video HD (Không Logo)",
                    type = "video",
                    format = "mp4",
                    size = 0L,
                    has_watermark = false
                )
            )
        } else {
            downloads
        }

        return rawList.map { item ->
            val format = item.format?.uppercase() ?: "MP4"
            val label = item.label ?: "Tải xuống ($format)"
            val sizeStr = formatSize(item.size)
            val isAudio = item.format?.equals("mp3", ignoreCase = true) == true
            val watermarkText = if (item.has_watermark == true) "Có logo" else "Không logo"

            val details = buildString {
                append("Định dạng: $format")
                if (sizeStr.isNotEmpty()) append(" • $sizeStr")
                if (!isAudio) append(" • $watermarkText")
            }

            val iconRes = if (isAudio) {
                android.R.drawable.ic_media_play
            } else {
                R.drawable.ic_download
            }

            DownloadOptionUiModel(
                title = label,
                details = details,
                iconRes = iconRes,
                downloadItem = item,
                videoTitle = videoInfo.title,
                platform = videoInfo.platform
            )
        }
    }

    fun formatSize(bytes: Long?): String {
        if (bytes == null || bytes <= 0L) return ""
        val mb = bytes / (1024.0 * 1024.0)
        return String.format(Locale.getDefault(), "%.1f MB", mb)
    }

    fun resolveUrl(url: String?): String {
        if (url.isNullOrEmpty()) return ""
        return if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            val baseUrl = BuildConfig.BASE_API_URL.removeSuffix("/")
            val path = if (url.startsWith("/")) url else "/$url"
            "$baseUrl$path"
        }
    }

    companion object {
        const val EXTRA_VIDEO_INFO = "video_info"
    }
}

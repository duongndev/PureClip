package com.pureclip.app.ui.result

import androidx.annotation.DrawableRes
import com.pureclip.app.data.model.DownloadItem
import com.pureclip.app.data.model.VideoInfo

sealed interface ResultUiState {
    data object Loading : ResultUiState

    data class Success(
        val videoInfo: VideoInfo,
        val title: String,
        val thumbnailUrl: String,
        val downloadOptions: List<DownloadOptionUiModel>
    ) : ResultUiState

    data class Error(val message: String) : ResultUiState
}

data class DownloadOptionUiModel(
    val title: String,
    val details: String,
    @param:DrawableRes val iconRes: Int,
    val downloadItem: DownloadItem,
    val videoTitle: String,
    val platform: String
)

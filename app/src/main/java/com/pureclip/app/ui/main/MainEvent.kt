package com.pureclip.app.ui.main

import com.pureclip.app.data.model.VideoInfo

sealed interface MainEvent {
    data class NavigateToResult(val videoInfo: VideoInfo) : MainEvent
    data class ShowToast(val message: String) : MainEvent
}

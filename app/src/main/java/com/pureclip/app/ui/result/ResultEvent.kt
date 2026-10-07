package com.pureclip.app.ui.result

sealed interface ResultEvent {
    data class StartDownload(val option: DownloadOptionUiModel) : ResultEvent
    data class ShowToast(val message: String) : ResultEvent
}

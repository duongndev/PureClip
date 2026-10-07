package com.pureclip.app.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pureclip.app.data.repository.VideoRepository
import com.pureclip.app.data.repository.VideoRepositoryImpl
import com.pureclip.app.utils.NoInternetException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class PureClipViewModel(
    private val videoRepository: VideoRepository = VideoRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Idle)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _eventChannel = Channel<MainEvent>(Channel.BUFFERED)
    val eventFlow = _eventChannel.receiveAsFlow()

    var lastEnteredUrl: String = ""
        private set

    fun fetchVideo(url: String) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isEmpty()) {
            viewModelScope.launch {
                _eventChannel.send(MainEvent.ShowToast("Vui lòng dán hoặc nhập liên kết video"))
            }
            return
        }

        lastEnteredUrl = trimmedUrl
        _uiState.value = MainUiState.Loading

        viewModelScope.launch {
            val result = videoRepository.fetchVideoInfo(trimmedUrl)

            result.fold(
                onSuccess = { videoInfo ->
                    _uiState.value = MainUiState.Idle
                    _eventChannel.send(MainEvent.NavigateToResult(videoInfo))
                },
                onFailure = { error ->
                    Log.e("PureClipViewModel", "Error fetching video: ${error.message}", error)
                    if (error is NoInternetException) {
                        _uiState.value = MainUiState.NoInternet
                    } else {
                        _uiState.value = MainUiState.Error(error.message)
                    }
                }
            )
        }
    }

    fun setNoInternet() {
        _uiState.value = MainUiState.NoInternet
    }

    fun retry() {
        if (lastEnteredUrl.isNotEmpty()) {
            fetchVideo(lastEnteredUrl)
        } else {
            resetState()
        }
    }

    fun resetState() {
        _uiState.value = MainUiState.Idle
    }
}

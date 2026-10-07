package com.pureclip.app.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SplashViewModel : ViewModel() {

    private val _eventChannel = Channel<SplashEvent>(Channel.BUFFERED)
    val eventFlow = _eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            delay(SPLASH_DURATION_MS)
            _eventChannel.send(SplashEvent.NavigateToMain)
        }
    }

    companion object {
        private const val SPLASH_DURATION_MS = 1500L
    }
}

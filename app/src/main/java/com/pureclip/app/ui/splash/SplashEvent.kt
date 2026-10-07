package com.pureclip.app.ui.splash

sealed interface SplashEvent {
    data object NavigateToMain : SplashEvent
}

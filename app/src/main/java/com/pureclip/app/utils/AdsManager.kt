package com.pureclip.app.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.pureclip.app.BuildConfig

/**
 * Enhanced AdsManager for managing AdMob Banner and Interstitial Ads professionally.
 * - Robust lifecycle management (destroying AdViews to prevent memory leaks)
 * - Automatic pre-loading and retry logic for Interstitial ads
 * - Strict leak-free application context usage
 * - Ad status callbacks and telemetry logging
 */
object AdsManager {
    private const val TAG = "AdsManager"

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    private val BANNER_AD_UNIT_ID = BuildConfig.BANNER_MAIN_ID.ifBlank { AdConstants.TEST_BANNER }
    private val INTERSTITIAL_AD_UNIT_ID = BuildConfig.INTERSTITIAL_DOWNLOAD_ID.ifBlank { AdConstants.TEST_INTERSTITIAL }

    /**
     * Initializes the Google Mobile Ads SDK and pre-loads the first interstitial ad.
     */
    fun initialize(context: Context) {
        val appContext = context.applicationContext
        MobileAds.initialize(appContext) { initializationStatus ->
            Log.d(TAG, "MobileAds Initialization Complete: $initializationStatus")
        }
        loadInterstitial(appContext)
    }

    /**
     * Loads a banner ad into the specified container FrameLayout.
     * Cleans up any existing AdView first to prevent memory leaks and attaches to the Activity lifecycle.
     */
    fun loadBannerAd(context: Context, container: FrameLayout) {
        // Clean up existing ad view if present
        destroyBanner(container)

        val adView = AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = BANNER_AD_UNIT_ID
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "Banner ad loaded successfully.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Banner ad failed to load: ${error.message}")
                }
            }
        }

        container.addView(adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        // Tự động gắn kết vòng đời của Activity
        (context as? LifecycleOwner)?.lifecycle?.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> adView.pause()
                    Lifecycle.Event.ON_RESUME -> adView.resume()
                    Lifecycle.Event.ON_DESTROY -> {
                        adView.destroy()
                        container.removeAllViews()
                        source.lifecycle.removeObserver(this)
                    }
                    else -> {}
                }
            }
        })
    }

    fun pauseBanner(container: FrameLayout) {
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is AdView) {
                child.pause()
            }
        }
    }

    fun resumeBanner(container: FrameLayout) {
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is AdView) {
                child.resume()
            }
        }
    }

    fun destroyBanner(container: FrameLayout) {
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is AdView) {
                child.destroy()
            }
        }
        container.removeAllViews()
    }

    /**
     * Pre-loads an Interstitial Ad using ApplicationContext to prevent Activity leaks.
     */
    private fun loadInterstitial(context: Context) {
        if (isInterstitialLoading || interstitialAd != null) return
        isInterstitialLoading = true

        val appContext = context.applicationContext
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            appContext,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    Log.d(TAG, "Interstitial ad loaded successfully.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                    Log.e(TAG, "Interstitial ad failed to load: ${error.message}")
                }
            }
        )
    }

    /**
     * Shows the pre-loaded interstitial ad before performing an action (e.g. video download).
     * If the ad is not ready, calls [onAdDismissed] immediately and attempts to load a new one.
     */
    fun showInterstitial(activity: Activity, onAdDismissed: () -> Unit) {
        val currentAd = interstitialAd
        val appContext = activity.applicationContext
        if (currentAd != null) {
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed.")
                    interstitialAd = null
                    loadInterstitial(appContext)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Interstitial ad failed to show: ${adError.message}")
                    interstitialAd = null
                    loadInterstitial(appContext)
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad showed full screen content.")
                    interstitialAd = null
                }
            }
            currentAd.show(activity)
        } else {
            Log.w(TAG, "Interstitial ad was not ready yet. Proceeding with action.")
            onAdDismissed()
            loadInterstitial(appContext)
        }
    }
}

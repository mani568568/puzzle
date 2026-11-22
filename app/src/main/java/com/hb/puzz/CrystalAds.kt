package com.hb.puzz

import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.hb.puzz.data.GameSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.IOException

internal val LocalCrystalAds = staticCompositionLocalOf<CrystalAds?> { null }

// An earned wallet write must finish even when the activity rotates or the player leaves the level.
private val walletWrites = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

internal class CrystalAds(private val activity: ComponentActivity, private val settings: GameSettings, private val premium: PremiumBilling) {
    private val consent = UserMessagingPlatform.getConsentInformation(activity)
    private var initialized = false
    private var initializing = false
    var ready by mutableStateOf(false)
        private set
    var privacyRequired by mutableStateOf(false)
        private set

    fun updateConsent() {
        consent.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
            if (active()) {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { refresh() }
            }
        }, { refresh() })
    }

    private fun active() = !activity.isFinishing && !activity.isDestroyed

    private fun refresh() {
        if (!active()) return
        privacyRequired = consent.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        ready = initialized && consent.canRequestAds()
        if (!consent.canRequestAds() || initializing || initialized) return
        initializing = true
        MobileAds.initialize(activity.applicationContext) {
            activity.runOnUiThread {
                initialized = true
                initializing = false
                ready = active() && consent.canRequestAds()
            }
        }
    }

    fun openPrivacy() {
        if (!active()) return
        ready = false
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            refresh()
            if (error != null && active()) notifyPlayer("Ad privacy choices are unavailable. Try again later.")
        }
    }

    private var interstitial: InterstitialAd? = null
    private var loadedAt = 0L
    private var fullScreen = false

    private fun mayShowStandardAds() =
        active() && ready && consent.canRequestAds() && premium.ownership == Ownership.FREE

    suspend fun preloadInterstitials() {
        try {
            while (mayShowStandardAds()) {
                if (interstitial == null || SystemClock.elapsedRealtime() - loadedAt >= 55 * 60_000L) {
                    interstitial = null
                    val loaded = suspendCancellableCoroutine<InterstitialAd?> { continuation ->
                        InterstitialAd.load(activity, BuildConfig.INTERSTITIAL_AD_ID, AdRequest.Builder().build(),
                            object : InterstitialAdLoadCallback() {
                                override fun onAdLoaded(ad: InterstitialAd) {
                                    if (continuation.isActive) continuation.resume(ad)
                                }
                                override fun onAdFailedToLoad(error: LoadAdError) {
                                    if (continuation.isActive) continuation.resume(null)
                                }
                            })
                    }
                    if (mayShowStandardAds()) {
                        interstitial = loaded
                        loadedAt = SystemClock.elapsedRealtime()
                    }
                }
                delay(if (interstitial == null) 60_000L else 15_000L)
            }
        } finally {
            interstitial = null
        }
    }

    fun showLevelBreak() {
        if (!mayShowStandardAds() || fullScreen ||
            !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        val ad = interstitial ?: return
        interstitial = null
        if (SystemClock.elapsedRealtime() - loadedAt >= 55 * 60_000L) return
        fullScreen = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { fullScreen = false }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { fullScreen = false }
        }
        ad.show(activity)
    }

    fun load(onLoaded: (RewardedAd?) -> Unit) {
        if (!ready || !consent.canRequestAds()) {
            onLoaded(null)
            return
        }
        RewardedAd.load(activity, BuildConfig.REWARDED_AD_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) = onLoaded(ad)
                override fun onAdFailedToLoad(error: LoadAdError) = onLoaded(null)
            })
    }

    fun show(ad: RewardedAd, onEarned: () -> Unit, onClosed: () -> Unit) {
        if (!active() || fullScreen || !ready || !consent.canRequestAds()) {
            onClosed()
            return
        }
        fullScreen = true
        fun closed() {
            fullScreen = false
            onClosed()
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = closed()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = closed()
        }
        var credited = false
        ad.show(activity) {
            if (!credited) {
                credited = true
                walletWrites.launch {
                    try {
                        settings.creditAdCrystal()
                        onEarned()
                        notifyPlayer("+1 Crystal added")
                    } catch (_: IOException) {
                        notifyPlayer("The Crystal could not be saved.")
                    }
                }
            }
        }
    }

    private fun notifyPlayer(message: String) {
        Toast.makeText(activity.applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}

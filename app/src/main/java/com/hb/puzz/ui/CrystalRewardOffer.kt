package com.hb.puzz.ui

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.rewarded.RewardedAd
import com.hb.puzz.LocalCrystalAds
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
internal fun CrystalRewardOffer(levelId: Int) {
    val ads = LocalCrystalAds.current ?: return
    var earned by rememberSaveable(levelId) { mutableStateOf(false) }
    if (earned || !ads.ready) return

    var ad by remember(ads, levelId) { mutableStateOf<RewardedAd?>(null) }
    var showing by remember { mutableStateOf(false) }
    LaunchedEffect(ads, levelId, showing) {
        if (showing) return@LaunchedEffect
        while (true) {
            ad = suspendCancellableCoroutine<RewardedAd?> { result ->
                ads.load { loaded -> if (result.isActive) result.resume(loaded) }
            }
            // Google ads expire after an hour. Retry no-fill less frequently than screen redraws.
            delay(if (ad == null) 60_000L else 55 * 60_000L)
            ad = null
        }
    }
    if (ad != null && !showing) {
        TextButton(onClick = {
            val loaded = ad ?: return@TextButton
            ad = null
            showing = true
            ads.show(loaded, onEarned = { earned = true }, onClosed = { showing = false })
        }) { Text("Watch an ad for 1 Crystal") }
    }
}

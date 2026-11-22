package com.hb.puzz.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.*
import com.hb.puzz.BuildConfig

/** Persistent, non-scrolling footer. Loading and refreshing never change its height. */
@Composable
internal fun BottomBanner(loadAds: Boolean) {
    BoxWithConstraints(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        contentAlignment = Alignment.Center
    ) {
        if (maxWidth < 320.dp) return@BoxWithConstraints
        Box(Modifier.height(54.dp).width(320.dp), contentAlignment = Alignment.BottomCenter) {
            if (!loadAds) return@Box
            val context = LocalContext.current
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            val banner = remember(context) {
                AdView(context).apply {
                    adUnitId = BuildConfig.BANNER_AD_ID
                    setAdSize(AdSize.BANNER)
                }
            }
            AndroidView(factory = { banner }, modifier = Modifier.size(320.dp, 50.dp))
            DisposableEffect(banner, lifecycle) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> banner.resume()
                        Lifecycle.Event.ON_PAUSE -> banner.pause()
                        else -> Unit
                    }
                }
                lifecycle.addObserver(observer)
                banner.loadAd(AdRequest.Builder().build())
                onDispose {
                    lifecycle.removeObserver(observer)
                    banner.destroy()
                }
            }
        }
    }
}

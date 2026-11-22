package com.hb.puzz

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.hb.puzz.ui.BottomBanner
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.DisposableEffect
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import com.hb.puzz.data.GameSettings
import com.hb.puzz.ui.CozyBlocksApp
import com.hb.puzz.ui.theme.CozyBlocksTheme

/** Single-activity entry point for Cozy Picture Blocks. */
class MainActivity : ComponentActivity() {
    private lateinit var premium: PremiumBilling

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settings = GameSettings(applicationContext)
        premium = PremiumBilling(this)
        val ads = CrystalAds(this, settings, premium)
        setContent {
            val darkTheme by settings.darkThemeFlow.collectAsState(initial = false)

            DisposableEffect(darkTheme) {
                val transparent = android.graphics.Color.TRANSPARENT
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent),
                    navigationBarStyle = if (darkTheme) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
                )
                onDispose { }
            }
            CozyBlocksTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(LocalCrystalAds provides ads, LocalPremium provides premium) {
                    val free = premium.ownership == Ownership.FREE
                    val loadAds = free && ads.ready
                    LaunchedEffect(loadAds) {
                        if (loadAds) ads.preloadInterstitials()
                    }
                    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))) {
                        Box(Modifier.fillMaxWidth().weight(1f)) { CozyBlocksApp(settings = settings) }
                        if (premium.ownership != Ownership.PREMIUM) BottomBanner(loadAds)
                    }
                }
            }
        }
        ads.updateConsent()
    }

    override fun onResume() {
        super.onResume()
        premium.refresh()
    }

    override fun onDestroy() {
        premium.close()
        super.onDestroy()
    }
}

package com.hb.puzz

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.android.billingclient.api.*

internal val LocalPremium = staticCompositionLocalOf<PremiumBilling?> { null }
internal enum class Ownership { CHECKING, FREE, PREMIUM }

internal class PremiumBilling(private val activity: Activity) : PurchasesUpdatedListener {
    companion object {
        const val PRODUCT = "picabloom_premium"
        private const val OK = BillingClient.BillingResponseCode.OK
    }

    private val cache = activity.getSharedPreferences("play_entitlement", Context.MODE_PRIVATE)
    var ownership by mutableStateOf(if (cache.getBoolean(PRODUCT, false)) Ownership.PREMIUM else Ownership.CHECKING)
        private set
    private var product by mutableStateOf<ProductDetails?>(null)
    private val buyOffer get() = product?.oneTimePurchaseOfferDetailsList?.firstOrNull()
        ?: product?.oneTimePurchaseOfferDetails
    val price get() = buyOffer?.formattedPrice
    var message by mutableStateOf("")
        private set
    var pending by mutableStateOf(false)
        private set
    var launching by mutableStateOf(false)
        private set
    private var generation = 0
    private var connecting = false
    private var closed = false
    private val main = Handler(Looper.getMainLooper())
    private val retry = Runnable { refresh() }
    private val acknowledging = mutableSetOf<String>()
    private val client = BillingClient.newBuilder(activity.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private fun onMain(action: () -> Unit) = activity.runOnUiThread { if (!closed) action() }

    private fun retryLater() {
        main.removeCallbacks(retry)
        main.postDelayed(retry, 60_000)
    }

    fun refresh() {
        if (closed || connecting) return
        if (client.isReady) {
            queryPlay()
            return
        }
        connecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) = onMain {
                connecting = false
                if (result.responseCode == OK) queryPlay() else unavailable()
            }
            override fun onBillingServiceDisconnected() = onMain {
                connecting = false
                unavailable()
            }
        })
    }

    private fun unavailable() {
        product = null
        message = "Google Play is unavailable. Try Restore purchases again later."
        retryLater()
    }

    private fun queryPlay() {
        val request = ++generation
        val type = BillingClient.ProductType.INAPP
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build()) { result, purchases ->
            onMain {
                if (request != generation) return@onMain
                if (result.responseCode == OK) {
                    val matching = purchases.filter { PRODUCT in it.products }
                    val owned = matching.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    setOwned(owned.isNotEmpty())
                    pending = owned.isEmpty() && matching.any { it.purchaseState == Purchase.PurchaseState.PENDING }
                    owned.forEach(::acknowledge)
                } else unavailable()
            }
        }
        val item = QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT).setProductType(type).build()
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(item)).build()) { result, details ->
            onMain {
                if (request != generation) return@onMain
                product = details.productDetailsList.firstOrNull { it.productId == PRODUCT && result.responseCode == OK }
                message = if (buyOffer == null) "Premium is currently unavailable in Google Play." else ""
            }
        }
    }

    fun buy() {
        val details = product ?: return
        if (ownership != Ownership.FREE || pending || launching) return
        val offer = buyOffer ?: return
        val item = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details)
        offer.offerToken?.let { if (it.isNotEmpty()) item.setOfferToken(it) }
        launching = true
        val result = client.launchBillingFlow(activity, BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(item.build())).build())
        if (result.responseCode != OK) {
            launching = false
            message = "Purchase could not start. Try again in Google Play."
            refresh()
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) = onMain {
        launching = false
        if (result.responseCode == OK) {
            val matching = purchases.orEmpty().filter { PRODUCT in it.products }
            matching.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach {
                ++generation
                setOwned(true)
                pending = false
                acknowledge(it)
            }
            if (matching.any { it.purchaseState == Purchase.PurchaseState.PENDING }) pending = true
        }
        refresh()
    }

    private fun setOwned(owned: Boolean) {
        cache.edit().putBoolean(PRODUCT, owned).apply()
        ownership = if (owned) Ownership.PREMIUM else Ownership.FREE
    }

    private fun acknowledge(purchase: Purchase) {
        if (purchase.isAcknowledged || !acknowledging.add(purchase.purchaseToken)) return
        client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) {
            result -> onMain {
                acknowledging.remove(purchase.purchaseToken)
                if (result.responseCode != OK) retryLater()
            }
        }
    }

    fun close() {
        closed = true
        main.removeCallbacksAndMessages(null)
        client.endConnection()
    }
}

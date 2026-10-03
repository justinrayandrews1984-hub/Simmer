package app.simmer.ads

import android.app.Activity
import android.content.Context
import app.simmer.Config
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Loads and shows one rewarded ad at a time. Uses Google's test ids until Config is updated.
 */
object AdsManager {
    private var loaded: RewardedAd? = null
    private var loading = false
    @Volatile var ready: Boolean = false
        private set

    fun init(context: Context) {
        MobileAds.initialize(context.applicationContext) { preload(context.applicationContext) }
    }

    fun preload(context: Context) {
        if (loaded != null || loading) return
        loading = true
        RewardedAd.load(
            context.applicationContext,
            Config.ADMOB_REWARDED_UNIT,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { loaded = ad; loading = false; ready = true }
                override fun onAdFailedToLoad(error: LoadAdError) { loaded = null; loading = false; ready = false }
            },
        )
    }

    /**
     * Shows the ad. [onRewarded] fires only if the viewer watched enough to earn the reward.
     * Returns false if no ad is ready yet (and starts loading one).
     */
    fun show(activity: Activity, onRewarded: () -> Unit, onClosed: () -> Unit = {}): Boolean {
        val ad = loaded ?: run { preload(activity); return false }
        loaded = null; ready = false
        ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { preload(activity); onClosed() }
            override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) { preload(activity); onClosed() }
        }
        ad.show(activity) { onRewarded() }
        return true
    }
}

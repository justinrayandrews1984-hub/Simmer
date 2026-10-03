package app.simmer

/**
 * Everything you'd change when going live is here, in one place.
 */
object Config {
    /** The charity named throughout the app. */
    const val CHARITY_NAME = "Greater Vancouver Food Bank"
    const val CHARITY_URL = "https://foodbank.bc.ca"
    /** Where the "Tip" buttons send people until Play Billing is set up. */
    const val DONATE_URL = "https://foodbank.bc.ca/donate/"

    /** How many meals one dollar funds, as the charity states it. Keep this conservative. */
    const val MEALS_PER_DOLLAR = 3

    /** Meals credited for one rewarded ad watch. Keep this below what the ad actually pays. */
    const val MEALS_PER_AD = 1
    /** How many rewarded ads one person can watch per day. */
    const val MAX_ADS_PER_DAY = 5

    /**
     * AdMob IDs. These are Google's official TEST ids: ads show, nobody gets paid.
     * Replace with your own from admob.google.com when the app is on the Play Store.
     */
    const val ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val ADMOB_REWARDED_UNIT = "ca-app-pub-3940256099942544/5224354917"

    /** Shown on the Impact screen. Update when you make a transfer. */
    const val COMMUNITY_GOAL_MEALS = 10_000
    const val COMMUNITY_MEALS_SO_FAR = 0
    const val LAST_TRANSFER_NOTE = "No transfers yet. First one goes out when the pot reaches \$100."

    const val APP_URL = "https://github.com/justinrayandrews1984-hub/Simmer"
}

package com.tokenmonitor.mobile.data

import android.content.Context
import android.content.SharedPreferences

data class SettingsSnapshot(
    val hubUrl: String = "",
    val secret: String = "",
    val refreshMs: Long = 60_000L,
    val currency: String = "USD",
    val currencyRate: Double? = null, // custom rate override; null = built-in default
    val darkTheme: Boolean = true,
    // "solid" | "wallpaper" (follow system wallpaper) | "custom" (user-picked image)
    val backgroundMode: String = "solid",
    val backgroundImageUri: String = "",
    // Show providers that report no quota windows at all (e.g. not configured).
    val showEmptyLimitProviders: Boolean = false
)

class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hub_settings", Context.MODE_PRIVATE)

    fun snapshot(): SettingsSnapshot = SettingsSnapshot(
        hubUrl = prefs.getString(KEY_URL, "") ?: "",
        secret = prefs.getString(KEY_SECRET, "") ?: "",
        refreshMs = prefs.getLong(KEY_REFRESH, 60_000L),
        currency = prefs.getString(KEY_CURRENCY, "USD") ?: "USD",
        currencyRate = if (prefs.contains(KEY_RATE)) prefs.getFloat(KEY_RATE, 0f).toDouble() else null,
        darkTheme = prefs.getBoolean(KEY_THEME, true),
        backgroundMode = prefs.getString(KEY_BG_MODE, "solid") ?: "solid",
        backgroundImageUri = prefs.getString(KEY_BG_URI, "") ?: "",
        showEmptyLimitProviders = prefs.getBoolean(KEY_EMPTY_LIMITS, false)
    )

    fun save(snapshot: SettingsSnapshot) {
        prefs.edit()
            .putString(KEY_URL, snapshot.hubUrl.trim())
            .putString(KEY_SECRET, snapshot.secret.filterNot { it.isWhitespace() })
            .putLong(KEY_REFRESH, snapshot.refreshMs)
            .putString(KEY_CURRENCY, snapshot.currency)
            .putBoolean(KEY_THEME, snapshot.darkTheme)
            .putString(KEY_BG_MODE, snapshot.backgroundMode)
            .putString(KEY_BG_URI, snapshot.backgroundImageUri)
            .putBoolean(KEY_EMPTY_LIMITS, snapshot.showEmptyLimitProviders)
            .apply {
                if (snapshot.currencyRate != null) putFloat(KEY_RATE, snapshot.currencyRate.toFloat())
                else remove(KEY_RATE)
            }
            .apply()
    }

    companion object {
        private const val KEY_URL = "hubUrl"
        private const val KEY_SECRET = "secret"
        private const val KEY_REFRESH = "refreshMs"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_RATE = "currencyRate"
        private const val KEY_THEME = "darkTheme"
        private const val KEY_BG_MODE = "backgroundMode"
        private const val KEY_BG_URI = "backgroundImageUri"
        private const val KEY_EMPTY_LIMITS = "showEmptyLimitProviders"
    }
}

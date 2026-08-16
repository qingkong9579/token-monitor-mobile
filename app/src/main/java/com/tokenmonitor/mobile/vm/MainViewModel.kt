package com.tokenmonitor.mobile.vm

import android.app.Application
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tokenmonitor.mobile.data.ApiJson
import com.tokenmonitor.mobile.data.HubApi
import com.tokenmonitor.mobile.data.SettingsSnapshot
import com.tokenmonitor.mobile.data.SettingsStore
import com.tokenmonitor.mobile.data.StatsCache
import com.tokenmonitor.mobile.data.StatsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Period(val wireName: String, val label: String) {
    TODAY("today", "今天"),
    MONTH("month", "本月"),
    ALL_TIME("allTime", "累计")
}

data class UiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val stats: StatsResponse? = null,
    val lastUpdated: Long? = null,
    val settings: SettingsSnapshot = SettingsSnapshot(),
    val period: Period = Period.TODAY,
    val testing: Boolean = false,
    val testResult: String? = null,
    /** Decoded background image (wallpaper or custom), null when solid color. */
    val backgroundBitmap: ImageBitmap? = null
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val appContext = app
    private val settingsStore = SettingsStore(app)
    private val statsCache = StatsCache(app)
    private val api = HubApi(HubApi.defaultClient())

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var refreshJob: Job? = null
    private var inFlight = false

    init {
        val snapshot = settingsStore.snapshot()
        // Decode the bundled default background synchronously so the first frame
        // shows the image immediately instead of a brief aurora-gradient flash.
        // Wallpaper / custom backgrounds stay async — they need slow I/O.
        val initialBackground =
            if (snapshot.backgroundMode == "solid") loadBackgroundBitmap(snapshot) else null
        _state.update {
            it.copy(
                settings = snapshot,
                backgroundBitmap = initialBackground,
                loading = snapshot.hubUrl.isBlank()
            )
        }
        // Show the last cached stats immediately (offline-first); a successful
        // refresh below replaces it and re-caches.
        loadCachedStats()
        startAutoRefresh()
        refresh()
        if (snapshot.backgroundMode != "solid") reloadBackground(snapshot)
    }

    fun setPeriod(period: Period) {
        _state.update { it.copy(period = period) }
    }

    fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                val interval = _state.value.settings.refreshMs.coerceAtLeast(15_000L)
                delay(interval)
                refresh()
            }
        }
    }

    fun refresh(manual: Boolean = false) {
        if (inFlight) return
        inFlight = true
        viewModelScope.launch {
            val snapshot = settingsStore.snapshot()
            _state.update { it.copy(refreshing = true, error = null) }
            try {
                if (snapshot.hubUrl.isBlank()) {
                    _state.update {
                        it.copy(
                            refreshing = false,
                            loading = false,
                            stats = null,
                            error = "未配置 Hub URL — 请打开设置填写云端 hub 地址"
                        )
                    }
                    return@launch
                }
                val fetched = api.fetchStats(snapshot.hubUrl, snapshot.secret)
                _state.update {
                    it.copy(
                        refreshing = false,
                        loading = false,
                        stats = fetched.stats,
                        lastUpdated = System.currentTimeMillis(),
                        error = null
                    )
                }
                // Cache the latest successful response so the next launch can
                // show data immediately even when the hub is unreachable.
                withContext(Dispatchers.IO) {
                    statsCache.save(fetched.rawJson, System.currentTimeMillis())
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        refreshing = false,
                        loading = false,
                        error = e.message ?: "网络错误"
                    )
                }
            } finally {
                inFlight = false
            }
        }
    }

    fun saveSettings(snapshot: SettingsSnapshot) {
        settingsStore.save(snapshot)
        _state.update { it.copy(settings = snapshot) }
        startAutoRefresh()
        refresh()
        reloadBackground(snapshot)
    }

    fun testConnection(url: String, secret: String) {
        if (_state.value.testing) return
        _state.update { it.copy(testing = true, testResult = null) }
        viewModelScope.launch {
            val result = try {
                val stats = api.fetchStats(url, secret).stats
                val deviceCount = stats.devices?.size ?: 0
                val today = stats.periods?.today?.totalTokens ?: 0
                "连接成功 · ${deviceCount} 台设备 · 今日 ${today} tokens"
            } catch (e: Exception) {
                "连接失败: ${e.message}"
            }
            _state.update { it.copy(testing = false, testResult = result) }
        }
    }

    fun setShowEmptyLimitProviders(show: Boolean) {
        val snapshot = _state.value.settings.copy(showEmptyLimitProviders = show)
        settingsStore.save(snapshot)
        _state.update { it.copy(settings = snapshot) }
    }

    /**
     * Applies a background mode/uri immediately when a settings chip is tapped
     * (preview), so the wallpaper / custom image visibly takes effect without
     * requiring Save. Nothing is persisted here — Save persists the real snapshot.
     */
    fun previewBackground(mode: String, uri: String?) {
        val settings = _state.value.settings
        val updated = settings.copy(
            backgroundMode = mode,
            backgroundImageUri = uri ?: settings.backgroundImageUri
        )
        _state.update { it.copy(settings = updated) }
        reloadBackground(updated)
    }

    /**
     * Applies a settings change immediately (in-memory preview) without
     * persisting — used by the theme and currency toggles so they take effect
     * the moment a chip is tapped. Save persists the real snapshot.
     */
    fun previewSettings(transform: (SettingsSnapshot) -> SettingsSnapshot) {
        _state.update { it.copy(settings = transform(it.settings)) }
    }

    /** Loads the last cached stats response so the UI has data immediately. */
    private fun loadCachedStats() {
        val cached = statsCache.load() ?: return
        try {
            val stats = ApiJson.decodeFromString(StatsResponse.serializer(), cached.first)
            _state.update { it.copy(stats = stats, lastUpdated = cached.second.takeIf { it > 0 }) }
        } catch (_: Exception) {
            // Corrupt cache: ignore and fall back to the live fetch.
        }
    }

    /** Loads the wallpaper or custom image on a background thread. */
    private fun reloadBackground(snapshot: SettingsSnapshot) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) { loadBackgroundBitmap(snapshot) }
            _state.update { it.copy(backgroundBitmap = bitmap) }
        }
    }

    private fun loadBackgroundBitmap(snapshot: SettingsSnapshot): ImageBitmap? {
        return try {
            when (snapshot.backgroundMode) {
                "solid" -> {
                    // Bundled default background image (assets/bg_default.jpg).
                    val stream = appContext.assets.open("bg_default.jpg")
                    val bmp = try {
                        BitmapFactory.decodeStream(stream)
                    } finally {
                        stream.close()
                    } ?: return null
                    bmp.asImageBitmap()
                }
                "wallpaper" -> {
                    val wm = WallpaperManager.getInstance(appContext)
                    val drawable = wm.drawable ?: wm.fastDrawable
                    if (drawable != null) {
                        try {
                            val (w, h) = scaledSize(
                                drawable.intrinsicWidth.coerceAtLeast(1),
                                drawable.intrinsicHeight.coerceAtLeast(1)
                            )
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bmp)
                            drawable.setBounds(0, 0, w, h)
                            drawable.draw(canvas)
                            bmp.asImageBitmap()
                        } catch (e: Exception) {
                            Log.w("TokenMonitor", "wallpaper drawable draw failed: ${e.message}")
                            null
                        }
                    } else {
                        // Live / undrawable wallpaper: decode the system wallpaper
                        // file directly, capped at the same 1440px long edge.
                        val pfd = runCatching { wm.getWallpaperFile(WallpaperManager.FLAG_SYSTEM) }
                            .getOrNull()
                        if (pfd == null) {
                            Log.w("TokenMonitor", "wallpaper: drawable null and getWallpaperFile null")
                            return null
                        }
                        try {
                            val bmp = pfd.use {
                                BitmapFactory.decodeFileDescriptor(it.fileDescriptor)
                            } ?: return null
                            val (w, h) = scaledSize(bmp.width, bmp.height)
                            val scaled =
                                if (bmp.width > w || bmp.height > h) Bitmap.createScaledBitmap(bmp, w, h, true)
                                else bmp
                            if (scaled != bmp) bmp.recycle()
                            scaled.asImageBitmap()
                        } catch (e: Exception) {
                            Log.w("TokenMonitor", "wallpaper file decode failed: ${e.message}")
                            null
                        }
                    }
                }
                "custom" -> {
                    val uri = snapshot.backgroundImageUri
                    if (uri.isBlank()) return null
                    decodeSampled(uri)?.asImageBitmap()
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Two-pass decode capped at ~1440px on the long edge (full-res images
     *  otherwise cost tens of MB in memory for a blurred background). */
    private fun decodeSampled(uri: String): Bitmap? {
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(Uri.parse(uri))?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val (w, h) = scaledSize(bounds.outWidth, bounds.outHeight)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, w, h)
        }
        return resolver.openInputStream(Uri.parse(uri))?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }

    private fun scaledSize(w: Int, h: Int): Pair<Int, Int> {
        if (w <= 0 || h <= 0) return 1440 to 1440
        val maxDim = 1440
        val scale = maxOf(w, h).toFloat() / maxDim
        return if (scale <= 1f) w to h
        else (w / scale).toInt().coerceAtLeast(1) to (h / scale).toInt().coerceAtLeast(1)
    }

    private fun calculateInSampleSize(w: Int, h: Int, reqW: Int, reqH: Int): Int {
        var sample = 1
        if (w > 0 && h > 0) {
            var width = w
            var height = h
            while (width / 2 >= reqW && height / 2 >= reqH) {
                width /= 2
                height /= 2
                sample *= 2
            }
        }
        return sample
    }
}

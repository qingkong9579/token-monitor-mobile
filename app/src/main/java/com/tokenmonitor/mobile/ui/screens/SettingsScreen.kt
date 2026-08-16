package com.tokenmonitor.mobile.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokenmonitor.mobile.data.SettingsSnapshot
import com.tokenmonitor.mobile.ui.components.GlassButton
import com.tokenmonitor.mobile.ui.components.GlassChip
import com.tokenmonitor.mobile.ui.components.SectionTitle
import com.tokenmonitor.mobile.ui.theme.Accent
import com.tokenmonitor.mobile.ui.theme.AccentOn
import com.tokenmonitor.mobile.ui.theme.Error
import com.tokenmonitor.mobile.ui.theme.Success
import com.tokenmonitor.mobile.ui.theme.TextMuted
import com.tokenmonitor.mobile.ui.theme.TextPrimary
import com.tokenmonitor.mobile.util.CURRENCY_CODES
import com.tokenmonitor.mobile.util.moneySymbol
import com.tokenmonitor.mobile.vm.UiState
import kotlinx.coroutines.launch

private val REFRESH_OPTIONS = listOf(
    15_000L to "15 秒",
    30_000L to "30 秒",
    60_000L to "1 分钟",
    300_000L to "5 分钟"
)

/** The runtime permission needed to read the system wallpaper for the background. */
private fun wallpaperPermission(): String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES
    else Manifest.permission.READ_EXTERNAL_STORAGE

@Composable
fun SettingsScreen(
    state: UiState,
    onSave: (SettingsSnapshot) -> Unit,
    onTest: (String, String) -> Unit,
    onPreviewBackground: (String, String?) -> Unit,
    onPreviewSettings: ((SettingsSnapshot) -> SettingsSnapshot) -> Unit
) {
    var url by remember { mutableStateOf(state.settings.hubUrl) }
    var secret by remember { mutableStateOf(state.settings.secret) }
    var refreshMs by remember { mutableStateOf(state.settings.refreshMs) }
    var currency by remember { mutableStateOf(state.settings.currency) }
    var rate by remember { mutableStateOf(state.settings.currencyRate?.toString() ?: "") }
    var dark by remember { mutableStateOf(state.settings.darkTheme) }
    var backgroundMode by remember { mutableStateOf(state.settings.backgroundMode) }
    var backgroundUri by remember { mutableStateOf(state.settings.backgroundImageUri) }
    var showEmptyLimits by remember { mutableStateOf(state.settings.showEmptyLimitProviders) }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val wallpaperPermGranted = ContextCompat.checkSelfPermission(
        context, wallpaperPermission()
    ) == PackageManager.PERMISSION_GRANTED
    val wallpaperPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            backgroundMode = "wallpaper"
            onPreviewBackground("wallpaper", null)
        }
    }
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            backgroundUri = uri.toString()
            backgroundMode = "custom"
            onPreviewBackground("custom", uri.toString())
            // Persist read access across app restarts (best effort).
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Session-only access is fine for one-time use.
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        SectionTitle("Hub 连接", "与云端 hub 通信")
        Text(
            "填写你部署的 Token Monitor hub 地址与共享密钥。Secret 为必填项,用于 Bearer 鉴权。",
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Hub URL(必填)") },
            placeholder = { Text("https://hub.example.com") },
            supportingText = { Text("示例:https://hub.example.com 或 http://192.168.x.x:17321") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        OutlinedTextField(
            value = secret,
            onValueChange = { secret = it },
            label = { Text("Secret(共享密钥,必填)") },
            placeholder = { Text("粘贴 hub 的 TOKEN_MONITOR_SECRET") },
            supportingText = { Text("必填项,用于 Bearer 鉴权访问 /api/stats") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassButton(
                onClick = { onTest(url, secret) },
                enabled = !state.testing,
                tint = Accent.copy(0.5f)
            ) {
                if (state.testing) {
                    CircularProgressIndicator(
                        Modifier.width(16.dp).height(16.dp),
                        strokeWidth = 2.dp,
                        color = AccentOn
                    )
                }
                Text("测试连接", color = AccentOn, fontWeight = FontWeight.Medium)
            }
            GlassButton(
                onClick = {
                    onSave(
                        SettingsSnapshot(
                            hubUrl = url,
                            secret = secret,
                            refreshMs = refreshMs,
                            currency = currency,
                            currencyRate = rate.toDoubleOrNull(),
                            darkTheme = dark,
                            backgroundMode = backgroundMode,
                            backgroundImageUri = backgroundUri,
                            showEmptyLimitProviders = showEmptyLimits
                        )
                    )
                    scope.launch { snackbarHostState.showSnackbar("已保存并应用") }
                },
                tint = Accent.copy(0.5f)
            ) {
                Text("保存并应用", color = AccentOn, fontWeight = FontWeight.Medium)
            }
        }
        state.testResult?.let {
            Text(
                it,
                fontSize = 12.sp,
                color = if (it.startsWith("连接成功")) Success else Error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        SectionTitle("刷新间隔")
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            REFRESH_OPTIONS.forEach { (ms, label) ->
                GlassChip(
                    label = label,
                    selected = refreshMs == ms,
                    onClick = { refreshMs = ms }
                )
            }
        }

        SectionTitle("货币", "costUsd 按汇率换算显示")
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CURRENCY_CODES.forEach { c ->
                GlassChip(
                    label = "${moneySymbol(c)} $c",
                    selected = currency == c,
                    onClick = {
                        currency = c
                        onPreviewSettings { it.copy(currency = c) }
                    }
                )
            }
        }
        OutlinedTextField(
            value = rate,
            onValueChange = { rate = it },
            label = { Text("自定义汇率(1 USD = ?;留空用内置默认)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        SectionTitle("背景", "玻璃卡片会模糊背景图像")
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GlassChip(
                label = "默认图片",
                selected = backgroundMode == "solid",
                onClick = {
                    backgroundMode = "solid"
                    onPreviewBackground("solid", null)
                }
            )
            GlassChip(
                label = "跟随系统壁纸",
                selected = backgroundMode == "wallpaper",
                onClick = {
                    if (wallpaperPermGranted) {
                        backgroundMode = "wallpaper"
                        onPreviewBackground("wallpaper", null)
                    } else {
                        // Reading the wallpaper needs a permission; request it and
                        // preview on grant.
                        wallpaperPermissionLauncher.launch(wallpaperPermission())
                    }
                }
            )
            GlassChip(
                label = "自选图片",
                selected = backgroundMode == "custom",
                onClick = {
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
            if (backgroundMode == "custom" && backgroundUri.isNotBlank()) {
                GlassButton(onClick = {
                    backgroundUri = ""
                    backgroundMode = "solid"
                    onPreviewBackground("solid", null)
                }) {
                    Text("清除图片", fontSize = 11.sp, color = TextPrimary)
                }
            }
        }
        Text(
            when (backgroundMode) {
                "wallpaper" -> "使用当前系统桌面壁纸作为应用背景(Android 12+ 玻璃效果最佳)"
                "custom" -> if (backgroundUri.isNotBlank()) "已选择自定义图片" else "从相册选择一张图片"
                else -> "使用内置默认背景图片"
            },
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        SectionTitle("额度显示")
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GlassChip(
                label = "显示无额度窗口的 provider",
                selected = showEmptyLimits,
                onClick = { showEmptyLimits = !showEmptyLimits }
            )
        }
        Text(
            "默认隐藏未配置/无额度数据的 provider(如 antigravity、codex)。",
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        SectionTitle("外观")
        Row(Modifier.padding(horizontal = 16.dp)) {
            GlassChip(
                label = "深色",
                selected = dark,
                onClick = {
                    dark = true
                    onPreviewSettings { it.copy(darkTheme = true) }
                }
            )
            Spacer(Modifier.width(8.dp))
            GlassChip(
                label = "浅色",
                selected = !dark,
                onClick = {
                    dark = false
                    onPreviewSettings { it.copy(darkTheme = false) }
                }
            )
        }

        SectionTitle("关于")
        Text(
            "Token Monitor Mobile · 数据来自 Token Monitor hub(API 协议 docs/API.md)。\n桌面端采集 → hub 聚合 → 本应用只读展示。",
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        AboutLinkRow("本项目 GitHub", "https://github.com/qingkong9579/token-monitor-mobile")
        AboutLinkRow("参考项目 token-monitor", "https://github.com/Javis603/token-monitor")
        Spacer(Modifier.height(120.dp))
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter)
    )
    }
}

/** A clickable row that opens [url] in the browser. */
@Composable
private fun AboutLinkRow(label: String, url: String) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (_: Exception) {
                }
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(
            url,
            fontSize = 11.sp,
            color = Accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

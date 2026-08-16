package com.tokenmonitor.mobile.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.tokenmonitor.mobile.ui.components.PillState
import com.tokenmonitor.mobile.ui.components.SettingsCircleButton
import com.tokenmonitor.mobile.ui.components.StatusPill
import com.tokenmonitor.mobile.ui.liquid.GlassCard
import com.tokenmonitor.mobile.ui.liquid.LocalGlassBackdrop
import com.tokenmonitor.mobile.ui.screens.DevicesScreen
import com.tokenmonitor.mobile.ui.screens.HomeScreen
import com.tokenmonitor.mobile.ui.screens.LimitsScreen
import com.tokenmonitor.mobile.ui.screens.ModelsScreen
import com.tokenmonitor.mobile.ui.screens.ProjectsScreen
import com.tokenmonitor.mobile.ui.screens.SessionsScreen
import com.tokenmonitor.mobile.ui.screens.SettingsScreen
import com.tokenmonitor.mobile.ui.screens.ToolsScreen
import com.tokenmonitor.mobile.ui.screens.TrendsScreen
import com.tokenmonitor.mobile.ui.theme.Accent
import com.tokenmonitor.mobile.ui.theme.AccentOn
import com.tokenmonitor.mobile.ui.theme.AuroraBackground
import com.tokenmonitor.mobile.ui.theme.DarkAuroraColors
import com.tokenmonitor.mobile.ui.theme.DividerColor
import com.tokenmonitor.mobile.ui.theme.LightAuroraColors
import com.tokenmonitor.mobile.ui.theme.TextMuted
import com.tokenmonitor.mobile.ui.theme.TextPrimary
import com.tokenmonitor.mobile.vm.MainViewModel
import com.tokenmonitor.mobile.vm.UiState

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("首页", Icons.Filled.Home),
    TOOLS("工具", Icons.Filled.BarChart),
    LIMITS("额度", Icons.Filled.DonutLarge),
    MODELS("模型", Icons.Filled.Layers),
    DEVICES("设备", Icons.Filled.Devices),
    PROJECTS("项目", Icons.Filled.Folder),
    SESSIONS("会话", Icons.AutoMirrored.Filled.Chat),
    TRENDS("趋势", Icons.AutoMirrored.Filled.TrendingUp)
}

/** Phone-first layouts are centered within this width on larger screens. */
private const val MAX_CONTENT_WIDTH = 640

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {
    val state = vm.state.collectAsState().value
    var tab by remember { mutableStateOf(Tab.HOME) }
    // First run: no hub configured yet — open Settings so the user can fill in
    // the Hub URL and secret right away.
    var showSettings by remember { mutableStateOf(vm.state.value.settings.hubUrl.isBlank()) }
    // The pull-up view selector (opened by the right zone of the switcher pill).
    var switcherSheetOpen by remember { mutableStateOf(false) }

    // Wallpaper / custom / bundled-default image backdrop: glass cards blur it.
    val backgroundBackdrop = rememberLayerBackdrop()
    val useBackground = state.backgroundBitmap != null

    // Aurora backdrop: in solid mode the glass cards blur a snapshot of the
    // aurora gradient, so every pane reads as frosted glass over the night sky.
    val darkTheme = state.settings.darkTheme
    val auroraBackdrop = androidx.compose.runtime.key(darkTheme) {
        val aurora = if (darkTheme) DarkAuroraColors else LightAuroraColors
        rememberCanvasBackdrop {
            drawRect(
                Brush.linearGradient(
                    colors = aurora,
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
            )
        }
    }
    val glassBackdrop = if (useBackground) backgroundBackdrop else auroraBackdrop

    // System back closes Settings instead of exiting the app.
    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    CompositionLocalProvider(LocalGlassBackdrop provides glassBackdrop) {
        Box(Modifier.fillMaxSize()) {
            if (useBackground) {
                // Background image under everything: glass cards blur this layer.
                Box(
                    Modifier
                        .fillMaxSize()
                        .layerBackdrop(backgroundBackdrop)
                ) {
                    Image(
                        bitmap = state.backgroundBitmap!!,
                        contentDescription = "背景",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                if (darkTheme) Color.Black.copy(0.35f)
                                else Color.Black.copy(0.12f)
                            )
                    )
                }
            } else {
                // Solid mode: the living aurora ground the glass panels float over.
                AuroraBackground(isDark = darkTheme, modifier = Modifier.fillMaxSize())
            }

            Column(Modifier.fillMaxSize()) {
                TopBar(showSettings = showSettings, onBack = { showSettings = false }, state = state)
                // Responsive: center the screen content in a max-width column so
                // tablets don't stretch the phone-first layout edge-to-edge.
                Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .widthIn(max = MAX_CONTENT_WIDTH.dp)
                    ) {
                        when {
                        showSettings -> SettingsScreen(
                            state = state,
                            onSave = { vm.saveSettings(it) },
                            onTest = { url, secret -> vm.testConnection(url, secret) },
                            onPreviewBackground = { mode, uri -> vm.previewBackground(mode, uri) },
                            onPreviewSettings = { transform -> vm.previewSettings(transform) }
                        )
                        tab == Tab.HOME -> HomeScreen(
                            state,
                            onPeriod = vm::setPeriod,
                            onRefresh = { vm.refresh(true) },
                            onOpenSettings = { showSettings = true }
                        )
                        tab == Tab.TOOLS -> ToolsScreen(state, onPeriod = vm::setPeriod, onRefresh = { vm.refresh(true) })
                        tab == Tab.LIMITS -> LimitsScreen(state, onRefresh = { vm.refresh(true) })
                        tab == Tab.MODELS -> ModelsScreen(state, onPeriod = vm::setPeriod, onRefresh = { vm.refresh(true) })
                        tab == Tab.DEVICES -> DevicesScreen(state, onRefresh = { vm.refresh(true) })
                        tab == Tab.PROJECTS -> ProjectsScreen(state, onPeriod = vm::setPeriod, onRefresh = { vm.refresh(true) })
                        tab == Tab.SESSIONS -> SessionsScreen(state, onPeriod = vm::setPeriod, onRefresh = { vm.refresh(true) })
                        else -> TrendsScreen(state, onRefresh = { vm.refresh(true) })
                        }
                    }
                }
            }

            if (!showSettings) {
                BottomNavBar(
                    current = tab,
                    onCycle = { tab = Tab.entries[(tab.ordinal + 1) % Tab.entries.size] },
                    onOpenSelector = { switcherSheetOpen = true },
                    onOpenSettings = { showSettings = true },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }

    if (switcherSheetOpen && !showSettings) {
        ModalBottomSheet(
            onDismissRequest = { switcherSheetOpen = false },
            containerColor = Color.Transparent,
            contentColor = TextPrimary,
            dragHandle = null
        ) {
            // The sheet is a separate window, so it does not inherit the app's
            // glass backdrop — re-provide it so the rows blur the aurora instead
            // of falling back to a flat purple fill.
            CompositionLocalProvider(LocalGlassBackdrop provides glassBackdrop) {
                // Scrollable, and the last card sits one card-gap (8dp) above the
                // navigation bar instead of being clipped at the sheet's bottom.
                Column(
                    Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Tab.entries.forEach { t ->
                        ViewSwitcherRow(
                            t,
                            selected = tab == t,
                            onClick = {
                                tab = t
                                switcherSheetOpen = false
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * The desktop's title bar on mobile: the Σ mark in a frosted tile, the app
 * name, then the hub connection status — or the settings header when open.
 */
@Composable
private fun TopBar(showSettings: Boolean, onBack: () -> Unit, state: UiState) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showSettings) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = TextPrimary)
            }
            Text("设置", color = TextPrimary, fontWeight = FontWeight.SemiBold)
        } else {
            // The Σ mark (token-monitor's logo) in a small frosted tile.
            GlassCard(
                shape = com.kyant.shapes.RoundedRectangle(10f.dp),
                contentPadding = 0.dp
            ) {
                Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                    Text("Σ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Accent)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text("Token Monitor", fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.width(10.dp))
            HubStatusPill(state)
        }
    }
}

/**
 * Bottom navigation, token-monitor style: a single glass view-switcher pill
 * (left) and the circular glass settings button (right).
 */
@Composable
private fun BottomNavBar(
    current: Tab,
    onCycle: () -> Unit,
    onOpenSelector: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 10.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ViewSwitcherPill(
            current = current,
            onCycle = onCycle,
            onOpenSelector = onOpenSelector,
            modifier = Modifier.weight(1f)
        )
        SettingsCircleButton(onClick = onOpenSettings)
    }
}

/**
 * The desktop's view switcher carried into one liquid-glass rounded rectangle:
 * the left zone cycles to the next view in the list; the right zone pulls up
 * the selector sheet.
 */
@Composable
private fun ViewSwitcherPill(
    current: Tab,
    onCycle: () -> Unit,
    onOpenSelector: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.height(50.dp),
        shape = com.kyant.shapes.RoundedRectangle(16f.dp),
        contentPadding = 0.dp,
        tint = Accent.copy(0.25f)
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .weight(3f)
                    .fillMaxHeight()
                    .clickable(onClick = onCycle)
                    .padding(start = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(current.icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                Text(current.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
            Box(
                Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(DividerColor)
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(onClick = onOpenSelector),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "选择视图", tint = TextMuted)
            }
        }
    }
}

/** A frosted-glass rounded row inside the pull-up view selector sheet. */
@Composable
private fun ViewSwitcherRow(t: Tab, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) Accent.copy(0.5f) else null
    val fg = if (selected) AccentOn else TextPrimary
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = com.kyant.shapes.RoundedRectangle(12f.dp),
        contentPadding = 0.dp,
        tint = tint
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(t.icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Text(
                t.label,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = fg
            )
            Spacer(Modifier.weight(1f))
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = "当前", tint = AccentOn, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun HubStatusPill(state: UiState) {
    val pill = when {
        state.settings.hubUrl.isBlank() -> PillState.OFFLINE to "未配置"
        state.refreshing -> PillState.WARN to "同步中"
        state.error != null -> PillState.ERROR to "离线"
        state.stats != null -> PillState.LIVE to "Live"
        else -> PillState.OFFLINE to "未连接"
    }
    StatusPill(pill.first, pill.second)
}

/*
 * Adapted from AndroidLiquidGlass (https://github.com/Kyant0/AndroidLiquidGlass)
 * Licensed under the Apache License, Version 2.0.
 */
package com.tokenmonitor.mobile.ui.liquid

import androidx.compose.runtime.withFrameNanos

/** Android implementation of the catalog's expect declaration. */
internal suspend fun awaitFrame(): Long = withFrameNanos { it }

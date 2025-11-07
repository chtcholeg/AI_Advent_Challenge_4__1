package ru.chtcholeg.aichat.utils

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun Modifier.consumedInsetsModifier(isBottomSheet: Boolean) = if (!isBottomSheet) {
    imePadding()
        .statusBarsPadding()
        .navigationBarsPadding()
} else {
    consumeWindowInsets(
        WindowInsets.ime
            .union(WindowInsets.statusBars)
            .union(WindowInsets.navigationBars)
    )
}
package com.fitnessmarket.myworkerlodger.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect

/** Reloads a screen's data each time it comes back into view, so changes made elsewhere show up. */
@Composable
fun RefreshOnResume(refresh: () -> Unit) {
    LifecycleResumeEffect(Unit) {
        refresh()
        onPauseOrDispose {}
    }
}

/** Shows [message] centred on the screen when it isn't null, and [content] otherwise. */
@Composable
fun ContentOrMessage(
    innerPadding: PaddingValues,
    @StringRes message: Int?,
    content: @Composable () -> Unit,
) {
    if (message != null) {
        Box(Modifier.fillMaxSize().padding(innerPadding).padding(16.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(message), textAlign = TextAlign.Center)
        }
    } else {
        content()
    }
}

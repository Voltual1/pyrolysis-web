package me.voltual.pyrolysis.ui.player

import androidx.compose.runtime.Composable

@Composable
expect fun PlayerScreen(
    viewModel: PlayerViewModel, 
    onBack: () -> Unit
)
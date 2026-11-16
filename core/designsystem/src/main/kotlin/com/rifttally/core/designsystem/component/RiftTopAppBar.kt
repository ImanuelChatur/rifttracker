package com.rifttally.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rifttally.core.designsystem.icon.RiftIcons
import com.rifttally.core.designsystem.theme.RiftTheme

/** Simple top app bar. Pass a [RiftIconButton] as [navigationIcon] / in [actions]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun RiftTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(text = title, style = RiftTheme.typography.titleLarge) },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = RiftTheme.colorScheme.surface,
            titleContentColor = RiftTheme.colorScheme.onSurface,
        ),
    )
}

@PreviewRiftThemes
@Composable
private fun RiftTopAppBarPreview() {
    RiftTheme {
        RiftTopAppBar(
            title = "Rift Tally",
            navigationIcon = {
                RiftIconButton(
                    imageVector = RiftIcons.ArrowBack,
                    contentDescription = "Back",
                    onClick = {},
                )
            },
            actions = {
                RiftIconButton(imageVector = RiftIcons.Settings, contentDescription = "Settings", onClick = {})
            },
        )
    }
}

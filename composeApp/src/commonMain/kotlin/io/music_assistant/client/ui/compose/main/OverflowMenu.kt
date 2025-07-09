package io.music_assistant.client.ui.compose.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.EllipsisV
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun OverflowMenu(
    modifier: Modifier = Modifier,
    options: List<OverflowMenuOption>,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = modifier.wrapContentSize(Alignment.TopStart)
    ) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = FontAwesomeIcons.Solid.EllipsisV,
                contentDescription = "Menu",
                tint = MaterialTheme.colors.onPrimary,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        option.onClick()
                        expanded = false
                    }
                ) {
                    Text(modifier = Modifier.padding(all = 4.dp), text = option.title)
                }
            }
        }
    }
}

data class OverflowMenuOption(
    val title: String,
    val onClick: () -> Unit
)

@Preview
@Composable
fun OverflowMenuPreview() {
    OverflowMenu(
        options = listOf(
            OverflowMenuOption("Option 1") { /* Handle Option 1 */ },
            OverflowMenuOption("Option 2") { /* Handle Option 2 */ },
            OverflowMenuOption("Option 3") { /* Handle Option 3 */ }
        )
    )
}
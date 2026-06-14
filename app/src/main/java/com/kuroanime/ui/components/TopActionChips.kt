package com.kuroanime.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TopActionChips(
    modifier: Modifier = Modifier,
    onChipClick: (String) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.width(12.dp))
        listOf(
            "history" to "Historial",
            "peliculas" to "Películas",
            "ovas" to "OVAs",
            "random" to " Aleatorio",
        ).forEach { (value, label) ->
            KuroChip(
                text = label,
                selected = false,
                onClick = { onChipClick(value) },
            )
        }
        Spacer(Modifier.width(4.dp))
    }
}

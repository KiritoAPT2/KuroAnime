package com.kuroanime.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kuroanime.data.model.Anime
import com.kuroanime.ui.components.AnimeCard
import com.kuroanime.ui.design.KuroDimens

@Composable
fun AnimeHorizontalSection(
    title: String,
    items: List<Anime>,
    onItemClick: (Anime) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(Modifier.height(KuroDimens.spacingLg))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = KuroDimens.spacingMd, vertical = 8.dp),
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = KuroDimens.spacingMd),
            horizontalArrangement = Arrangement.spacedBy(KuroDimens.spacingSm),
        ) {
            items(items, key = { it.url }) { anime ->
                AnimeCard(
                    anime = anime,
                    onClick = { onItemClick(anime) },
                )
            }
        }
    }
}

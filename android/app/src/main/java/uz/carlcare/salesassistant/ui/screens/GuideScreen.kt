package uz.carlcare.salesassistant.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uz.carlcare.salesassistant.R
import uz.carlcare.salesassistant.core.data.GuideCard
import uz.carlcare.salesassistant.core.data.GuideContent
import uz.carlcare.salesassistant.core.design.BrandColors
import uz.carlcare.salesassistant.ui.GuideViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(viewModel: GuideViewModel, onBack: () -> Unit) {
    val cards by viewModel.cards.collectAsState()
    val language = LocalConfiguration.current.locales.get(0)?.language ?: "en"
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }

    val selected = cards.firstOrNull { it.id == selectedId }

    BackHandler(enabled = selected != null) { selectedId = null }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selected != null) selected.content(language).title else stringResource(R.string.guide),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (selected != null) selectedId = null else onBack() }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (selected == null) {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(cards, key = { _, card -> card.id }) { index, card ->
                    GuideListItem(
                        number = index + 1,
                        content = card.content(language),
                        onClick = { selectedId = card.id },
                    )
                }
            }
        } else {
            GuideCardDetail(
                content = selected.content(language),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            )
        }
    }
}

@Composable
private fun GuideListItem(number: Int, content: GuideContent, onClick: () -> Unit) {
    val accent = BrandColors.Infinix
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(color = accent.copy(alpha = 0.14f), shape = RoundedCornerShape(10.dp)) {
                Text(
                    text = number.toString(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(content.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = content.what,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun GuideCardDetail(content: GuideContent, modifier: Modifier = Modifier) {
    val accent = BrandColors.Infinix
    val advantageColor = Color(0xFF2E7D32)
    val importantColor = Color(0xFFB26A00)
    val nuanceColor = Color(0xFF1565C0)

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionBlock(title = stringResource(R.string.guide_what), text = content.what)
        SectionBlock(title = stringResource(R.string.guide_watch), text = content.watch)

        Surface(
            color = accent.copy(alpha = 0.10f),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.guide_explain),
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                    )
                }
                Text(
                    text = content.explain,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

        CalloutRow("✅", stringResource(R.string.guide_advantage), content.advantage, advantageColor)
        CalloutRow("⚠️", stringResource(R.string.guide_important), content.important, importantColor)
        CalloutRow("💡", stringResource(R.string.guide_nuance), content.nuance, nuanceColor)

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

        SectionBlock(title = stringResource(R.string.guide_examples), text = content.examples)
    }
}

@Composable
private fun SectionBlock(title: String, text: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = text,
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun CalloutRow(emoji: String, label: String, text: String, color: Color) {
    Row(Modifier.fillMaxWidth()) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(100.dp)) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                )
            }
            Text(
                text = text,
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

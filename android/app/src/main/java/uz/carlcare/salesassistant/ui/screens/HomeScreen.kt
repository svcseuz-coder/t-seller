@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package uz.carlcare.salesassistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.carlcare.salesassistant.R
import uz.carlcare.salesassistant.core.design.BrandColors
import uz.carlcare.salesassistant.core.design.FeatureTile
import uz.carlcare.salesassistant.core.design.Pill
import uz.carlcare.salesassistant.core.design.SectionTitle
import uz.carlcare.salesassistant.ui.HomeViewModel
import uz.carlcare.salesassistant.ui.Routes

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSearch: (String) -> Unit,
    onOpenBrand: (String) -> Unit,
    onOpenTool: (String) -> Unit,
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val tecno = items.count { it.product.brand == "TECNO" }
    val infinix = items.count { it.product.brand == "Infinix" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            LanguageMenu()
        }
        Header()

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    stringResource(R.string.search_hint),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Outlined.Close, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focus.clearFocus()
                if (query.isNotBlank()) onSearch(query)
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BrandCard(
                name = stringResource(R.string.brand_tecno),
                count = tecno,
                accent = BrandColors.Tecno,
                onClick = { onOpenBrand("TECNO") },
                modifier = Modifier.weight(1f),
            )
            BrandCard(
                name = stringResource(R.string.brand_infinix),
                count = infinix,
                accent = BrandColors.Infinix,
                onClick = { onOpenBrand("Infinix") },
                modifier = Modifier.weight(1f),
            )
        }

        SectionTitle(stringResource(R.string.tools))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureTile(
                icon = Icons.AutoMirrored.Outlined.CompareArrows,
                title = stringResource(R.string.compare),
                subtitle = stringResource(R.string.compare_sub),
                accent = BrandColors.Tecno,
                onClick = { onOpenTool(Routes.COMPARE) },
                modifier = Modifier.weight(1f),
            )
            FeatureTile(
                icon = Icons.AutoMirrored.Outlined.MenuBook,
                title = stringResource(R.string.guide),
                subtitle = stringResource(R.string.guide_sub),
                accent = BrandColors.Infinix,
                onClick = { onOpenTool(Routes.GUIDE) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureTile(
                icon = Icons.Outlined.AutoAwesome,
                title = stringResource(R.string.assistant),
                subtitle = stringResource(R.string.assistant_sub),
                accent = BrandColors.Infinix,
                onClick = { onOpenTool(Routes.ASSISTANT) },
                modifier = Modifier.weight(1f),
            )
            FeatureTile(
                icon = Icons.Outlined.Quiz,
                title = stringResource(R.string.quiz),
                subtitle = stringResource(R.string.quiz_sub),
                accent = BrandColors.Tecno,
                onClick = { onOpenTool(Routes.QUIZ) },
                modifier = Modifier.weight(1f),
            )
        }
        FeatureTile(
            icon = Icons.Outlined.Build,
            title = stringResource(R.string.service_support),
            subtitle = stringResource(R.string.service_sub),
            accent = BrandColors.Tecno,
            onClick = { onOpenTool(Routes.SERVICE) },
            modifier = Modifier.fillMaxWidth(),
            background = painterResource(R.drawable.carlcare_service),
        )

        Text(
            text = stringResource(R.string.catalog_note, items.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.footer_credit),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LanguageMenu() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val overrides = AppCompatDelegate.getApplicationLocales()
    val current = if (overrides.isEmpty) {
        LocalConfiguration.current.locales.get(0)?.language
    } else {
        overrides.get(0)?.language
    }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Outlined.Translate,
                contentDescription = stringResource(R.string.language),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                "en" to R.string.language_en,
                "ru" to R.string.language_ru,
                "uz" to R.string.language_uz,
            ).forEach { (tag, label) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    leadingIcon = {
                        if (tag == current) {
                            Icon(Icons.Outlined.Check, contentDescription = null)
                        }
                    },
                    onClick = {
                        expanded = false
                        if (tag != current) {
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun Header() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        BrandColors.Tecno.copy(alpha = 0.22f),
                        BrandColors.Infinix.copy(alpha = 0.16f),
                    ),
                ),
            )
            .padding(22.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(text = "TECNO · INFINIX", accent = BrandColors.Tecno)
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BrandCard(
    name: String,
    count: Int,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(128.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.20f), Color.Transparent),
                    ),
                )
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(name, style = MaterialTheme.typography.titleMedium, color = accent)
                    Icon(
                        Icons.Outlined.ArrowOutward,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column {
                    Text(
                        text = if (count > 0) stringResource(R.string.models_count, count) else "—",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = stringResource(R.string.catalog),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

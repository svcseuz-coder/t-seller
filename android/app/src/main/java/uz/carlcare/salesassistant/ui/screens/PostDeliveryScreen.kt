package uz.carlcare.salesassistant.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.carlcare.salesassistant.R
import uz.carlcare.salesassistant.core.data.PostBranch
import uz.carlcare.salesassistant.core.design.BrandColors
import uz.carlcare.salesassistant.core.design.Pill
import uz.carlcare.salesassistant.core.design.SectionTitle
import uz.carlcare.salesassistant.ui.PostDeliveryViewModel
import uz.carlcare.salesassistant.ui.components.MultiSelectFilterSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDeliveryScreen(viewModel: PostDeliveryViewModel, onBack: () -> Unit) {
    val branches by viewModel.branches.collectAsStateWithLifecycle()
    val checkedAt by viewModel.checkedAt.collectAsStateWithLifecycle()
    val regions by viewModel.regions.collectAsStateWithLifecycle()
    val selectedRegions by viewModel.selected.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var filterOpen by rememberSaveable { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    val accent = BrandColors.Tecno

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.post_send),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { IntroCard(accent) }

            item {
                SectionTitle(
                    pluralStringResource(R.plurals.post_branches, branches.size, branches.size),
                )
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        viewModel.search(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            stringResource(R.string.post_search_hint),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = {
                                query = ""
                                viewModel.search("")
                            }) {
                                Icon(Icons.Outlined.Close, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AssistChip(
                        onClick = { filterOpen = true },
                        label = {
                            Text(
                                if (selectedRegions.isEmpty() || selectedRegions.size == regions.size) {
                                    stringResource(R.string.all_regions)
                                } else {
                                    pluralStringResource(
                                        R.plurals.post_regions_selected,
                                        selectedRegions.size,
                                        selectedRegions.size,
                                    )
                                },
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Tune,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                    if (selectedRegions.isNotEmpty()) {
                        TextButton(onClick = viewModel::clearRegions) {
                            Text(stringResource(R.string.region_clear))
                        }
                    }
                }
            }

            if (branches.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_results),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(branches, key = { it.id }) { branch ->
                BranchCard(branch, accent, uri)
            }

            item {
                Text(
                    text = stringResource(R.string.post_source_note, checkedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (filterOpen) {
        MultiSelectFilterSheet(
            title = stringResource(R.string.filter_region),
            hint = stringResource(R.string.region_search_hint),
            options = regions,
            selected = selectedRegions,
            onApply = { viewModel.applyRegions(it); filterOpen = false },
            onDismiss = { filterOpen = false },
        )
    }
}

@Composable
private fun IntroCard(accent: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(color = accent.copy(alpha = 0.16f), shape = RoundedCornerShape(12.dp)) {
                Icon(
                    Icons.Outlined.LocalShipping,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.post_intro_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.post_intro_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BranchCard(branch: PostBranch, accent: Color, uri: androidx.compose.ui.platform.UriHandler) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(branch.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = branch.region,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Pill(text = branch.city, accent = accent)
            }
            Text(
                text = branch.address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            branch.phones.forEach { phone ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { uri.openUri("tel:+$phone") }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Outlined.Phone,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = formatPhone(phone),
                        style = MaterialTheme.typography.bodyMedium,
                        color = accent,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.center_hours),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatPostHours(branch.hours),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row {
                TextButton(onClick = {
                    uri.openUri(
                        "https://www.google.com/maps/dir/?api=1&origin=&destination=${branch.lat},${branch.lng}",
                    )
                }) {
                    Icon(
                        Icons.Outlined.ArrowOutward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.route))
                }
                if (branch.phones.isNotEmpty()) {
                    TextButton(onClick = { uri.openUri("tel:+${branch.phones.first()}") }) {
                        Icon(
                            Icons.Outlined.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.call))
                    }
                }
            }
        }
    }
}

private fun formatPhone(raw: String): String =
    if (raw.length == 12) {
        "+998 ${raw.substring(3, 5)} ${raw.substring(5, 8)}-${raw.substring(8, 10)}-${raw.substring(10)}"
    } else {
        "+$raw"
    }

@Composable
private fun formatPostHours(raw: String): String {
    val labels = mapOf(
        "Du-Ya" to stringResource(R.string.post_days_mon_sun),
        "Du-Sh" to stringResource(R.string.post_days_mon_sat),
        "Du-Ju" to stringResource(R.string.post_days_mon_fri),
        "Sh-Ya" to stringResource(R.string.post_days_sat_sun),
        "Se-Ya" to stringResource(R.string.post_days_tue_sun),
    )
    return raw.split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" · ") { segment ->
            val key = labels.keys.firstOrNull { segment.startsWith(it) }
            val rest = segment.removePrefix(key.orEmpty())
                .trim()
                .replace('-', '–')
            if (key == null || rest.isEmpty()) segment else "${labels.getValue(key)} $rest"
        }
}

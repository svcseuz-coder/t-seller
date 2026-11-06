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
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import uz.carlcare.salesassistant.core.data.db.ServiceCenterEntity
import uz.carlcare.salesassistant.core.data.db.SupportInfoEntity
import uz.carlcare.salesassistant.core.design.BrandColors
import uz.carlcare.salesassistant.core.design.Pill
import uz.carlcare.salesassistant.core.design.SectionTitle
import uz.carlcare.salesassistant.ui.ServiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceScreen(viewModel: ServiceViewModel, onBack: () -> Unit, onOpenWarranty: () -> Unit) {
    val support by viewModel.support.collectAsStateWithLifecycle()
    val centers by viewModel.centers.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val uri = LocalUriHandler.current
    val accent = BrandColors.Tecno

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.service_support),
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
            support?.let { info ->
                item { SupportCard(info, accent, uri) }
                item { QuickLinks(accent, onOpenWarranty) }
            }

            item {
                SectionTitle(
                    pluralStringResource(R.plurals.service_centers, centers.size, centers.size),
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
                            stringResource(R.string.center_search_hint),
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

            if (centers.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_results),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(centers, key = { it.id }) { center ->
                CenterCard(center, accent, uri)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.service_data_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    support?.let {
                        Text(
                            text = stringResource(R.string.service_source_note, it.checkedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportCard(info: SupportInfoEntity, accent: Color, uri: androidx.compose.ui.platform.UriHandler) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = accent.copy(alpha = 0.16f), shape = RoundedCornerShape(12.dp)) {
                    Icon(
                        Icons.Outlined.Phone,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.hotline),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(info.hotline, style = MaterialTheme.typography.titleLarge)
                }
                FilledTonalButton(
                    onClick = { uri.openUri("tel:${info.hotlineDial}") },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(stringResource(R.string.call_hotline), maxLines = 1)
                }
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { uri.openUri("mailto:${info.email}") }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Outlined.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text(info.email, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun QuickLinks(accent: Color, onOpenWarranty: () -> Unit) {
    AssistChip(
        onClick = onOpenWarranty,
        label = { Text(stringResource(R.string.warranty_terms)) },
        leadingIcon = {
            Icon(
                Icons.Outlined.Assignment,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp),
            )
        },
    )
}

@Composable
private fun CenterCard(center: ServiceCenterEntity, accent: Color, uri: androidx.compose.ui.platform.UriHandler) {
    val phones = center.phones.split('|').filter { it.isNotBlank() }
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
                    Text(center.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = center.code,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Pill(text = center.city, accent = accent)
            }
            Text(
                text = center.address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            phones.forEach { phone ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { uri.openUri("tel:${phone.filter { it.isDigit() || it == '+' }}") }
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
                    Text(phone, style = MaterialTheme.typography.bodyMedium, color = accent)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.center_hours),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (center.weekdayHours.isNotBlank()) {
                    HoursRow(R.string.hours_weekdays, center.weekdayHours)
                }
                if (center.saturdayHours.isNotBlank()) {
                    HoursRow(R.string.hours_saturday, center.saturdayHours)
                }
                if (center.sundayHours.isNotBlank()) {
                    HoursRow(R.string.hours_sunday, center.sundayHours)
                }
            }
            Text(
                text = stringResource(R.string.brands) + ": " + center.brands.replace(",", ", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row {
                TextButton(onClick = {
                    uri.openUri("https://www.google.com/maps/dir/?api=1&origin=&destination=${center.lat},${center.lng}")
                }) {
                    Icon(
                        Icons.Outlined.ArrowOutward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.route))
                }
                if (phones.isNotEmpty()) {
                    TextButton(onClick = {
                        uri.openUri("tel:${phones.first().filter { it.isDigit() || it == '+' }}")
                    }) {
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

@Composable
private fun HoursRow(labelRes: Int, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp),
        )
        Text(text = formatHours(value), style = MaterialTheme.typography.bodySmall)
    }
}

private fun formatHours(raw: String): String =
    raw.split(',')
        .filter { it.isNotBlank() }
        .joinToString(" · ") { it.trim().replace('-', '–') }

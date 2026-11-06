package uz.carlcare.salesassistant.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import uz.carlcare.salesassistant.R
import uz.carlcare.salesassistant.core.data.ProductShareText
import uz.carlcare.salesassistant.core.data.ShareLine
import uz.carlcare.salesassistant.core.data.db.ProductWithSpecs
import uz.carlcare.salesassistant.core.design.BrandColors
import uz.carlcare.salesassistant.core.design.Pill
import uz.carlcare.salesassistant.core.design.SpecRow
import uz.carlcare.salesassistant.ui.ProductViewModel

internal val GROUPS = listOf(
    "release", "body", "display", "platform", "memory", "main_camera",
    "selfie_camera", "battery", "comms", "sound", "features", "software",
)

internal val FIELD_ORDER = mapOf(
    "display" to listOf("type", "size", "resolution", "refresh_rate", "brightness", "protection"),
    "platform" to listOf("chipset", "cpu", "gpu", "os"),
    "memory" to listOf("ram", "storage", "card_slot"),
    "main_camera" to listOf("modules", "features", "video", "ois", "eis"),
    "selfie_camera" to listOf("modules", "features", "video"),
    "battery" to listOf("capacity", "type", "wired_charging", "wireless_charging", "reverse_charging", "bypass_charging"),
    "comms" to listOf("network_5g", "nfc", "wlan", "bluetooth", "gps", "usb"),
    "features" to listOf("fingerprint", "face_unlock", "sensors"),
    "body" to listOf("dimensions", "weight", "build", "sim", "ip_rating", "colors"),
    "sound" to listOf("speakers", "jack"),
    "software" to listOf("android", "custom_ui", "updates"),
    "release" to listOf("announced", "status"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScreen(viewModel: ProductViewModel, onBack: () -> Unit) {
    val item by viewModel.item.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    var shareOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = item?.product?.model.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleFavorite, enabled = item != null) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = stringResource(
                                if (isFavorite) R.string.remove_favorite else R.string.add_favorite,
                            ),
                            tint = if (isFavorite) BrandColors.Favorite else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { shareOpen = true }, enabled = item != null) {
                        Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.share))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        val data = item
        if (data == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val product = data.product
        val accent = if (product.brand == "TECNO") BrandColors.Tecno else BrandColors.Infinix
        val variants = data.variants()
        var selected by rememberSaveable(product.id) { mutableStateOf(variants.first()) }
        val groupValues = data.specsFor(selected)
        val uri = LocalUriHandler.current

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                if (product.imageUrl != null) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.model,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Icon(
                        Icons.Outlined.PhoneAndroid,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = accent,
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(product.brand, accent)
                    Pill(product.series, accent)
                    Pill(product.year.toString(), accent)
                }
                Text(product.model, style = MaterialTheme.typography.headlineSmall)
                if (variants.size > 1) {
                    Text(
                        stringResource(R.string.variant),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        variants.forEach { variant ->
                            FilterChip(
                                selected = selected == variant,
                                onClick = { selected = variant },
                                label = { Text(variant) },
                            )
                        }
                    }
                    product.matchNote?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            GROUPS.forEach { group ->
                val fields = groupValues[group] ?: return@forEach
                if (fields.isEmpty()) return@forEach
                val order = FIELD_ORDER[group].orEmpty()
                val sorted = fields.keys.sortedWith(
                    compareBy({ order.indexOf(it).let { i -> if (i < 0) 99 else i } }, { it }),
                )
                Text(
                    text = stringResource(groupLabel(group)),
                    modifier = Modifier.padding(horizontal = 20.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        sorted.forEach { key ->
                            SpecRow(
                                label = stringResource(fieldLabel(group, key)),
                                value = fields[key],
                                emptyLabel = stringResource(R.string.not_verified),
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.missing_note),
                modifier = Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(R.string.source_note, product.fetchedAt ?: "—"),
                modifier = Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            product.gsmarenaUrl?.let { url ->
                TextButton(
                    onClick = { runCatching { uri.openUri(url) } },
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    Text(stringResource(R.string.open_source))
                }
            }
        }

        if (shareOpen) {
            SharePreviewDialog(
                data = data,
                variant = selected,
                onDismiss = { shareOpen = false },
            )
        }
    }
}

private fun shareTitle(data: ProductWithSpecs, variant: String): String {
    val base = "${data.product.brand} ${data.product.model}"
    return if (data.variants().size > 1) "$base · $variant" else base
}

@Composable
private fun shareMessage(data: ProductWithSpecs, variant: String): String {
    val specs = data.specsFor(variant)
    fun value(group: String, field: String) = specs[group]?.get(field)
    val displayLabel = stringResource(R.string.share_spec_display)
    val chipsetLabel = stringResource(R.string.share_spec_chipset)
    val memoryLabel = stringResource(R.string.share_spec_memory)
    val cameraLabel = stringResource(R.string.share_spec_camera)
    val batteryLabel = stringResource(R.string.share_spec_battery)
    val protectionLabel = stringResource(R.string.share_spec_protection)
    val lines = buildList {
        ProductShareText.displayLine(
            type = value("display", "type"),
            size = value("display", "size"),
            resolution = value("display", "resolution"),
            refresh = value("display", "refresh_rate"),
        )?.let { add(ShareLine(displayLabel, it)) }
        ProductShareText.chipsetLine(value("platform", "chipset"))?.let { add(ShareLine(chipsetLabel, it)) }
        ProductShareText.memoryLine(value("memory", "ram"), value("memory", "storage"))
            ?.let { add(ShareLine(memoryLabel, it)) }
        ProductShareText.cameraLine(value("main_camera", "modules"))?.let { add(ShareLine(cameraLabel, it)) }
        ProductShareText.batteryLine(value("battery", "capacity"), value("battery", "wired_charging"))
            ?.let { add(ShareLine(batteryLabel, it)) }
        ProductShareText.protectionLine(value("body", "ip_rating"))?.let { add(ShareLine(protectionLabel, it)) }
    }
    return ProductShareText.build(shareTitle(data, variant), lines, stringResource(R.string.footer_credit))
}

@Composable
private fun SharePreviewDialog(data: ProductWithSpecs, variant: String, onDismiss: () -> Unit) {
    val text = shareMessage(data, variant)
    val context = LocalContext.current
    val chooserTitle = stringResource(R.string.share_chooser)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_title)) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(text, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                        putExtra(Intent.EXTRA_SUBJECT, shareTitle(data, variant))
                    }
                    context.startActivity(Intent.createChooser(send, chooserTitle))
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.share_send))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.share_close))
            }
        },
    )
}

internal fun groupLabel(group: String): Int = when (group) {
    "release" -> R.string.group_general
    "body" -> R.string.group_body
    "display" -> R.string.group_display
    "platform" -> R.string.group_platform
    "memory" -> R.string.group_memory
    "main_camera" -> R.string.group_main_camera
    "selfie_camera" -> R.string.group_selfie_camera
    "battery" -> R.string.group_battery
    "comms" -> R.string.group_comms
    "sound" -> R.string.group_sound
    "features" -> R.string.group_features
    "software" -> R.string.group_software
    else -> R.string.group_general
}

internal fun fieldLabel(group: String, field: String): Int = when (field) {
    "announced" -> R.string.field_announced
    "status" -> R.string.field_status
    "dimensions" -> R.string.field_dimensions
    "weight" -> R.string.field_weight
    "build" -> R.string.field_build
    "sim" -> R.string.field_sim
    "ip_rating" -> R.string.field_ip_rating
    "colors" -> R.string.field_colors
    "type" -> if (group == "battery") R.string.field_battery_type else R.string.field_display_type
    "size" -> R.string.field_size
    "resolution" -> R.string.field_resolution
    "refresh_rate" -> R.string.field_refresh_rate
    "brightness" -> R.string.field_brightness
    "protection" -> R.string.field_protection
    "os" -> R.string.field_os
    "chipset" -> R.string.field_chipset
    "cpu" -> R.string.field_cpu
    "gpu" -> R.string.field_gpu
    "ram" -> R.string.field_ram
    "storage" -> R.string.field_storage
    "card_slot" -> R.string.field_card_slot
    "modules" -> R.string.field_modules
    "features" -> R.string.field_camera_features
    "video" -> R.string.field_video
    "ois" -> R.string.field_ois
    "eis" -> R.string.field_eis
    "capacity" -> R.string.field_capacity
    "wired_charging" -> R.string.field_wired_charging
    "wireless_charging" -> R.string.field_wireless_charging
    "reverse_charging" -> R.string.field_reverse_charging
    "bypass_charging" -> R.string.field_bypass_charging
    "network_5g" -> R.string.field_5g
    "nfc" -> R.string.field_nfc
    "wlan" -> R.string.field_wlan
    "bluetooth" -> R.string.field_bluetooth
    "gps" -> R.string.field_gps
    "usb" -> R.string.field_usb
    "sensors" -> R.string.field_sensors
    "fingerprint" -> R.string.field_fingerprint
    "face_unlock" -> R.string.field_face_unlock
    "speakers" -> R.string.field_speakers
    "jack" -> R.string.field_jack
    "android" -> R.string.field_android
    "custom_ui" -> R.string.field_custom_ui
    "updates" -> R.string.field_updates
    else -> R.string.field_other
}

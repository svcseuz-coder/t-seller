package uz.carlcare.salesassistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.carlcare.salesassistant.R
import uz.carlcare.salesassistant.core.data.SearchText
import uz.carlcare.salesassistant.core.data.db.ProductWithSpecs
import uz.carlcare.salesassistant.core.design.BrandColors
import uz.carlcare.salesassistant.ui.CompareViewModel

private data class CompareField(
    val group: String,
    val field: String,
    val values: List<String?>,
) {
    fun differs(): Boolean = values.map { it?.trim().orEmpty() }.distinct().size > 1
}

private fun buildGroups(
    items: List<ProductWithSpecs>,
    variantMap: Map<String, String>,
    onlyDifferences: Boolean,
): List<Pair<String, List<CompareField>>> {
    if (items.size < 2) return emptyList()
    val maps = items.map { data ->
        val labels = data.variants()
        val variant = variantMap[data.product.id]?.takeIf { it in labels } ?: labels.first()
        data.specsFor(variant)
    }
    return GROUPS.mapNotNull { group ->
        val order = FIELD_ORDER[group].orEmpty()
        val rows = maps.flatMap { it[group]?.keys.orEmpty() }
            .distinct()
            .sortedWith(
                compareBy({ order.indexOf(it).let { i -> if (i < 0) 99 else i } }, { it }),
            )
            .map { field -> CompareField(group, field, maps.map { it[group]?.get(field) }) }
            .filter { !onlyDifferences || it.differs() }
        if (rows.isEmpty()) null else group to rows
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(viewModel: CompareViewModel, onBack: () -> Unit) {
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val variantMap by viewModel.variantMap.collectAsStateWithLifecycle()
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    var onlyDifferences by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.compare)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                selected.forEach { data ->
                    CompareSlot(
                        data = data,
                        variant = variantMap[data.product.id] ?: data.variants().first(),
                        modifier = Modifier.weight(1f),
                        onVariant = { viewModel.setVariant(data.product.id, it) },
                        onRemove = { viewModel.remove(data.product.id) },
                    )
                }
                if (selected.size < CompareViewModel.MAX_ITEMS) {
                    AddSlot(
                        modifier = Modifier.weight(1f),
                        onClick = { pickerOpen = true },
                    )
                }
            }

            if (selected.size >= 2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.compare_only_diff),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(
                        checked = onlyDifferences,
                        onCheckedChange = { onlyDifferences = it },
                    )
                }
            }

            val groups = remember(selected, variantMap, onlyDifferences) {
                buildGroups(selected, variantMap, onlyDifferences)
            }

            when {
                selected.size < 2 -> HintCard(stringResource(R.string.compare_need_two))
                groups.isEmpty() -> HintCard(stringResource(R.string.compare_no_diff))
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    groups.forEach { (group, rows) ->
                        item(key = "title_$group") {
                            Text(
                                text = stringResource(groupLabel(group)),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        item(key = "card_$group") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                            ) {
                                Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                                    rows.forEachIndexed { index, row ->
                                        if (index > 0) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                            )
                                        }
                                        FieldRow(row)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            text = stringResource(R.string.missing_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (pickerOpen) {
        ProductPickerSheet(
            products = products,
            selectedIds = selected.map { it.product.id }.toSet(),
            onPick = {
                viewModel.toggle(it)
                pickerOpen = false
            },
            onDismiss = { pickerOpen = false },
        )
    }
}

@Composable
private fun CompareSlot(
    data: ProductWithSpecs,
    variant: String,
    modifier: Modifier,
    onVariant: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val product = data.product
    val accent = if (product.brand == "TECNO") BrandColors.Tecno else BrandColors.Infinix
    Card(
        modifier = modifier.height(132.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(product.brand, style = MaterialTheme.typography.labelSmall, color = accent)
                Text(
                    text = product.model,
                    modifier = Modifier.padding(end = 14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val variants = data.variants()
                if (variants.size > 1) {
                    VariantSwitch(
                        variants = variants,
                        selected = variant,
                        accent = accent,
                        onSelect = onVariant,
                    )
                }
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(26.dp),
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.compare_remove),
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun VariantSwitch(
    variants: List<String>,
    selected: String,
    accent: Color,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(2.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        variants.forEach { label ->
            val active = label == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (active) accent.copy(alpha = 0.18f) else Color.Transparent)
                    .selectable(selected = active, role = Role.RadioButton) { onSelect(label) }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AddSlot(modifier: Modifier, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.height(132.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.compare_add),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HintCard(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FieldRow(row: CompareField) {
    val differs = row.differs()
    val highlight = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
    ) {
        Text(
            text = stringResource(fieldLabel(row.group, row.field)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.values.forEach { value ->
                val empty = value.isNullOrBlank()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (differs && !empty) highlight.copy(alpha = 0.10f) else Color.Transparent,
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = value?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.not_verified),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (empty) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        fontStyle = if (empty) FontStyle.Italic else null,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductPickerSheet(
    products: List<ProductWithSpecs>,
    selectedIds: Set<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp),
        ) {
            Text(stringResource(R.string.compare_pick), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                placeholder = {
                    Text(
                        stringResource(R.string.search_hint),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
            )
            val filtered = remember(products, query) {
                products.filter { data ->
                    SearchText.matches(
                        "${data.product.brand} ${data.product.series} ${data.product.model}",
                        query,
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(filtered, key = { it.product.id }) { data ->
                    val isSelected = data.product.id in selectedIds
                    val accent = if (data.product.brand == "TECNO") {
                        BrandColors.Tecno
                    } else {
                        BrandColors.Infinix
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isSelected) { onPick(data.product.id) }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = data.product.brand,
                                style = MaterialTheme.typography.labelSmall,
                                color = accent,
                            )
                            Text(data.product.model, style = MaterialTheme.typography.bodyLarge)
                        }
                        if (isSelected) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = accent,
                            )
                        }
                    }
                }
            }
        }
    }
}

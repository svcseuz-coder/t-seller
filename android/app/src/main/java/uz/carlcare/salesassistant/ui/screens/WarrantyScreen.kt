package uz.carlcare.salesassistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uz.carlcare.salesassistant.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarrantyScreen(onBack: () -> Unit) {
    val coveredColor = Color(0xFF2E7D32)
    val excludedColor = Color(0xFFC62828)
    val importantColor = Color(0xFFB26A00)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.warranty_terms),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            WarrantyBlock(
                title = stringResource(R.string.warranty_covered_title),
                color = coveredColor,
            ) {
                Text(stringResource(R.string.warranty_covered_text), style = MaterialTheme.typography.bodyMedium)
            }

            WarrantyBlock(
                title = stringResource(R.string.warranty_excluded_title),
                color = excludedColor,
            ) {
                Text(stringResource(R.string.warranty_excluded_intro), style = MaterialTheme.typography.bodyMedium)
                val cases = stringArrayResource(R.array.warranty_excluded_cases)
                cases.forEach { item ->
                    Text(
                        text = "• $item",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            Column(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.warranty_how_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.warranty_how_text),
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Surface(
                color = importantColor.copy(alpha = 0.10f),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(
                        text = "⚠️ " + stringResource(R.string.warranty_important),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun WarrantyBlock(
    title: String,
    color: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = color,
            )
            Column(Modifier.padding(top = 8.dp), content = content)
        }
    }
}

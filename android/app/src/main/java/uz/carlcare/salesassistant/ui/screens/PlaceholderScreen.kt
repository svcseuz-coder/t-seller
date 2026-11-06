package uz.carlcare.salesassistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import uz.carlcare.salesassistant.R
import uz.carlcare.salesassistant.core.design.BrandColors
import uz.carlcare.salesassistant.ui.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(route: String, onBack: () -> Unit) {
    val titleRes = when (route) {
        Routes.COMPARE -> R.string.compare
        Routes.GUIDE -> R.string.guide
        Routes.ASSISTANT -> R.string.assistant
        else -> R.string.quiz
    }
    val subtitleRes = when (route) {
        Routes.COMPARE -> R.string.compare_sub
        Routes.GUIDE -> R.string.guide_sub
        Routes.ASSISTANT -> R.string.assistant_sub
        else -> R.string.quiz_sub
    }
    val icon: ImageVector = when (route) {
        Routes.COMPARE -> Icons.AutoMirrored.Outlined.CompareArrows
        Routes.GUIDE -> Icons.AutoMirrored.Outlined.MenuBook
        Routes.ASSISTANT -> Icons.Outlined.AutoAwesome
        else -> Icons.Outlined.Quiz
    }
    val accent = when (route) {
        Routes.COMPARE, Routes.QUIZ -> BrandColors.Tecno
        else -> BrandColors.Infinix
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
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
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(accent.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(44.dp))
            }
            Text(
                text = stringResource(titleRes),
                modifier = Modifier.padding(top = 24.dp),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(subtitleRes),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.coming_soon),
                modifier = Modifier.padding(top = 28.dp),
                style = MaterialTheme.typography.labelLarge,
                color = accent,
            )
        }
    }
}

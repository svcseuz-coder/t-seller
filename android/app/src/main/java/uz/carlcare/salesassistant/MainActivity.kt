package uz.carlcare.salesassistant

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import uz.carlcare.salesassistant.core.design.SalesAssistantTheme
import uz.carlcare.salesassistant.ui.AppNavHost
import uz.carlcare.salesassistant.ui.ThemeMode
import uz.carlcare.salesassistant.ui.ThemeSetting

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeSetting.init(this)
        enableEdgeToEdge()
        setContent {
            val dark = when (ThemeSetting.mode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            SalesAssistantTheme(darkTheme = dark) {
                val view = LocalView.current
                val background = MaterialTheme.colorScheme.background.toArgb()
                SideEffect {
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                    window.setBackgroundDrawable(ColorDrawable(background))
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNavHost(
                        container = (application as SalesAssistantApp).container,
                    )
                }
            }
        }
    }
}

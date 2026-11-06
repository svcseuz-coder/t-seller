package uz.carlcare.salesassistant

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import uz.carlcare.salesassistant.core.design.SalesAssistantTheme
import uz.carlcare.salesassistant.ui.AppNavHost

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SalesAssistantTheme {
                AppNavHost(
                    container = (application as SalesAssistantApp).container,
                )
            }
        }
    }
}

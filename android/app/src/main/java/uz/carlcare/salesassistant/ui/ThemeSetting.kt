package uz.carlcare.salesassistant.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode { System, Light, Dark }

object ThemeSetting {
    private const val PREFS = "t_seller_settings"
    private const val KEY = "theme_mode"

    private var prefs: SharedPreferences? = null

    var mode: ThemeMode by mutableStateOf(ThemeMode.System)
        private set

    fun init(context: Context) {
        val store = prefs ?: context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .also { prefs = it }
        mode = ThemeMode.entries.firstOrNull { it.name == store.getString(KEY, null) }
            ?: ThemeMode.System
    }

    fun update(context: Context, value: ThemeMode) {
        init(context)
        mode = value
        prefs?.edit()?.putString(KEY, value.name)?.apply()
    }
}

package uz.carlcare.salesassistant

import android.app.Application
import uz.carlcare.salesassistant.core.data.AppContainer

class SalesAssistantApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(
            context = this,
            assistantProxyUrl = BuildConfig.ASSISTANT_PROXY_URL,
            assistantAppToken = BuildConfig.ASSISTANT_APP_TOKEN,
        )
    }
}

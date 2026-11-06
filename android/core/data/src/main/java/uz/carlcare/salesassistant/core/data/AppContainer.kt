package uz.carlcare.salesassistant.core.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import uz.carlcare.salesassistant.core.data.db.AppDatabase

class AppContainer(
    context: Context,
    assistantProxyUrl: String = "",
    assistantAppToken: String = "",
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "sales_assistant.db",
    ).fallbackToDestructiveMigration().build()

    val catalogRepository = CatalogRepository(database, context.applicationContext)
    val serviceRepository = ServiceRepository(database, context.applicationContext)
    val guideRepository = GuideRepository(context.applicationContext)
    val quizRepository = QuizRepository(context.applicationContext)
    val assistantRepository = AssistantRepository(
        catalog = catalogRepository,
        guide = guideRepository,
        service = serviceRepository,
        proxyUrl = assistantProxyUrl,
        appToken = assistantAppToken,
    )

    init {
        scope.launch {
            catalogRepository.seedIfEmpty()
            serviceRepository.seedIfEmpty()
            guideRepository.load()
            quizRepository.load()
        }
    }
}

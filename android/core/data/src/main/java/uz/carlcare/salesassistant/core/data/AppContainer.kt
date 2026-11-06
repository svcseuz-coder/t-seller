package uz.carlcare.salesassistant.core.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import uz.carlcare.salesassistant.core.data.db.AppDatabase

class AppContainer(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "sales_assistant.db",
    ).fallbackToDestructiveMigration().build()

    val catalogRepository = CatalogRepository(database, context.applicationContext)
    val serviceRepository = ServiceRepository(database, context.applicationContext)
    val guideRepository = GuideRepository(context.applicationContext)

    init {
        scope.launch {
            catalogRepository.seedIfEmpty()
            serviceRepository.seedIfEmpty()
            guideRepository.load()
        }
    }
}

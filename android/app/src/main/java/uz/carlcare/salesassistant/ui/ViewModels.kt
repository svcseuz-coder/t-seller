package uz.carlcare.salesassistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import uz.carlcare.salesassistant.core.data.CatalogRepository
import uz.carlcare.salesassistant.core.data.GuideCard
import uz.carlcare.salesassistant.core.data.GuideRepository
import uz.carlcare.salesassistant.core.data.SearchText
import uz.carlcare.salesassistant.core.data.ServiceRepository
import uz.carlcare.salesassistant.core.data.db.ProductWithSpecs
import uz.carlcare.salesassistant.core.data.db.ServiceCenterEntity
import uz.carlcare.salesassistant.core.data.db.SupportInfoEntity

class HomeViewModel(repository: CatalogRepository) : ViewModel() {
    val items: StateFlow<List<ProductWithSpecs>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModel(
    repository: CatalogRepository,
    val brand: String?,
    val initialQuery: String,
) : ViewModel() {
    private val queryFlow = MutableStateFlow(initialQuery)

    val items: StateFlow<List<ProductWithSpecs>> = queryFlow
        .flatMapLatest { repository.observeFiltered(brand, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun search(query: String) {
        queryFlow.value = query
    }
}

class ProductViewModel(
    repository: CatalogRepository,
    id: String,
) : ViewModel() {
    val item: StateFlow<ProductWithSpecs?> = repository.observeProduct(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

class CompareViewModel(repository: CatalogRepository) : ViewModel() {
    private val selection = MutableStateFlow<List<String>>(emptyList())
    private val variants = MutableStateFlow<Map<String, String>>(emptyMap())

    val products: StateFlow<List<ProductWithSpecs>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selected: StateFlow<List<ProductWithSpecs>> = combine(products, selection) { items, ids ->
        ids.mapNotNull { id -> items.firstOrNull { it.product.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val variantMap: StateFlow<Map<String, String>> = variants

    fun variantOf(data: ProductWithSpecs): String {
        val labels = data.variants()
        return variants.value[data.product.id]?.takeIf { it in labels } ?: labels.first()
    }

    fun setVariant(id: String, variant: String) {
        variants.value = variants.value + (id to variant)
    }

    fun toggle(id: String) {
        val current = selection.value
        selection.value = when {
            id in current -> current - id
            current.size < MAX_ITEMS -> current + id
            else -> current
        }
    }

    fun remove(id: String) {
        selection.value = selection.value - id
    }

    companion object {
        const val MAX_ITEMS = 3
    }
}

class ServiceViewModel(repository: ServiceRepository) : ViewModel() {
    private val query = MutableStateFlow("")

    val support: StateFlow<SupportInfoEntity?> = repository.observeSupport()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val centers: StateFlow<List<ServiceCenterEntity>> =
        combine(repository.observeCenters(), query) { all, q ->
            all.filter { center ->
                SearchText.matches(
                    "${center.city} ${center.name} ${center.address} ${center.code}",
                    q,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun search(value: String) {
        query.value = value
    }
}

class GuideViewModel(repository: GuideRepository) : ViewModel() {
    val cards: StateFlow<List<GuideCard>> = repository.observeCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

object AppViewModelFactory {
    fun home(repository: CatalogRepository) = viewModelFactory {
        initializer { HomeViewModel(repository) }
    }

    fun catalog(repository: CatalogRepository, brand: String?, query: String) = viewModelFactory {
        initializer { CatalogViewModel(repository, brand, query) }
    }

    fun product(repository: CatalogRepository, id: String) = viewModelFactory {
        initializer { ProductViewModel(repository, id) }
    }

    fun compare(repository: CatalogRepository) = viewModelFactory {
        initializer { CompareViewModel(repository) }
    }

    fun service(repository: ServiceRepository) = viewModelFactory {
        initializer { ServiceViewModel(repository) }
    }

    fun guide(repository: GuideRepository) = viewModelFactory {
        initializer { GuideViewModel(repository) }
    }
}

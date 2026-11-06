package uz.carlcare.salesassistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import uz.carlcare.salesassistant.core.data.AssistantRepository
import uz.carlcare.salesassistant.core.data.AssistantResult
import uz.carlcare.salesassistant.core.data.CatalogRepository
import uz.carlcare.salesassistant.core.data.GuideCard
import uz.carlcare.salesassistant.core.data.GuideRepository
import uz.carlcare.salesassistant.core.data.QuizQuestion
import uz.carlcare.salesassistant.core.data.QuizRepository
import uz.carlcare.salesassistant.core.data.SearchText
import uz.carlcare.salesassistant.core.data.ServiceRepository
import uz.carlcare.salesassistant.core.data.db.ProductWithSpecs
import uz.carlcare.salesassistant.core.data.db.ServiceCenterEntity
import uz.carlcare.salesassistant.core.data.db.SupportInfoEntity

class HomeViewModel(private val repository: CatalogRepository) : ViewModel() {
    val items: StateFlow<List<ProductWithSpecs>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favorites: StateFlow<List<ProductWithSpecs>> = repository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recent: StateFlow<List<ProductWithSpecs>> = repository.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteIds: StateFlow<Set<String>> = repository.observeFavoriteIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleFavorite(id: String) {
        viewModelScope.launch { repository.toggleFavorite(id) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModel(
    private val repository: CatalogRepository,
    val brand: String?,
    val initialQuery: String,
) : ViewModel() {
    private val queryFlow = MutableStateFlow(initialQuery)

    val items: StateFlow<List<ProductWithSpecs>> = queryFlow
        .flatMapLatest { repository.observeFiltered(brand, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteIds: StateFlow<Set<String>> = repository.observeFavoriteIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun search(query: String) {
        queryFlow.value = query
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch { repository.toggleFavorite(id) }
    }
}

class ProductViewModel(
    private val repository: CatalogRepository,
    private val id: String,
) : ViewModel() {
    val item: StateFlow<ProductWithSpecs?> = repository.observeProduct(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isFavorite: StateFlow<Boolean> = repository.observeFavoriteIds()
        .map { id in it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch { repository.markViewed(id) }
    }

    fun toggleFavorite() {
        viewModelScope.launch { repository.toggleFavorite(id) }
    }
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

data class QuizState(
    val questions: List<QuizQuestion> = emptyList(),
    val index: Int = 0,
    val selected: Int? = null,
    val answers: List<Int?> = emptyList(),
    val finished: Boolean = false,
) {
    val current: QuizQuestion? get() = questions.getOrNull(index)
    val score: Int get() = questions.indices.count { answers.getOrNull(it) == questions[it].en.answer }
}

class QuizViewModel(private val repository: QuizRepository) : ViewModel() {
    private val _state = MutableStateFlow(QuizState())
    val state: StateFlow<QuizState> = _state

    fun start() {
        if (_state.value.questions.isNotEmpty()) return
        viewModelScope.launch {
            val all = repository.observeQuestions().first { it.isNotEmpty() }
            if (_state.value.questions.isNotEmpty()) return@launch
            val questions = all.shuffled().take(SESSION_SIZE).map { question ->
                question.permuted(question.en.options.indices.shuffled())
            }
            _state.value = QuizState(
                questions = questions,
                answers = List(questions.size) { null },
            )
        }
    }

    fun answer(option: Int) {
        val state = _state.value
        if (state.finished || state.selected != null || state.current == null) return
        _state.value = state.copy(
            selected = option,
            answers = state.answers.toMutableList().also { it[state.index] = option },
        )
    }

    fun next() {
        val state = _state.value
        if (state.selected == null) return
        _state.value = if (state.index + 1 >= state.questions.size) {
            state.copy(finished = true)
        } else {
            state.copy(index = state.index + 1, selected = null)
        }
    }

    fun restart() {
        _state.value = QuizState()
        start()
    }

    companion object {
        const val SESSION_SIZE = 10
    }
}

data class AssistantMessage(
    val fromUser: Boolean,
    val text: String,
    val sources: List<String> = emptyList(),
)

enum class AssistantError { NotConfigured, Unavailable }

data class AssistantState(
    val messages: List<AssistantMessage> = emptyList(),
    val input: String = "",
    val loading: Boolean = false,
    val error: AssistantError? = null,
)

class AssistantViewModel(private val repository: AssistantRepository) : ViewModel() {
    private val _state = MutableStateFlow(AssistantState())
    val state: StateFlow<AssistantState> = _state
    val configured: Boolean = repository.configured

    fun onInput(value: String) {
        _state.value = _state.value.copy(input = value, error = null)
    }

    fun send(language: String, prompt: String? = null) {
        val current = _state.value
        if (current.loading) return
        val question = (prompt ?: current.input).trim()
        if (question.isEmpty()) return
        val history = current.messages.map { it.fromUser to it.text }
        _state.value = current.copy(
            messages = current.messages + AssistantMessage(fromUser = true, text = question),
            input = "",
            loading = true,
            error = null,
        )
        viewModelScope.launch {
            val result = repository.ask(question, language, history)
            val latest = _state.value
            _state.value = when (result) {
                is AssistantResult.Success -> latest.copy(
                    messages = latest.messages + AssistantMessage(
                        fromUser = false,
                        text = result.answer,
                        sources = result.sources,
                    ),
                    loading = false,
                )
                AssistantResult.NotConfigured -> latest.copy(
                    loading = false,
                    error = AssistantError.NotConfigured,
                )
                AssistantResult.Unavailable -> latest.copy(
                    loading = false,
                    error = AssistantError.Unavailable,
                )
            }
        }
    }
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

    fun quiz(repository: QuizRepository) = viewModelFactory {
        initializer { QuizViewModel(repository) }
    }

    fun assistant(repository: AssistantRepository) = viewModelFactory {
        initializer { AssistantViewModel(repository) }
    }
}

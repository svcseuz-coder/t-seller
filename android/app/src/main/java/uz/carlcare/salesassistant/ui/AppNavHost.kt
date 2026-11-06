package uz.carlcare.salesassistant.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import uz.carlcare.salesassistant.core.data.AppContainer
import uz.carlcare.salesassistant.ui.screens.AssistantScreen
import uz.carlcare.salesassistant.ui.screens.CatalogScreen
import uz.carlcare.salesassistant.ui.screens.CompareScreen
import uz.carlcare.salesassistant.ui.screens.GuideScreen
import uz.carlcare.salesassistant.ui.screens.HomeScreen
import uz.carlcare.salesassistant.ui.screens.ProductScreen
import uz.carlcare.salesassistant.ui.screens.QuizScreen
import uz.carlcare.salesassistant.ui.screens.ServiceScreen
import uz.carlcare.salesassistant.ui.screens.WarrantyScreen

object Routes {
    const val HOME = "home"
    const val CATALOG = "catalog/{brand}?q={q}"
    const val PRODUCT = "product/{id}"
    const val COMPARE = "compare"
    const val GUIDE = "guide"
    const val ASSISTANT = "assistant"
    const val QUIZ = "quiz"
    const val SERVICE = "service"
    const val WARRANTY = "warranty"

    fun catalog(brand: String, query: String = "") =
        "catalog/$brand?q=${Uri.encode(query)}"

    fun product(id: String) = "product/$id"
}

@Composable
fun AppNavHost(container: AppContainer) {
    val nav = rememberNavController()
    val repository = container.catalogRepository

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel(factory = AppViewModelFactory.home(repository))
            HomeScreen(
                viewModel = vm,
                onSearch = { nav.navigate(Routes.catalog("ALL", it)) },
                onOpenBrand = { nav.navigate(Routes.catalog(it)) },
                onOpenTool = { nav.navigate(it) },
                onOpenProduct = { nav.navigate(Routes.product(it)) },
            )
        }
        composable(
            route = Routes.CATALOG,
            arguments = listOf(
                navArgument("brand") { type = NavType.StringType },
                navArgument("q") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val brandArg = entry.arguments?.getString("brand").orEmpty()
            val brand = brandArg.takeIf { it.isNotBlank() && it != "ALL" }
            val query = entry.arguments?.getString("q").orEmpty()
            val vm: CatalogViewModel = viewModel(
                factory = AppViewModelFactory.catalog(repository, brand, query),
            )
            CatalogScreen(
                viewModel = vm,
                onBack = { nav.popBackStack() },
                onOpenProduct = { nav.navigate(Routes.product(it)) },
            )
        }
        composable(
            route = Routes.PRODUCT,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            val vm: ProductViewModel = viewModel(
                factory = AppViewModelFactory.product(repository, id),
            )
            ProductScreen(
                viewModel = vm,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.COMPARE) {
            val vm: CompareViewModel = viewModel(
                factory = AppViewModelFactory.compare(repository),
            )
            CompareScreen(viewModel = vm, onBack = { nav.popBackStack() })
        }
        composable(Routes.GUIDE) {
            val vm: GuideViewModel = viewModel(
                factory = AppViewModelFactory.guide(container.guideRepository),
            )
            GuideScreen(viewModel = vm, onBack = { nav.popBackStack() })
        }
        composable(Routes.ASSISTANT) {
            val vm: AssistantViewModel = viewModel(
                factory = AppViewModelFactory.assistant(container.assistantRepository),
            )
            AssistantScreen(viewModel = vm, onBack = { nav.popBackStack() })
        }
        composable(Routes.QUIZ) {
            val vm: QuizViewModel = viewModel(
                factory = AppViewModelFactory.quiz(container.quizRepository),
            )
            QuizScreen(viewModel = vm, onBack = { nav.popBackStack() })
        }
        composable(Routes.SERVICE) {
            val vm: ServiceViewModel = viewModel(
                factory = AppViewModelFactory.service(container.serviceRepository),
            )
            ServiceScreen(
                viewModel = vm,
                onBack = { nav.popBackStack() },
                onOpenWarranty = { nav.navigate(Routes.WARRANTY) },
            )
        }
        composable(Routes.WARRANTY) {
            WarrantyScreen(onBack = { nav.popBackStack() })
        }
    }
}

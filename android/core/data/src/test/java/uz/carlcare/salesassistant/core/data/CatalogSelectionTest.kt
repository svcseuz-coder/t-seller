package uz.carlcare.salesassistant.core.data

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.carlcare.salesassistant.core.data.db.ProductEntity
import uz.carlcare.salesassistant.core.data.db.ProductWithSpecs

class CatalogSelectionTest {

    private fun product(id: String, model: String) = ProductWithSpecs(
        product = ProductEntity(
            id = id,
            brand = "TECNO",
            series = "CAMON",
            model = model,
            year = 2026,
            status = "Available",
            gsmarenaUrl = null,
            gsmarenaName = null,
            matchNote = null,
            fetchedAt = null,
            imageUrl = null,
            variantLabels = null,
        ),
        specs = emptyList(),
    )

    @Test
    fun `keeps the id order and skips unknown ids`() {
        val items = listOf(product("a", "A"), product("b", "B"), product("c", "C"))

        assertEquals(
            listOf("c", "a"),
            selectInOrder(listOf("c", "missing", "a"), items).map { it.product.id },
        )
    }

    @Test
    fun `empty ids yield an empty selection`() {
        assertEquals(emptyList<ProductWithSpecs>(), selectInOrder(emptyList(), listOf(product("a", "A"))))
    }
}

package uz.carlcare.salesassistant.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.carlcare.salesassistant.core.data.db.ProductEntity
import uz.carlcare.salesassistant.core.data.db.ProductWithSpecs
import uz.carlcare.salesassistant.core.data.db.ServiceCenterEntity
import uz.carlcare.salesassistant.core.data.db.SpecEntity
import uz.carlcare.salesassistant.core.data.db.SupportInfoEntity

class AssistantContextBuilderTest {

    private fun product(
        id: String,
        brand: String,
        series: String,
        model: String,
        vararg specs: Triple<String, String, String>,
    ) = ProductWithSpecs(
        product = ProductEntity(
            id = id,
            brand = brand,
            series = series,
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
        specs = specs.map { (group, field, value) -> SpecEntity(id, "global", group, field, value) },
    )

    private val camon = product(
        "camon50pro5g", "TECNO", "CAMON", "Camon 50 Pro 5G",
        Triple("battery", "capacity", "5200 mAh"),
        Triple("battery", "wired_charging", "70W"),
        Triple("display", "refresh_rate", "144Hz"),
    )
    private val spark = product(
        "spark50", "TECNO", "SPARK", "Spark 50 4G",
        Triple("battery", "capacity", "7000 mAh"),
        Triple("battery", "wired_charging", "18W"),
        Triple("display", "refresh_rate", "120Hz"),
    )
    private val pova = product(
        "pova8", "Infinix", "POVA", "Pova 8 5G",
        Triple("battery", "capacity", "8000 mAh"),
        Triple("battery", "wired_charging", "45W"),
        Triple("display", "refresh_rate", "120Hz"),
    )
    private val products = listOf(camon, spark, pova)

    private val bypassGuide = GuideCard(
        id = "g_charging",
        ru = content("Зарядка и bypass", "Питание от сети", "Играйте без нагрева", "Bypass-зарядка питает напрямую", "Не все модели", "Нагрев ниже"),
        en = content("Charging and bypass", "Power from the grid", "Play without heat", "Bypass charging feeds the phone directly", "Not every model", "Less heat"),
        uz = content("Quvvatlash va bypass", "Tarmoqdan quvvat", "Issiqliksiz o'ynang", "Bypass to'g'ridan-to'g'ri quvvatlaydi", "Ham modelda emas", "Issiqlik kam"),
    )

    private fun content(title: String, what: String, explain: String, important: String, nuance: String, examples: String) =
        GuideContent(
            title = title,
            what = what,
            watch = "watch",
            explain = explain,
            advantage = "advantage",
            important = important,
            nuance = nuance,
            examples = examples,
        )

    @Test
    fun `model question keeps both mentioned models`() {
        val context = AssistantContextBuilder.build(
            query = "Camon 50 Pro 5G или Spark 50 4G — что лучше?",
            language = "ru",
            products = products,
        )
        assertTrue(context.text.contains("TECNO Camon 50 Pro 5G"))
        assertTrue(context.text.contains("TECNO Spark 50 4G"))
        assertEquals(listOf("TECNO Camon 50 Pro 5G", "TECNO Spark 50 4G"), context.sources.take(2))
    }

    @Test
    fun `battery question ranks by capacity`() {
        val context = AssistantContextBuilder.build(
            query = "У какой модели самая ёмкая батарея?",
            language = "ru",
            products = products,
        )
        assertEquals("Infinix Pova 8 5G", context.sources.first())
        assertTrue(context.text.contains("capacity=8000 mAh"))
    }

    @Test
    fun `guide is included for technology question`() {
        val context = AssistantContextBuilder.build(
            query = "Как работает bypass-зарядка?",
            language = "ru",
            products = products,
            guides = listOf(bypassGuide),
        )
        assertTrue(context.text.contains("TECHNOLOGY GUIDE"))
        assertTrue(context.text.contains("Bypass-зарядка"))
        assertTrue(context.sources.contains("Зарядка и bypass"))
    }

    @Test
    fun `service question includes hotline and matching city`() {
        val context = AssistantContextBuilder.build(
            query = "Где сервисный центр в Ташкенте?",
            language = "ru",
            products = products,
            support = SupportInfoEntity(
                hotline = "71 2055420",
                hotlineDial = "712055420",
                email = "info@carlcare.com",
                sourceUrl = "https://www.carlcare.com/",
                checkedAt = "2026-11-05",
            ),
            centers = listOf(
                ServiceCenterEntity(
                    id = 1, code = "UZSI00061", name = "IP Mekhtiev Zaur", city = "Toshkent",
                    address = "Sergeli", phones = "712055420", brands = "TECNO,Infinix",
                    lat = 41.2, lng = 69.2, weekdayHours = "09:00–18:00",
                    saturdayHours = "", sundayHours = "",
                ),
            ),
        )
        assertTrue(context.text.contains("SERVICE"))
        assertTrue(context.text.contains("71 2055420"))
        assertTrue(context.text.contains("Toshkent"))
        assertTrue(context.sources.contains("Carlcare Uzbekistan"))
    }

    @Test
    fun `unknown topic stays empty instead of inventing`() {
        val context = AssistantContextBuilder.build(
            query = "zzz qqq",
            language = "en",
            products = products,
        )
        assertTrue(context.text.contains("no catalog match"))
        assertTrue(context.sources.isEmpty())
    }
}

package uz.carlcare.salesassistant.core.data

import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedAssetsTest {

    private fun asset(name: String): JSONObject {
        val file = listOf("src/main/assets/$name", "core/data/src/main/assets/$name")
            .map(::File)
            .firstOrNull { it.exists() }
            ?: error("missing asset $name (cwd=${File(".").absolutePath})")
        return JSONObject(file.readText())
    }

    @Test
    fun `catalog bundle keeps the agreed 30 models`() {
        val items = asset("products.json").getJSONArray("items")
        assertEquals(30, items.length())

        val ids = mutableSetOf<String>()
        var tecno = 0
        var infinix = 0
        for (i in 0 until items.length()) {
            val product = items.getJSONObject(i)
            assertTrue("blank id", product.getString("id").isNotBlank())
            assertTrue("duplicate id ${product.getString("id")}", ids.add(product.getString("id")))
            assertTrue("no specs for ${product.getString("id")}", product.getJSONObject("specs").length() > 0)
            when (product.getString("brand")) {
                "TECNO" -> tecno++
                "Infinix" -> infinix++
            }
        }
        assertEquals(19, tecno)
        assertEquals(11, infinix)
    }

    @Test
    fun `hot 60i keeps both 4G and 5G variants`() {
        val items = asset("products.json").getJSONArray("items")
        val hot = (0 until items.length())
            .map { items.getJSONObject(it) }
            .single { it.getString("model") == "Hot 60i" }

        val labels = hot.getJSONArray("variant_labels")
        assertEquals(listOf("4G", "5G"), (0 until labels.length()).map { labels.getString(it) })
        assertEquals(1, hot.getJSONArray("regional_variants").length())
        assertEquals("5G", hot.getJSONArray("regional_variants").getJSONObject(0).getString("label"))
    }

    @Test
    fun `service bundle has valid centers and support contacts`() {
        val root = asset("service_centers.json")
        val centers = root.getJSONArray("centers")
        assertEquals(17, centers.length())

        val ids = mutableSetOf<Int>()
        val codes = mutableSetOf<String>()
        val cities = mutableSetOf<String>()
        val cityByCode = mutableMapOf<String, String>()
        for (i in 0 until centers.length()) {
            val center = centers.getJSONObject(i)
            val label = center.optString("name", "#$i")
            assertTrue("$label: blank name", center.getString("name").isNotBlank())
            assertTrue("$label: blank address", center.getString("address").isNotBlank())
            assertTrue("$label: bad code", center.getString("code").startsWith("UZSI"))
            assertTrue("$label: duplicate id", ids.add(center.getInt("id")))
            assertTrue("$label: duplicate code", codes.add(center.getString("code")))
            assertTrue("$label: no phones", center.getJSONArray("phones").length() > 0)
            assertTrue("$label: no brands", center.getJSONArray("brands").length() > 0)
            assertTrue("$label: no coordinates", center.getDouble("lat") != 0.0 && center.getDouble("lng") != 0.0)
            assertTrue("$label: no weekday hours", center.getString("weekday_hours").isNotBlank())
            cities += center.getString("city")
            cityByCode[center.getString("code")] = center.getString("city")
        }
        assertEquals(17, ids.size)
        assertEquals(15, cities.size)
        assertEquals("Toshkent", cityByCode["UZSI00061"])
        assertEquals("2026-11-05", root.getString("checked_at"))

        val support = root.getJSONObject("support")
        assertEquals("71 2055420", support.getString("hotline"))
        assertEquals("712055420", support.getString("hotline_dial"))
        assertTrue(support.getString("email").contains("@"))
        assertTrue(root.getString("source").startsWith("https://www.carlcare.com/"))
    }
}

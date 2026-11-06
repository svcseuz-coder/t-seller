package uz.carlcare.salesassistant.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTextTest {
    @Test
    fun `cyrillic city names fold to the uzbek latin spelling`() {
        assertEquals("toshkent", SearchText.normalize("Ташкент"))
        assertEquals("toshkent", SearchText.normalize("Тошкент"))
        assertEquals("buxoro", SearchText.normalize("Бухара"))
        assertEquals("samarqand", SearchText.normalize("Самарканд"))
        assertEquals("qoqon", SearchText.normalize("Коканд"))
        assertEquals("jizzax", SearchText.normalize("Джизак"))
        assertEquals("andijon", SearchText.normalize("Андижан"))
        assertEquals("margilon", SearchText.normalize("Маргилан"))
        assertEquals("urganch", SearchText.normalize("Ургенч"))
        assertEquals("xiva", SearchText.normalize("Хива"))
        assertEquals("ohangaron", SearchText.normalize("Ахангаран"))
        assertEquals("yangiyol", SearchText.normalize("Янгиюль"))
    }

    @Test
    fun `latin and cyrillic queries of the same city are equal`() {
        assertEquals(SearchText.normalize("Tashkent"), SearchText.normalize("Ташкент"))
        assertEquals(SearchText.normalize("Yangiyo'l"), SearchText.normalize("Янгиюль"))
    }

    @Test
    fun `apostrophes are dropped instead of splitting words`() {
        assertEquals("yangiyol", SearchText.normalize("Yangiyo'l"))
        assertEquals("gijduvon", SearchText.normalize("G'ijduvon"))
        assertEquals("margilon", SearchText.normalize("Marg'ilon"))
        assertEquals("kochasi", SearchText.normalize("ko'chasi"))
    }

    @Test
    fun `russian address matches a latin city query`() {
        val address = SearchText.normalize("город Ташкент, Сергелийский район, улица Янги Сергели 8")
        assertTrue(address.contains(SearchText.normalize("Toshkent")))
        assertTrue(address.contains(SearchText.normalize("Ташкент")))
        assertTrue(address.contains("sergeli"))
    }

    @Test
    fun `codes and punctuation are normalized`() {
        assertEquals("uzsi00061", SearchText.normalize("UZSI00061"))
        assertEquals("uzsi 00061", SearchText.normalize("uzsi-00061"))
        assertEquals("", SearchText.normalize("  "))
    }

    @Test
    fun `russian model names fold to the latin catalog spelling`() {
        assertEquals("tecno", SearchText.normalize("Техно"))
        assertEquals("tecno", SearchText.normalize("Текно"))
        assertEquals("infinix", SearchText.normalize("Инфиникс"))
        assertEquals("camon", SearchText.normalize("Камон"))
        assertEquals("spark", SearchText.normalize("Спарк"))
        assertEquals("pova", SearchText.normalize("Пова"))
        assertEquals("hot", SearchText.normalize("Хот"))
        assertEquals("note", SearchText.normalize("Ноте"))
        assertEquals("pop", SearchText.normalize("Поп"))
        assertEquals("edge", SearchText.normalize("Эдж"))
        assertEquals("spark go 3", SearchText.normalize("Спарк Гоу 3"))
        assertEquals("camon 50 pro 5g", SearchText.normalize("Камон 50 Про 5G"))
    }

    @Test
    fun `russian queries match latin model names`() {
        assertTrue(SearchText.matches("TECNO CAMON Camon 50 Pro 5G", "Камон Про 5G"))
        assertTrue(SearchText.matches("TECNO SPARK Spark 50 5G", "Спарк 50"))
        assertTrue(SearchText.matches("Infinix HOT Hot 60i 5G", "Хот 60"))
        assertTrue(SearchText.matches("Infinix SMART Smart 20", "Инфиникс Смарт"))
        assertTrue(SearchText.matches("TECNO POP Pop X 5G", "Поп Икс"))
        assertTrue(SearchText.matches("TECNO POVA Pova 8 5G", "Пова 8"))
        assertFalse(SearchText.matches("TECNO SPARK Spark 50 5G", "Ноте 60"))
        assertFalse(SearchText.matches("TECNO SPARK Spark 50 5G", "Камон"))
    }

    @Test
    fun `query tokens may appear anywhere in the haystack`() {
        assertTrue(SearchText.matches("TECNO SPARK Spark 50 5G", "spark 50 5g"))
        assertTrue(SearchText.matches("TECNO SPARK Spark 50 5G", "5g"))
        assertFalse(SearchText.matches("TECNO SPARK Spark 50 4G", "5g"))
        assertFalse(SearchText.matches("TECNO SPARK Spark 50 5G", "spark 60"))
    }
}

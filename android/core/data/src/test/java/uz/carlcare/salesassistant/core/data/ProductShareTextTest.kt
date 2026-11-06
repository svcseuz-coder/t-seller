package uz.carlcare.salesassistant.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductShareTextTest {

    @Test
    fun `compacts verbose gsmarena values`() {
        assertEquals(
            "6.78 inches · AMOLED · 1208 x 2644 · 144Hz",
            ProductShareText.displayLine(
                type = "AMOLED, 144Hz",
                size = "6.78 inches, 112.1 cm2 (~89.6% screen-to-body ratio)",
                resolution = "1208 x 2644 pixels, 19.5:9 ratio (~429 ppi density)",
                refresh = "144Hz",
            ),
        )
        assertEquals(
            "Mediatek Dimensity 7400 Ultimate",
            ProductShareText.chipsetLine("Mediatek Dimensity 7400 Ultimate (4 nm)"),
        )
        assertEquals("8GB, 12GB · 256GB, 512GB", ProductShareText.memoryLine("8GB, 12GB", "256GB, 512GB"))
        assertEquals(
            "50 MP + 50 MP + 8 MP",
            ProductShareText.cameraLine(
                "50 MP, f/1.8, 23mm (wide), PDAF, OIS; 50 MP, f/2.4, 70mm (telephoto), OIS; " +
                    "8 MP, f/2.2, 14mm (ultrawide)",
            ),
        )
        assertEquals("6500 mAh · 45W wired", ProductShareText.batteryLine("6500 mAh", "45W wired"))
        assertEquals(
            "IP68/IP69K",
            ProductShareText.protectionLine(
                "IP68/IP69K dust tight and water resistant (immersion up to 2m for 30 min)",
            ),
        )
    }

    @Test
    fun `keeps refresh rate when the type does not mention it`() {
        assertEquals(
            "6.7 inches · IPS LCD · 720 x 1600 · 120Hz",
            ProductShareText.displayLine("IPS LCD", "6.7 inches, 108.4 cm2", "720 x 1600 pixels", "120Hz"),
        )
    }

    @Test
    fun `falls back to the module list when modules cannot be parsed`() {
        assertEquals("Quad camera setup", ProductShareText.cameraLine("Quad camera setup"))
        assertEquals(
            "50 MP, (wide), AF · Auxiliary lens",
            ProductShareText.cameraLine("50 MP, (wide), AF; Auxiliary lens"),
        )
    }

    @Test
    fun `drops charging detail after the comma`() {
        assertEquals(
            "5160 mAh · 45W wired",
            ProductShareText.batteryLine("5160 mAh", "45W wired, 50% in 24 min"),
        )
    }

    @Test
    fun `missing values are skipped`() {
        assertNull(ProductShareText.displayLine(null, null, null, null))
        assertNull(ProductShareText.memoryLine(null, " "))
        assertEquals("5200 mAh", ProductShareText.batteryLine("5200 mAh", null))
    }

    @Test
    fun `builds the message with title, labelled lines and footer`() {
        val text = ProductShareText.build(
            title = "TECNO Camon 50 Pro 5G",
            lines = listOf(
                ShareLine("Экран", "6.78 inches · AMOLED"),
                ShareLine("Батарея", "6500 mAh · 45W wired"),
            ),
            footer = "T-Seller · Carlcare Uzbekistan",
        )
        assertEquals(
            "TECNO Camon 50 Pro 5G\n\nЭкран: 6.78 inches · AMOLED\nБатарея: 6500 mAh · 45W wired\n\n" +
                "T-Seller · Carlcare Uzbekistan",
            text,
        )
    }
}

package uz.carlcare.salesassistant.core.data

import uz.carlcare.salesassistant.core.data.db.ProductWithSpecs
import uz.carlcare.salesassistant.core.data.db.ServiceCenterEntity
import uz.carlcare.salesassistant.core.data.db.SupportInfoEntity

data class AssistantContext(
    val text: String,
    val sources: List<String>,
)

object AssistantContextBuilder {
    private const val MAX_PRODUCTS = 4
    private const val MAX_GUIDES = 2
    const val MAX_CHARS = 7000

    private val stopWords = setOf(
        "chto", "kakoy", "kakaya", "kakie", "chem", "ili", "dlya", "pro", "mne", "est",
        "luchshe", "huzhe", "skolko", "mozhno", "nuzhno", "kak", "pochemu", "eto", "po",
        "ot", "do", "samyy", "samaya", "samoe", "bolshoy", "bolshaya", "bolshe", "chem",
        "what", "which", "the", "is", "are", "does", "do", "for", "with", "vs", "versus",
        "better", "best", "how", "why", "this", "that", "can", "you", "tell", "me", "about",
        "qaysi", "qanday", "nima", "bormi", "uchun", "bilan", "yaxshiroq", "telefon",
        "haqida", "menga", "ayting", "qanaqa", "eng", "bor", "kerak",
    )

    private val serviceTokens = setOf(
        "servis", "service", "xizmat", "kafolat", "warranty", "garant", "remont", "tamir",
        "repair", "markaz", "center", "tsentr", "ishonch", "hotline", "podderzhka", "support",
    )

    private val batteryIntent = setOf("battery", "batareya", "akkumulyator", "emos")
    private val cameraIntent = setOf("camera", "kamera", "megapixel", "mp", "photo", "foto")
    private val chargingIntent = setOf("charging", "zaryadka", "quvvatlash", "charge", "charger")
    private val refreshIntent = setOf("refresh", "gerts", "hz", "fps")

    private val specGroups = listOf(
        "release" to listOf("status"),
        "body" to listOf("dimensions", "weight", "build", "sim", "ip_rating", "colors"),
        "display" to listOf("type", "size", "resolution", "refresh_rate", "brightness", "protection"),
        "platform" to listOf("os", "chipset", "cpu", "gpu"),
        "memory" to listOf("ram", "storage", "card_slot"),
        "main_camera" to listOf("modules", "features", "video", "ois", "eis"),
        "selfie_camera" to listOf("modules"),
        "battery" to listOf("type", "capacity", "wired_charging", "wireless_charging", "reverse_charging", "bypass_charging"),
        "comms" to listOf("network_5g", "nfc", "wlan", "bluetooth", "usb"),
        "features" to listOf("fingerprint", "face_unlock"),
        "sound" to listOf("speakers", "jack"),
        "software" to listOf("android", "custom_ui", "updates"),
    )

    fun build(
        query: String,
        language: String,
        products: List<ProductWithSpecs>,
        guides: List<GuideCard> = emptyList(),
        support: SupportInfoEntity? = null,
        centers: List<ServiceCenterEntity> = emptyList(),
    ): AssistantContext {
        val tokens = contentTokens(query)
        val normalizedQuery = SearchText.normalize(query)

        val keywordMatches = products
            .map { it to scoreProduct(it, tokens, normalizedQuery) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(MAX_PRODUCTS)
            .map { it.first }
        val chosenProducts = keywordMatches.ifEmpty { intentProducts(products, tokens) }

        val chosenGuides = guides
            .map { it to scoreGuide(it, tokens, language) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(MAX_GUIDES)
            .map { it.first }

        val includesService = tokens.any { it in serviceTokens }

        val text = buildString {
            appendLine("CATALOG: 30 models in the Uzbekistan line-up — 19 TECNO, 11 Infinix.")
            appendLine(
                "DATA RULES: use only the facts below. If a value is absent, say it is not verified " +
                    "in the app data. Never invent specs, prices, availability or warranty decisions.",
            )
            if (chosenProducts.isEmpty()) {
                appendLine("PRODUCTS: no catalog match for this question.")
            } else {
                appendLine("PRODUCTS:")
                chosenProducts.forEach { appendLine(renderProduct(it)) }
            }
            if (chosenGuides.isNotEmpty()) {
                appendLine("TECHNOLOGY GUIDE:")
                chosenGuides.forEach { card ->
                    val content = card.content(language)
                    appendLine(
                        "- ${content.title}: ${content.what} ${content.explain} " +
                            "Important: ${content.important} Nuance: ${content.nuance}",
                    )
                }
            }
            if (includesService && support != null) {
                val cities = centers.map { it.city }.distinct()
                appendLine("SERVICE (Carlcare Uzbekistan):")
                appendLine(
                    "- Hotline ${support.hotline} (dial ${support.hotlineDial}), email ${support.email}.",
                )
                appendLine(
                    "- ${centers.size} service centers in ${cities.size} cities: ${cities.joinToString(", ")}.",
                )
                val city = centers.firstOrNull { center ->
                    val normalizedCity = SearchText.normalize(center.city)
                    tokens.any { token -> normalizedCity.contains(token) }
                }
                if (city != null) {
                    appendLine(
                        "- ${city.city}: ${city.name}, ${city.address}; " +
                            "phones ${city.phones.replace("|", ", ")}; hours ${city.weekdayHours}.",
                    )
                }
                appendLine(
                    "- Warranty covers manufacturing defects within the warranty period. Mechanical " +
                        "damage, liquid damage and unauthorized repair are excluded; the final decision " +
                        "follows diagnostics at an authorized center.",
                )
            }
        }.take(MAX_CHARS)

        val sources = buildList {
            chosenProducts.forEach { add("${it.product.brand} ${it.product.model}") }
            chosenGuides.forEach { add(it.content(language).title) }
            if (includesService && support != null) add("Carlcare Uzbekistan")
        }

        return AssistantContext(text = text, sources = sources)
    }

    private fun contentTokens(query: String): List<String> =
        SearchText.normalize(query)
            .split(' ')
            .filter { it.length >= 2 && it !in stopWords }
            .distinct()

    private fun scoreProduct(
        data: ProductWithSpecs,
        tokens: List<String>,
        normalizedQuery: String,
    ): Int {
        if (tokens.isEmpty()) return 0
        val name = SearchText.normalize(
            "${data.product.brand} ${data.product.series} ${data.product.model}",
        )
        val nameTokens = name.split(' ')
        val values = SearchText.normalize(data.specs.joinToString(" ") { it.value })

        var score = 0
        for (token in tokens) {
            score += when {
                token in nameTokens -> 4
                name.contains(token) -> 2
                values.contains(token) -> 1
                else -> 0
            }
        }
        if (normalizedQuery.contains(SearchText.normalize(data.product.model))) score += 6
        return score
    }

    private fun scoreGuide(card: GuideCard, tokens: List<String>, language: String): Int {
        if (tokens.isEmpty()) return 0
        val content = card.content(language)
        val haystack = SearchText.normalize(
            "${content.title} ${content.what} ${content.explain} ${content.important} ${content.nuance}",
        )
        return tokens.count { haystack.contains(it) }
    }

    private fun intentProducts(products: List<ProductWithSpecs>, tokens: List<String>): List<ProductWithSpecs> {
        val intent = when {
            tokens.any { it in batteryIntent } -> "battery"
            tokens.any { it in cameraIntent } -> "camera"
            tokens.any { it in chargingIntent } -> "charging"
            tokens.any { it in refreshIntent } -> "refresh"
            else -> return emptyList()
        }
        return when (intent) {
            "battery" -> products.sortedByDescending { specNumber(it, "battery", "capacity") }
            "camera" -> products.sortedByDescending { cameraMegapixels(it) }
            "charging" -> products.sortedByDescending { specNumber(it, "battery", "wired_charging") }
            else -> products.sortedByDescending { specNumber(it, "display", "refresh_rate") }
        }.take(MAX_PRODUCTS)
    }

    private fun specNumber(data: ProductWithSpecs, group: String, field: String): Int {
        val value = data.field(group, field, data.variants().first()) ?: return 0
        return Regex("\\d+").find(value)?.value?.toIntOrNull() ?: 0
    }

    private fun cameraMegapixels(data: ProductWithSpecs): Int {
        val value = data.field("main_camera", "modules", data.variants().first()) ?: return 0
        return Regex("(\\d+)\\s*MP").findAll(value)
            .mapNotNull { it.groupValues.getOrNull(1)?.toIntOrNull() }
            .maxOrNull() ?: 0
    }

    private fun renderProduct(data: ProductWithSpecs): String {
        val variants = data.variants()
        return buildString {
            append("MODEL: ${data.product.brand} ${data.product.model}")
            if (variants.size > 1) append(" (variants: ${variants.joinToString(" / ")})")
            appendLine()
            for (variant in variants) {
                val specs = data.specsFor(variant)
                if (variants.size > 1) appendLine("variant $variant:")
                for ((group, fields) in specGroups) {
                    val parts = fields.mapNotNull { field ->
                        specs[group]?.get(field)?.let { "$field=$it" }
                    }
                    if (parts.isNotEmpty()) {
                        appendLine("- $group: ${parts.joinToString("; ")}")
                    }
                }
            }
        }
    }
}

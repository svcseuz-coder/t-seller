package uz.carlcare.salesassistant.core.data

// Aliases bridge Russian transliteration and the Latin spellings used in the data (Ташкент/Tashkent -> toshkent, Техно -> tecno).
object SearchText {
    private val cyrillic = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e", 'ё' to "yo",
        'ж' to "j", 'з' to "z", 'и' to "i", 'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m",
        'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u",
        'ф' to "f", 'х' to "h", 'ц' to "ts", 'ч' to "ch", 'ш' to "sh", 'щ' to "sh",
        'ъ' to "", 'ы' to "i", 'ь' to "", 'э' to "e", 'ю' to "yu", 'я' to "ya",
        'ў' to "o", 'қ' to "q", 'ғ' to "g", 'ҳ' to "h", 'ң' to "ng",
    )

    private val aliases = mapOf(
        "tashkent" to "toshkent",
        "buxara" to "buxoro",
        "buhara" to "buxoro",
        "bukhara" to "buxoro",
        "samarkand" to "samarqand",
        "andijan" to "andijon",
        "fergana" to "fargona",
        "kokand" to "qoqon",
        "khiva" to "xiva",
        "hiva" to "xiva",
        "termez" to "termiz",
        "djizak" to "jizzax",
        "jizzak" to "jizzax",
        "navoi" to "navoiy",
        "karshi" to "qarshi",
        "urgench" to "urganch",
        "margilan" to "margilon",
        "yangiyul" to "yangiyol",
        "axangaran" to "ohangaron",
        "akhangaran" to "ohangaron",
        "ahangaran" to "ohangaron",
        "gijduvan" to "gijduvon",
        "gulistan" to "guliston",
        "chirchik" to "chirchiq",
        "tehno" to "tecno",
        "tekno" to "tecno",
        "infiniks" to "infinix",
        "infinks" to "infinix",
        "kamon" to "camon",
        "gou" to "go",
        "edj" to "edge",
        "iks" to "x",
    )

    private val apostrophes = setOf('\'', '’', 'ʻ', 'ʼ', '`', '´')

    fun normalize(value: String): String {
        val buffer = StringBuilder(value.length)
        for (ch in value.lowercase()) {
            when {
                ch in apostrophes -> Unit
                cyrillic.containsKey(ch) -> buffer.append(cyrillic.getValue(ch))
                ch.isLetterOrDigit() -> buffer.append(ch)
                else -> buffer.append(' ')
            }
        }
        return buffer.split(' ')
            .filter { it.isNotEmpty() }
            .joinToString(" ") { aliases[it] ?: it }
    }

    fun matches(haystack: String, query: String): Boolean {
        val needle = normalize(query)
        if (needle.isEmpty()) return true
        val hay = normalize(haystack)
        return needle.split(' ').all { hay.contains(it) }
    }
}

package uz.carlcare.salesassistant.core.data

data class ShareLine(val label: String, val value: String)

object ProductShareText {

    fun build(title: String, lines: List<ShareLine>, footer: String): String {
        val body = lines.joinToString("\n") { "${it.label}: ${it.value}" }
        return "$title\n\n$body\n\n$footer"
    }

    fun displayLine(type: String?, size: String?, resolution: String?, refresh: String?): String? {
        val parts = mutableListOf<String>()
        head(size, ',').let { if (it != null) parts += it }
        val typeHead = head(type, ',')
        if (typeHead != null) parts += typeHead
        head(resolution, ',')?.removeSuffix(" pixels")?.let { parts += it }
        val refreshValue = refresh?.trim()?.takeIf { it.isNotEmpty() }
        if (refreshValue != null && typeHead?.contains(refreshValue) != true) parts += refreshValue
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    fun chipsetLine(chipset: String?): String? = head(chipset, '(')

    fun memoryLine(ram: String?, storage: String?): String? =
        listOfNotNull(ram?.trim()?.takeIf { it.isNotEmpty() }, storage?.trim()?.takeIf { it.isNotEmpty() })
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" · ")

    fun cameraLine(modules: String?): String? {
        val raw = modules?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val list = raw.split(';').map { it.trim() }.filter { it.isNotEmpty() }
        val megapixels = list.mapNotNull { module -> Regex("^\\d+(?:\\.\\d+)?\\s*MP").find(module)?.value }
        return if (list.isNotEmpty() && megapixels.size == list.size) {
            megapixels.joinToString(" + ")
        } else {
            list.joinToString(" · ")
        }
    }

    fun batteryLine(capacity: String?, wiredCharging: String?): String? =
        listOfNotNull(
            capacity?.trim()?.takeIf { it.isNotEmpty() },
            wiredCharging?.let { head(it, ',') },
        ).takeIf { it.isNotEmpty() }?.joinToString(" · ")

    fun protectionLine(ipRating: String?): String? {
        val raw = ipRating?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val token = raw.substringBefore(' ')
        return if (token.startsWith("IP", ignoreCase = true)) token else raw
    }

    private fun head(value: String?, delimiter: Char): String? =
        value?.substringBefore(delimiter)?.trim()?.takeIf { it.isNotEmpty() }
}

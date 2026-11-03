package de.romerlap.einkaufsliste

data class DraftItem(
    val name: String,
    val detail: String? = null,
    val retailHint: String? = null
)

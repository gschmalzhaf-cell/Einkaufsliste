package de.romerlap.einkaufsliste

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.romerlap.einkaufsliste.data.AppDatabase
import de.romerlap.einkaufsliste.data.ShoppingItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ListViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.get(application).itemDao()

    val items: StateFlow<List<ShoppingItem>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(name: String, detail: String? = null, retailHint: String? = null, tag: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val cleanDetail = detail?.trim()?.ifEmpty { null }
        val cleanHint = retailHint?.trim()?.ifEmpty { null } ?: RetailCatalog.findHint(trimmed)
        val cleanTag = tag?.trim()?.ifEmpty { null }
        viewModelScope.launch {
            upsertSmart(
                DraftItem(
                    name = trimmed,
                    detail = cleanDetail,
                    retailHint = cleanHint,
                    tag = cleanTag
                ),
                currentSnapshot = items.value
            )
        }
    }

    fun addAll(drafts: List<DraftItem>) {
        val cleaned = drafts
            .map {
                val n = it.name.trim()
                it.copy(
                    name = n,
                    detail = it.detail?.trim()?.ifEmpty { null },
                    retailHint = it.retailHint?.trim()?.ifEmpty { null } ?: RetailCatalog.findHint(n),
                    tag = it.tag?.trim()?.ifEmpty { null }
                )
            }
            .filter { it.name.isNotEmpty() }
        if (cleaned.isEmpty()) return

        // Duplikate innerhalb der neuen Auswahl vorab zusammenführen
        val mergedDrafts = linkedMapOf<String, DraftItem>()
        for (draft in cleaned) {
            val key = QuantityMerger.normalizeName(draft.name)
            val prev = mergedDrafts[key]
            mergedDrafts[key] = if (prev == null) {
                draft
            } else {
                prev.copy(
                    detail = QuantityMerger.merge(prev.detail, draft.detail),
                    retailHint = prev.retailHint ?: draft.retailHint,
                    tag = prev.tag ?: draft.tag
                )
            }
        }

        viewModelScope.launch {
            val snapshot = items.value.toMutableList()
            for (draft in mergedDrafts.values) {
                val updated = upsertSmart(draft, snapshot)
                val idx = snapshot.indexOfFirst { it.id == updated.id && updated.id != 0L }
                if (idx >= 0) snapshot[idx] = updated else snapshot += updated
            }
        }
    }

    /**
     * Intelligentes Einfügen:
     * - Offenes Duplikat vorhanden -> Mengen werden addiert (z. B. 200 g + 300 g = 500 g).
     * - Erledigtes Duplikat vorhanden -> Eintrag wird wieder geöffnet mit neuer Menge.
     * - Sonst -> neuer Eintrag.
     */
    private suspend fun upsertSmart(draft: DraftItem, currentSnapshot: List<ShoppingItem>): ShoppingItem {
        val key = QuantityMerger.normalizeName(draft.name)
        val openMatch = currentSnapshot.firstOrNull { !it.isChecked && QuantityMerger.normalizeName(it.name) == key }
        if (openMatch != null) {
            val merged = openMatch.copy(
                detail = QuantityMerger.merge(openMatch.detail, draft.detail),
                retailHint = draft.retailHint ?: openMatch.retailHint,
                tag = openMatch.tag ?: draft.tag
            )
            dao.update(merged)
            return merged
        }
        val doneMatch = currentSnapshot.firstOrNull { it.isChecked && QuantityMerger.normalizeName(it.name) == key }
        if (doneMatch != null) {
            val reactivated = doneMatch.copy(
                isChecked = false,
                detail = draft.detail ?: doneMatch.detail,
                retailHint = draft.retailHint ?: doneMatch.retailHint,
                tag = draft.tag ?: doneMatch.tag,
                createdAt = System.currentTimeMillis()
            )
            dao.update(reactivated)
            return reactivated
        }
        val created = ShoppingItem(
            name = draft.name,
            detail = draft.detail,
            retailHint = draft.retailHint,
            tag = draft.tag
        )
        dao.insert(created)
        return created
    }

    fun toggle(item: ShoppingItem) {
        viewModelScope.launch { dao.update(item.copy(isChecked = !item.isChecked)) }
    }

    fun update(item: ShoppingItem, name: String, detail: String? = null, retailHint: String? = null, tag: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            dao.update(
                item.copy(
                    name = trimmed,
                    detail = detail?.trim()?.ifEmpty { null },
                    retailHint = retailHint?.trim()?.ifEmpty { null },
                    tag = tag?.trim()?.ifEmpty { null }
                )
            )
        }
    }

    fun delete(item: ShoppingItem) {
        viewModelScope.launch { dao.delete(item) }
    }

    fun deleteChecked() {
        viewModelScope.launch { dao.deleteChecked() }
    }
}

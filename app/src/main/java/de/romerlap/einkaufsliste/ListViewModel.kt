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
        insert(
            ShoppingItem(
                name = trimmed,
                detail = detail?.trim()?.ifEmpty { null },
                retailHint = retailHint?.trim()?.ifEmpty { null },
                tag = tag?.trim()?.ifEmpty { null }
            )
        )
    }

    fun addAll(drafts: List<DraftItem>) {
        val cleaned = drafts.map { it.copy(name = it.name.trim()) }.filter { it.name.isNotEmpty() }
        cleaned.forEach {
            insert(ShoppingItem(name = it.name, detail = it.detail, retailHint = it.retailHint, tag = it.tag))
        }
    }

    fun toggle(item: ShoppingItem) {
        viewModelScope.launch { dao.update(item.copy(isChecked = !item.isChecked)) }
    }

    fun delete(item: ShoppingItem) {
        viewModelScope.launch { dao.delete(item) }
    }

    fun deleteChecked() {
        viewModelScope.launch { dao.deleteChecked() }
    }

    private fun insert(item: ShoppingItem) {
        viewModelScope.launch { dao.insert(item) }
    }
}

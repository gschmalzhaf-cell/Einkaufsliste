package de.romerlap.einkaufsliste.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val detail: String? = null,
    val isChecked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

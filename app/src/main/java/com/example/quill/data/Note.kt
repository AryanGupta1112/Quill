package com.example.quill.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    // We’ll store formatted content as HTML (easy to save/restore spans)
    val contentHtml: String,
    // Comma-separated tags: e.g., "work, ideas, todo"
    val tagsCsv: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

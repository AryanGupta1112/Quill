package com.example.quill.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.quill.data.AppDatabase
import com.example.quill.data.Note
import com.example.quill.data.NoteRepository
import kotlinx.coroutines.launch

class NoteViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = NoteRepository(AppDatabase.get(app).noteDao())

    private val query = MutableLiveData<String?>(null)

    // Use KTX switchMap instead of Transformations
    val notes: LiveData<List<Note>> = query.switchMap { q ->
        if (q.isNullOrBlank()) repo.observeAll() else repo.search(q.trim())
    }

    fun setQuery(q: String?) { query.value = q }

    fun saveNote(id: Long?, title: String, html: String, tagsCsv: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val note = if (id != null && id != 0L) {
                val existing = repo.get(id)
                if (existing != null) {
                    existing.copy(
                        title = title,
                        contentHtml = html,
                        tagsCsv = tagsCsv,
                        updatedAt = now
                    )
                } else {
                    Note(title = title, contentHtml = html, tagsCsv = tagsCsv, updatedAt = now)
                }
            } else {
                Note(title = title, contentHtml = html, tagsCsv = tagsCsv, updatedAt = now)
            }
            repo.upsert(note)
        }
    }

    fun delete(note: Note) {
        viewModelScope.launch { repo.delete(note) }
    }
}

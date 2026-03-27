package com.example.quill

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quill.data.Note
import com.example.quill.databinding.ActivityMainBinding
import com.example.quill.ui.NoteViewModel
import com.example.quill.ui.NotesAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private val vm: NoteViewModel by viewModels()
    private lateinit var adapter: NotesAdapter

    private var cached: List<Note> = emptyList()
    private var sortMode: SortMode = SortMode.NEWEST

    enum class SortMode { NEWEST, OLDEST, TITLE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        setSupportActionBar(b.topAppBar)

        // Toolbar menu actions (Sort + Dark mode)
        b.topAppBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_sort -> {
                    showSortOptions()
                    true
                }
                R.id.action_dark -> {
                    toggleDarkMode()
                    true
                }
                else -> false
            }
        }

        // Recycler
        adapter = NotesAdapter(
            onClick = { note -> openEditor(note.id) },
            onLongClick = { note -> confirmDelete(note) }
        )
        b.rvNotes.layoutManager = LinearLayoutManager(this)
        b.rvNotes.adapter = adapter

        // Search
        b.etSearch.doAfterTextChanged { vm.setQuery(it?.toString()) }

        // Observe data
        vm.notes.observe(this) { list ->
            cached = list ?: emptyList()
            applyCurrentSort()
            b.topAppBar.subtitle = if (cached.isEmpty()) "No notes yet" else "${cached.size} notes"
        }

        // Add
        b.fabAdd.setOnClickListener { openEditor(null) }
    }

    private fun openEditor(noteId: Long?) {
        val i = Intent(this, EditorActivity::class.java)
        if (noteId != null) i.putExtra("noteId", noteId)
        startActivity(i)
    }

    private fun confirmDelete(note: Note) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete note?")
            .setMessage("“${note.title.ifBlank { "Untitled" }}” will be deleted.")
            .setPositiveButton("Delete") { _, _ -> vm.delete(note) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSortOptions() {
        val options = arrayOf("Newest first", "Oldest first", "Title A–Z")
        MaterialAlertDialogBuilder(this)
            .setTitle("Sort notes")
            .setItems(options) { _, which ->
                sortMode = when (which) {
                    0 -> SortMode.NEWEST
                    1 -> SortMode.OLDEST
                    else -> SortMode.TITLE
                }
                applyCurrentSort()
            }.show()
    }

    private fun applyCurrentSort() {
        val sorted = when (sortMode) {
            SortMode.NEWEST -> cached.sortedByDescending { it.updatedAt }
            SortMode.OLDEST -> cached.sortedBy { it.updatedAt }
            SortMode.TITLE  -> cached.sortedBy { it.title.lowercase() }
        }
        adapter.submitList(sorted)
    }

    private fun toggleDarkMode() {
        val mode = AppCompatDelegate.getDefaultNightMode()
        val next = if (mode == AppCompatDelegate.MODE_NIGHT_YES)
            AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
        AppCompatDelegate.setDefaultNightMode(next)
    }
}

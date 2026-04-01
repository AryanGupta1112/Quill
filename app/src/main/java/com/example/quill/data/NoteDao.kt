package com.example.quill.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeAll(): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: Note): Long

    @Delete
    suspend fun delete(note: Note)

    // Simple search on title OR tags
    @Query("SELECT * FROM notes WHERE title LIKE '%' || :q || '%' OR tagsCsv LIKE '%' || :q || '%' ORDER BY updatedAt DESC")
    fun search(q: String): LiveData<List<Note>>
}

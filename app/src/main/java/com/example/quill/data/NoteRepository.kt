package com.example.quill.data

class NoteRepository(private val dao: NoteDao) {
    fun observeAll() = dao.observeAll()
    fun search(q: String) = dao.search(q)
    suspend fun get(id: Long) = dao.getById(id)
    suspend fun upsert(note: Note) = dao.upsert(note)
    suspend fun delete(note: Note) = dao.delete(note)
}

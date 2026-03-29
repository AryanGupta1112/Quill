package com.example.quill.ui

import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.quill.data.Note
import com.example.quill.databinding.ItemNoteBinding
import com.google.android.material.chip.Chip

class NotesAdapter(
    private val onClick: (Note) -> Unit,
    private val onLongClick: (Note) -> Unit
) : ListAdapter<Note, NotesAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Note, newItem: Note) = oldItem == newItem
    }

    inner class VH(val b: ItemNoteBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val note = getItem(position)

        // Title
        holder.b.tvTitle.text = note.title.ifBlank { "Untitled" }

        // Preview: HTML -> plain, compact
        val plain = HtmlCompat.fromHtml(
            note.contentHtml.orEmpty(),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        ).toString()
            .replace("\\s+".toRegex(), " ")
            .trim()
        holder.b.tvPreview.text = ellipsize(plain, 180)

        // Chips (read-only, compact)
        val cg = holder.b.chipGroup
        cg.removeAllViews()
        val density = cg.resources.displayMetrics.density

        note.tagsCsv
            .orEmpty()
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { tag ->
                val chip = Chip(cg.context).apply {
                    text = tag
                    isClickable = false
                    isCheckable = false
                    isFocusable = false
                    // use API (px as Float) for compact height without touching private fields
                    chipMinHeight = 36f * density
                }
                cg.addView(chip)
            }

        holder.itemView.contentDescription =
            "Note: ${holder.b.tvTitle.text}. ${holder.b.tvPreview.text}"
        holder.itemView.setOnClickListener { onClick(note) }
        holder.itemView.setOnLongClickListener { onLongClick(note); true }
    }

    private fun ellipsize(text: String, max: Int): CharSequence {
        if (text.length <= max) return text
        val b = SpannableStringBuilder()
        b.append(text.substring(0, max).trimEnd())
        b.append('…')
        return b
    }
}

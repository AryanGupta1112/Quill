package com.example.quill

import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.example.quill.data.AppDatabase
import com.example.quill.databinding.ActivityEditorBinding
import com.example.quill.ui.NoteViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditorActivity : AppCompatActivity() {
    private lateinit var b: ActivityEditorBinding
    private val vm: NoteViewModel by viewModels()
    private var noteId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(b.root)

        setSupportActionBar(b.toolbar)
        // Remove setOnMenuItemClickListener — we’ll use the Activity menu callbacks instead.

        // Bottom-right big Save button
        b.fabSave.setOnClickListener { saveNoteAndClose() }

        // Load note if editing existing
        noteId = intent.getLongExtra("noteId", 0L).takeIf { it != 0L }
        if (noteId != null) {
            CoroutineScope(Dispatchers.Main).launch {
                val dao = AppDatabase.get(this@EditorActivity).noteDao()
                val note = dao.getById(noteId!!)
                note?.let {
                    b.etTitle.setText(it.title)
                    b.etContent.setText(
                        HtmlCompat.fromHtml(it.contentHtml, HtmlCompat.FROM_HTML_MODE_LEGACY)
                    )
                    b.etContent.setSelection(b.etContent.text?.length ?: 0)
                    b.etTags.setText(it.tagsCsv)
                }
            }
        }

        // Formatting
        b.btnBold.setOnClickListener { toggleStyle(android.graphics.Typeface.BOLD) }
        b.btnItalic.setOnClickListener { toggleStyle(android.graphics.Typeface.ITALIC) }
        b.btnUnderline.setOnClickListener { toggleUnderline() }
        b.btnBullet.setOnClickListener { toggleBullet() }

        // Auto-save on back
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveNote()
                finish()
            }
        })
    }

    // === Action bar menu ===
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> { saveNoteAndClose(); true }
            R.id.action_share -> { shareNote(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveNoteAndClose() {
        val didSave = saveNote()
        if (didSave) Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun saveNote(): Boolean {
        val titleTxt = b.etTitle.text?.toString()?.trim().orEmpty()
        val bodySpanned = b.etContent.text ?: SpannableStringBuilder("")
        val bodyHtml = HtmlCompat.toHtml(bodySpanned, HtmlCompat.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL)
        val bodyPlain = HtmlCompat.fromHtml(bodyHtml, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
        val tagsCsv = b.etTags.text?.toString()?.trim().orEmpty()

        val hasAnyContent = titleTxt.isNotEmpty() || bodyPlain.isNotEmpty() || tagsCsv.isNotEmpty()
        if (!hasAnyContent && noteId == null) return false

        val title = titleTxt.ifEmpty { "Untitled" }
        vm.saveNote(noteId, title, bodyHtml, tagsCsv)
        return true
    }

    private fun shareNote() {
        val title = b.etTitle.text?.toString()?.trim().ifNullOrBlank("Untitled")
        val spanned = b.etContent.text ?: SpannableStringBuilder("")
        val html = HtmlCompat.toHtml(spanned, HtmlCompat.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL)
        val plain = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
        val tags = b.etTags.text?.toString()?.trim().orEmpty()

        val body = buildString {
            appendLine(title)
            appendLine()
            if (plain.isNotEmpty()) {
                appendLine(plain)
                appendLine()
            }
            if (tags.isNotEmpty()) appendLine("Tags: $tags")
        }.trim()

        if (body.isEmpty()) {
            Toast.makeText(this, "Nothing to share", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        startActivity(Intent.createChooser(intent, "Share note via"))
    }

    private fun toggleStyle(style: Int) {
        val text = b.etContent.text as Spannable
        val start = b.etContent.selectionStart
        val end = b.etContent.selectionEnd
        if (start == end) return
        text.setSpan(StyleSpan(style), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun toggleUnderline() {
        val text = b.etContent.text as Spannable
        val start = b.etContent.selectionStart
        val end = b.etContent.selectionEnd
        if (start == end) return
        text.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun toggleBullet() {
        val text = b.etContent.text as SpannableStringBuilder
        val start = b.etContent.selectionStart
        val end = b.etContent.selectionEnd
        if (start == -1 || end == -1) return
        val layout = b.etContent.layout ?: return
        val lineStart = layout.getLineForOffset(start)
        val lineEnd = layout.getLineForOffset(end)
        for (line in lineStart..lineEnd) {
            val ls = layout.getLineStart(line)
            val le = layout.getLineEnd(line)
            text.setSpan(BulletSpan(16), ls, le, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        b.etContent.text = text
    }

    private fun String?.ifNullOrBlank(fallback: String) =
        if (this.isNullOrBlank()) fallback else this
}

package com.example.kotlintexteditor

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.amrdeveloper.codeview.CodeView
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.*
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private lateinit var editor: CodeView
    private lateinit var textCount: TextView
    private lateinit var clipboard: ClipboardManager

    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()
    private var isUndoOrRedo = false

    private val openFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? = result.data?.data
                uri?.let {
                    val inputStream = contentResolver.openInputStream(uri)
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val text = reader.readText()
                    reader.close()
                    setEditorText(text)
                }
            }
        }

    private val saveFileLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            uri?.let {
                val outputStream = contentResolver.openOutputStream(uri)
                val writer = OutputStreamWriter(outputStream)
                writer.write(editor.text.toString())
                writer.close()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editor = findViewById(R.id.editor)
        textCount = findViewById(R.id.textCount)
        clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        setupEditor()
        setupButtons()
        updateWordCount()
    }

    private fun setupEditor() {
        // Example Kotlin highlighting rules
        val keywordPattern = Pattern.compile("\\b(fun|val|var|class|object|if|else|for|while|return)\\b")
        val numberPattern = Pattern.compile("\\b\\d+\\b")
        val stringPattern = Pattern.compile("\".*?\"")

        editor.addSyntaxPattern(keywordPattern, getColor(R.color.purple_700))
        editor.addSyntaxPattern(numberPattern, getColor(R.color.teal_700))
        editor.addSyntaxPattern(stringPattern, getColor(R.color.orange))


        // Track text changes for undo/redo
        editor.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (!isUndoOrRedo) {
                    undoStack.push(s.toString())
                    redoStack.clear()
                }
                updateWordCount()
            }
        })
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnNew).setOnClickListener { setEditorText("") }

        findViewById<Button>(R.id.btnOpen).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            openFileLauncher.launch(intent)
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveFileLauncher.launch("untitled.txt")
        }

        findViewById<Button>(R.id.btnUndo).setOnClickListener {
            if (undoStack.isNotEmpty()) {
                val current = editor.text.toString()
                redoStack.push(current)
                val last = undoStack.pop()
                setEditorText(last, false)
            }
        }

        findViewById<Button>(R.id.btnRedo).setOnClickListener {
            if (redoStack.isNotEmpty()) {
                val next = redoStack.pop()
                undoStack.push(editor.text.toString())
                setEditorText(next, false)
            }
        }
    }

    private fun setEditorText(text: String, trackUndo: Boolean = true) {
        if (trackUndo) {
            undoStack.push(editor.text.toString())
            redoStack.clear()
        }
        isUndoOrRedo = true
        editor.setText(text)
        isUndoOrRedo = false
        updateWordCount()
    }

    private fun updateWordCount() {
        val text = editor.text.toString()
        val words = if (text.isBlank()) 0 else text.trim().split("\\s+".toRegex()).size
        val chars = text.length
        textCount.text = "Words: $words | Characters: $chars"
    }
}

package com.customboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class CustomKeyboardService : InputMethodService() {
    private var english = false
    private var symbols = false
    private var shifted = false
    private var panel = ""
    private lateinit var root: LinearLayout
    private val prefs by lazy { getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE) }
    private var russianWords: List<String> = emptyList()
    private var russianByFirst: Map<Char, List<String>> = emptyMap()
    private var russianByPrefix: Map<String, List<String>> = emptyMap()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun settingInt(key: String, fallback: Int) = prefs.getInt(key, fallback)
    private fun settingBool(key: String, fallback: Boolean) = prefs.getBoolean(key, fallback)
    private fun settingString(key: String, fallback: String) = prefs.getString(key, fallback) ?: fallback

    private fun colors(): Triple<Int, Int, Int> = when (settingString("theme", "Black")) {
        "White" -> Triple(Color.rgb(242,242,246), Color.WHITE, Color.rgb(218,218,226))
        "Glass" -> Triple(Color.rgb(20,20,25), Color.rgb(54,54,63), Color.rgb(86,86,100))
        else -> Triple(Color.rgb(13,13,16), Color.rgb(43,43,49), Color.rgb(66,66,76))
    }

    override fun onCreateInputView(): View {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(5), dp(4), dp(5))
        }
        loadRussianDictionary()
        render()
        return root
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        if (!restarting) {
            shifted = settingBool("auto_caps", true) &&
                (attribute?.inputType?.and(android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0)
        }
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (::root.isInitialized && panel.isEmpty()) render()
    }

    private fun loadRussianDictionary() {
        if (russianWords.isNotEmpty()) return
        val words = assets.open("words_ru.txt").bufferedReader().useLines { lines ->
            lines.map { it.trim().lowercase() }
                .filter { it.length >= 2 && it.all { ch -> ch in 'а'..'я' || ch == 'ё' } }
                .distinct().toList()
        }
        russianWords = words
        russianByFirst = words.groupBy { it.first() }
        russianByPrefix = words.groupBy { it.take(2) }
    }

    private val englishWords = listOf(
        "about","above","after","again","all","also","always","amazing","and","any","are","around",
        "awesome","back","because","before","best","better","between","big","bro","but","by","can",
        "cannot","change","chat","check","clean","come","cool","could","day","different","do","does",
        "done","down","easy","even","every","everything","example","feel","few","find","first","for",
        "friend","from","fun","game","games","get","getting","give","go","good","great","guys","had",
        "has","have","he","hello","help","here","hey","hi","home","hope","how","I","idea","if","important",
        "in","into","is","it","its","just","keep","keyboard","kind","know","later","let","like","little",
        "lol","look","love","make","many","maybe","me","more","much","my","need","never","new","nice",
        "no","not","now","of","off","okay","old","on","one","only","or","other","our","out","over",
        "people","phone","please","pretty","probably","really","right","same","see","should","since",
        "so","some","something","sorry","still","sure","take","tell","than","thanks","that","the","their",
        "them","then","there","these","they","thing","think","this","those","time","to","today","together",
        "tomorrow","too","try","two","up","use","very","want","was","way","we","well","what","when",
        "where","which","who","why","will","with","work","world","would","yeah","yes","you","your"
    )

    private fun currentWord(): String {
        val before = currentInputConnection?.getTextBeforeCursor(64, 0)?.toString().orEmpty()
        return before.takeLastWhile { it.isLetter() }
    }

    private fun editDistance(a: String, b: String): Int {
        if (abs(a.length - b.length) > 2) return 3
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            d[i][j] = minOf(d[i-1][j] + 1, d[i][j-1] + 1, d[i-1][j-1] + cost)
            if (i > 1 && j > 1 && a[i-1] == b[j-2] && a[i-2] == b[j-1])
                d[i][j] = minOf(d[i][j], d[i-2][j-2] + 1)
        }
        return d[a.length][b.length]
    }

    private fun suggestionsFor(word: String): List<String> {
        val typed = word.lowercase()
        if (typed.length < 2) return emptyList()
        val source = if (english) englishWords else russianByFirst[typed.first()].orEmpty()
        val completions = if (english) source.filter { it.startsWith(typed) && it != typed }
            .sortedBy { it.length }
        else russianByPrefix[typed.take(2)].orEmpty().filter { it.startsWith(typed) && it != typed }
        val maxDistance = if (typed.length >= 6) 2 else 1
        val corrections = source.asSequence().filter {
            it != typed && !it.startsWith(typed) && abs(it.length - typed.length) <= maxDistance &&
                editDistance(typed, it) <= maxDistance
        }.sortedWith(compareBy<String>({ editDistance(typed, it) }, { abs(it.length - typed.length) }, { it.length }))
            .take(5).toList()
        return (completions + corrections).distinct().take(5)
    }

    private fun rememberClipboard(text: String) {
        if (text.isBlank()) return
        val old = prefs.getString("clipboard_history", "").orEmpty().split("\n").filter { it.isNotBlank() }
        val next = (listOf(text.take(500)) + old.filter { it != text }).take(20)
        prefs.edit().putString("clipboard_history", next.joinToString("\n")).apply()
    }

    private fun clipboardItems(): List<String> =
        prefs.getString("clipboard_history", "").orEmpty().split("\n").filter { it.isNotBlank() }

    private fun phrases(): List<String> =
        prefs.getString("quick_phrases", "Привет!\nСпасибо!\nСейчас отвечу\nКак дела?\nОкей, понял").orEmpty()
            .split("\n").filter { it.isNotBlank() }

    private fun styleText(view: TextView, size: Float, color: Int, bold: Boolean = false) {
        view.textSize = size
        view.setTextColor(color)
        view.gravity = Gravity.CENTER
        if (bold) view.typeface = Typeface.DEFAULT_BOLD
    }

    private fun key(label: String, value: String, weight: Float = 1f, height: Int = settingInt("key_height", 44), size: Float = settingInt("text_size", 18).toFloat()) : TextView {
        val (background, normal, special) = colors()
        val whiteTheme = settingString("theme", "Black") == "White"
        return TextView(this).apply {
            text = label
            styleText(this, size, if (whiteTheme) Color.rgb(25,25,30) else Color.WHITE)
            this.background = GradientDrawable().apply {
                cornerRadius = dp(9).toFloat()
                setColor(when (value) {
                    "↵" -> Color.rgb(76,105,245)
                    "⇧","⌫","?123","ABC","🌐","📋","☺","⋯" -> special
                    else -> normal
                })
            }
            setPadding(dp(1), 0, dp(1), 0)
            setOnClickListener { feedback(); handle(value) }
            if (value == "⌫") {
                setOnLongClickListener {
                    val ic = currentInputConnection
                    if (ic != null) {
                        val count = currentWord().length
                        ic.deleteSurroundingText(if (count > 0) count else 8, 0)
                        render()
                    }
                    true
                }
            }
            val lp = LinearLayout.LayoutParams(0, dp(height), weight)
            lp.setMargins(dp(2), dp(2), dp(2), dp(2))
            layoutParams = lp
        }
    }

    private fun addRow(keys: List<Triple<String,String,Float>>, height: Int = settingInt("key_height",44), size: Float = settingInt("text_size",18).toFloat()) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        keys.forEach { row.addView(key(it.first, it.second, it.third, height, size)) }
        root.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(height + 4)))
    }

    private fun addToolbar() {
        val (_, _, _) = colors()
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val brand = TextView(this).apply {
            text = "CB"; styleText(this, 14f, if (settingString("theme","Black") == "White") Color.BLACK else Color.WHITE, true)
        }
        bar.addView(brand, LinearLayout.LayoutParams(dp(32), dp(30)))
        suggestionsFor(currentWord()).forEach { candidate ->
            val item = TextView(this).apply {
                text = candidate
                styleText(this, 13f, if (settingString("theme","Black") == "White") Color.BLACK else Color.WHITE)
                setSingleLine(true)
                setOnClickListener {
                    val ic = currentInputConnection ?: return@setOnClickListener
                    val typed = currentWord()
                    if (typed.isNotEmpty()) ic.deleteSurroundingText(typed.length, 0)
                    ic.commitText(candidate + " ", 1)
                    render()
                }
            }
            bar.addView(item, LinearLayout.LayoutParams(0, dp(30), 1f))
        }
        val lang = TextView(this).apply {
            text = if (english) "EN" else "RU"
            styleText(this, 12f, if (settingString("theme","Black") == "White") Color.DKGRAY else Color.LTGRAY, true)
            setOnClickListener { english = !english; render() }
        }
        bar.addView(lang, LinearLayout.LayoutParams(dp(32), dp(30)))
        root.addView(bar)
    }

    private fun addEditToolbar() {
        val white = settingString("theme", "Black") == "White"
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        fun action(label: String, actionId: Int) {
            val item = TextView(this).apply {
                text = label
                styleText(this, 11f, if (white) Color.DKGRAY else Color.LTGRAY, true)
                setOnClickListener {
                    currentInputConnection?.performContextMenuAction(actionId)
                }
            }
            bar.addView(item, LinearLayout.LayoutParams(0, dp(28), 1f))
        }
        action("Выделить всё", android.R.id.selectAll)
        action("Копировать", android.R.id.copy)
        action("Вставить", android.R.id.paste)
        root.addView(bar, LinearLayout.LayoutParams(-1, dp(28)))
    }

    private fun renderPanel() {
        val white = settingString("theme","Black") == "White"
        val title = TextView(this).apply {
            text = when(panel) { "clipboard" -> "БУФЕР ОБМЕНА"; "emoji" -> "ЭМОДЗИ"; else -> "БЫСТРЫЕ ФРАЗЫ" }
            styleText(this, 13f, if (white) Color.DKGRAY else Color.WHITE, true)
        }
        root.addView(title, LinearLayout.LayoutParams(-1, dp(34)))
        val items = when(panel) {
            "clipboard" -> clipboardItems()
            "emoji" -> listOf("😀","😂","🥹","😍","🔥","❤️","👍","😭","💀","✨","😎","🤝","🙏","🎉","🤣","😊","🤔","🙃","💯","🫡")
            else -> phrases()
        }
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        items.chunked(3).forEach { group ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            group.forEach { item ->
                val cell = TextView(this).apply {
                    text = item.take(36)
                    styleText(this, if (panel == "emoji") 25f else 14f, if (white) Color.BLACK else Color.WHITE)
                    setOnClickListener {
                        currentInputConnection?.commitText(item + if (panel == "emoji") "" else " ", 1)
                        panel = ""; render()
                    }
                }
                row.addView(cell, LinearLayout.LayoutParams(0, dp(42), 1f))
            }
            wrap.addView(row)
        }
        root.addView(wrap, LinearLayout.LayoutParams(-1, 0, 1f))
        addRow(listOf(Triple("⌨","close",1f), Triple("Очистить","clear",2f)), 42, 13f)
    }

    private fun render() {
        if (!::root.isInitialized) return
        val (background, _, _) = colors()
        root.removeAllViews()
        root.setBackgroundColor(background)
        root.minimumHeight = dp(settingInt("keyboard_height", 276))
        if (panel.isNotEmpty()) { renderPanel(); return }
        addToolbar()
        addEditToolbar()
        if (symbols) {
            addRow(("1234567890").map { Triple(it.toString(),it.toString(),1f) }, 38, 17f)
            addRow(listOf("@","#","\$","%","&","-","+","(",")","/").map { Triple(it,it,1f) }, 44, 17f)
            addRow(listOf("*","\"","'",":",";","!","?").map { Triple(it,it,1f) }, 44, 17f)
            addRow(listOf(Triple("ABC","ABC",1.2f),Triple("⌫","⌫",1.2f),Triple("🌐","🌐",1f),Triple("Space","␣",3f),Triple("↵","↵",1.2f)), 48, 14f)
            return
        }
        if (settingBool("number_row", true))
            addRow("1234567890".map { Triple(it.toString(),it.toString(),1f) }, 34, 14f)
        val first = if (english) "qwertyuiop".map { it.toString() } else listOf("й","ц","у","к","е","н","г","ш","щ","з","х")
        val second = if (english) "asdfghjkl".map { it.toString() } else listOf("ф","ы","в","а","п","р","о","л","д","ж","э")
        val third = if (english) "zxcvbnm".map { it.toString() } else listOf("я","ч","с","м","и","т","ь","б","ю")
        addRow(first.map { Triple(if(shifted) it.uppercase() else it,it,1f) })
        addRow(second.map { Triple(if(shifted) it.uppercase() else it,it,1f) })
        val bottom = mutableListOf(Triple("⇧","⇧",1.3f))
        third.forEach { bottom.add(Triple(if(shifted) it.uppercase() else it,it,1f)) }
        bottom.add(Triple("⌫","⌫",1.3f))
        addRow(bottom)
        addRow(listOf(
            Triple("?123","?123",1.15f), Triple("📋","clipboard",0.9f), Triple("☺","emoji",0.9f),
            Triple("🌐","🌐",0.9f), Triple(if(english) "English" else "Русский","␣",3.1f),
            Triple("⋯","phrases",0.9f), Triple("↵","↵",1.1f)
        ), 48, 14f)
    }

    private fun feedback() {
        if (settingBool("vibration", true)) {
            try {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
                else @Suppress("DEPRECATION") vibrator.vibrate(12)
            } catch (_: Exception) {}
        }
        if (settingBool("sound", false)) {
            try { (getSystemService(Context.AUDIO_SERVICE) as AudioManager).playSoundEffect(AudioManager.FX_KEY_CLICK) } catch (_: Exception) {}
        }
    }

    private fun handle(value: String) {
        val ic = currentInputConnection ?: return
        when(value) {
            "⌫" -> ic.deleteSurroundingText(1,0)
            "↵" -> {
                val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
                if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) ic.performEditorAction(action)
                else ic.commitText("\n",1)
            }
            "⇧" -> shifted = !shifted
            "?123" -> symbols = true
            "ABC" -> symbols = false
            "🌐" -> english = !english
            "␣" -> { ic.commitText(" ",1); if (settingBool("auto_caps",true)) shifted = false }
            "clipboard" -> {
                val clip = (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
                if (clip != null && clip.itemCount > 0) rememberClipboard(clip.getItemAt(0).coerceToText(this).toString())
                panel = "clipboard"
            }
            "emoji" -> panel = "emoji"
            "phrases" -> panel = "phrases"
            "close" -> panel = ""
            "clear" -> { prefs.edit().remove("clipboard_history").apply(); panel = "clipboard" }
            else -> {
                ic.commitText(if (shifted) value.uppercase() else value,1)
                if (shifted) shifted = false
            }
        }
        render()
    }
}

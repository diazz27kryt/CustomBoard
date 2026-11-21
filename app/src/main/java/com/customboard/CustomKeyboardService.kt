package com.customboard

import android.inputmethodservice.InputMethodService
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView

class CustomKeyboardService : InputMethodService() {
    private var english = false
    private var symbols = false
    private var shifted = false
    private lateinit var root: LinearLayout

    private val bg = Color.rgb(13, 13, 16)
    private val keyColor = Color.rgb(43, 43, 49)
    private val specialColor = Color.rgb(66, 66, 76)
    private val accentColor = Color.rgb(76, 105, 245)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onCreateInputView(): View {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(5), dp(4), dp(5))
            setBackgroundColor(bg)
            minimumHeight = dp(276)
        }
        render()
        return root
    }

    private fun makeKey(label: String, key: String, weight: Float = 1f, size: Float = 18f): TextView {
        return TextView(this).apply {
            text = label
            textSize = size
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            background = GradientDrawable().apply {
                cornerRadius = dp(7).toFloat()
                setColor(
                    when (key) {
                        "↵" -> accentColor
                        "⇧", "⌫", "?123", "ABC", "🌐" -> specialColor
                        else -> keyColor
                    }
                )
            }
            setPadding(dp(1), 0, dp(1), 0)
            setOnClickListener { handle(key) }
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight)
            lp.setMargins(dp(2), dp(2), dp(2), dp(2))
            layoutParams = lp
        }
    }

    private fun addRow(keys: List<Triple<String, String, Float>>, height: Int, textSize: Float = 18f) {
        val line = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        keys.forEach { (label, key, weight) ->
            line.addView(makeKey(label, key, weight, textSize))
        }
        root.addView(
            line,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(height))
        )
    }

    private val russianWords = listOf(
        "привет", "приветик", "привычка", "природа", "пример", "просто", "проблема", "проверка",
        "почему", "пока", "потом", "после", "пожалуйста", "спасибо", "сегодня", "завтра",
        "сейчас", "хорошо", "хороший", "хорошая", "можно", "нужно", "будет", "было",
        "есть", "если", "когда", "который", "которая", "которые", "чтобы", "тогда",
        "делать", "дело", "дом", "друг", "друзья", "игра", "играть", "клавиатура",
        "телефон", "компьютер", "работает", "работать", "люблю", "хочу", "могу",
        "давай", "ладно", "ага", "нет", "да", "мой", "моя", "мне", "тебе", "твой",
        "это", "этот", "эта", "там", "тут", "очень", "больше", "меньше", "новый",
        "новая", "красиво", "удобно", "быстро", "медленно", "круто", "классно"
    )
    private val englishWords = listOf(
        "hello", "help", "hey", "thanks", "thank", "there", "their", "then", "this",
        "that", "these", "those", "the", "they", "them", "today", "tomorrow", "time",
        "good", "great", "going", "game", "games", "keyboard", "phone", "computer",
        "work", "working", "world", "would", "what", "when", "where", "why", "which",
        "with", "will", "want", "was", "are", "and", "about", "after", "again",
        "because", "before", "can", "could", "cool", "done", "don't", "for", "from",
        "have", "here", "how", "just", "know", "like", "love", "make", "more",
        "much", "nice", "now", "okay", "one", "please", "really", "see", "some",
        "something", "sure", "you", "your", "yes"
    )

    private fun currentWord(): String {
        val before = currentInputConnection?.getTextBeforeCursor(48, 0)?.toString().orEmpty()
        return before.takeLastWhile { it.isLetter() }
    }

    private fun suggestionsFor(word: String): List<String> {
        if (word.isBlank()) return emptyList()
        val dictionary = if (english) englishWords else russianWords
        val prefix = word.lowercase()
        return dictionary.asSequence()
            .filter { it.startsWith(prefix) && it != prefix }
            .distinct()
            .take(3)
            .toList()
    }

    private fun addToolbar() {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(5), 0, dp(5), 0)
        }
        val brand = TextView(this).apply {
            text = "CB"
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        bar.addView(brand, LinearLayout.LayoutParams(dp(34), ViewGroup.LayoutParams.MATCH_PARENT))

        val word = currentWord()
        val suggestions = suggestionsFor(word)
        suggestions.forEach { candidate ->
            val item = TextView(this).apply {
                text = candidate
                textSize = 14f
                setTextColor(Color.rgb(235, 235, 242))
                gravity = Gravity.CENTER
                setSingleLine(true)
                setOnClickListener {
                    val ic = currentInputConnection ?: return@setOnClickListener
                    val typed = currentWord()
                    if (typed.isNotEmpty()) ic.deleteSurroundingText(typed.length, 0)
                    ic.commitText(candidate + " ", 1)
                    render()
                }
            }
            bar.addView(item, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        }
        if (suggestions.isEmpty()) {
            val hint = TextView(this).apply {
                text = if (english) "Type to see suggestions" else "Начните вводить слово"
                textSize = 11f
                setTextColor(Color.rgb(125, 125, 140))
                gravity = Gravity.CENTER
            }
            bar.addView(hint, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        }
        val lang = TextView(this).apply {
            text = if (english) "EN" else "RU"
            textSize = 12f
            setTextColor(Color.rgb(165, 165, 180))
            gravity = Gravity.CENTER
            setOnClickListener { english = !english; render() }
        }
        bar.addView(lang, LinearLayout.LayoutParams(dp(34), ViewGroup.LayoutParams.MATCH_PARENT))
        root.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(30)))
    }

    private fun render() {
        if (!::root.isInitialized) return
        root.removeAllViews()
        addToolbar()

        if (symbols) {
            addRow(listOf("1","2","3","4","5","6","7","8","9","0").map { Triple(it, it, 1f) }, 38, 17f)
            addRow(listOf("@","#","\$","%","&","-","+","(",")","/").map { Triple(it, it, 1f) }, 44, 17f)
            addRow(listOf("*","\"","'",":",";","!","?").map { Triple(it, it, 1f) }, 44, 17f)
            addRow(listOf(Triple("ABC", "ABC", 1.35f), Triple("⌫", "⌫", 1.35f), Triple("🌐", "🌐", 1.2f), Triple("Пробел", "␣", 4f), Triple("↵", "↵", 1.35f)), 48, 15f)
            return
        }

        addRow(("1234567890").map { Triple(it.toString(), it.toString(), 1f) }, 34, 14f)

        val first = if (english) listOf("q","w","e","r","t","y","u","i","o","p") else listOf("й","ц","у","к","е","н","г","ш","щ","з","х")
        val second = if (english) listOf("a","s","d","f","g","h","j","k","l") else listOf("ф","ы","в","а","п","р","о","л","д","ж","э")
        val third = if (english) listOf("z","x","c","v","b","n","m") else listOf("я","ч","с","м","и","т","ь","б","ю")

        addRow(first.map { Triple(if (shifted) it.uppercase() else it, it, 1f) }, 44, 18f)
        addRow(second.map { Triple(if (shifted) it.uppercase() else it, it, 1f) }, 44, 18f)
        val bottomLetters = mutableListOf(Triple("⇧", "⇧", 1.35f))
        third.forEach { bottomLetters.add(Triple(if (shifted) it.uppercase() else it, it, 1f)) }
        bottomLetters.add(Triple("⌫", "⌫", 1.35f))
        addRow(bottomLetters, 44, 18f)

        addRow(
            listOf(
                Triple("?123", "?123", 1.35f),
                Triple(",", ",", 0.8f),
                Triple("🌐", "🌐", 1.05f),
                Triple(if (english) "English" else "Русский", "␣", 3.7f),
                Triple(".", ".", 0.8f),
                Triple("↵", "↵", 1.25f)
            ),
            48,
            14f
        )
    }

    private fun handle(key: String) {
        val ic = currentInputConnection ?: return
        when (key) {
            "" -> Unit
            "⌫" -> ic.deleteSurroundingText(1, 0)
            "↵" -> {
                val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
                    ?: EditorInfo.IME_ACTION_NONE
                if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(action)
                } else {
                    ic.commitText("\n", 1)
                }
            }
            "⇧" -> shifted = !shifted
            "?123" -> symbols = true
            "ABC" -> symbols = false
            "🌐" -> english = !english
            "␣" -> ic.commitText(" ", 1)
            ",", "." -> ic.commitText(key, 1)
            else -> {
                ic.commitText(if (shifted) key.uppercase() else key, 1)
                if (shifted) shifted = false
            }
        }
        render()
    }
}

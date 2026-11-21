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

    override fun onCreateInputView(): View {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(5, 8, 5, 8)
            setBackgroundColor(Color.rgb(12, 12, 14))
        }
        render()
        return root
    }

    private fun render() {
        root.removeAllViews()
        val rows = if (symbols) listOf(
            listOf("1","2","3","4","5","6","7","8","9","0"),
            listOf("@","#","$","%","&","-","+","(",")","/"),
            listOf("*","\"","'",":",";","!","?"),
            listOf("ABC","⌫","?123","RU/EN","␣","↵")
        ) else {
            val first = if (english) listOf("q","w","e","r","t","y","u","i","o","p") else listOf("й","ц","у","к","е","н","г","ш","щ","з","х")
            val second = if (english) listOf("a","s","d","f","g","h","j","k","l") else listOf("ф","ы","в","а","п","р","о","л","д","ж","э")
            val third = if (english) listOf("⇧","z","x","c","v","b","n","m","⌫") else listOf("⇧","я","ч","с","м","и","т","ь","б","ю","⌫")
            listOf(first, second, third, listOf("?123",",","RU/EN","␣",".","↵"))
        }
        rows.forEachIndexed { index, row ->
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            row.forEach { key ->
                val label = when {
                    key == "␣" -> if (english) "English" else "Русский"
                    key.length == 1 && shifted && key[0].isLetter() -> key.uppercase()
                    else -> key
                }
                val tv = TextView(this).apply {
                    text = label
                    textSize = if (key == "␣") 13f else 18f
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                    background = GradientDrawable().apply {
                        cornerRadius = 9f
                        setColor(if (key in listOf("↵","⌫","⇧","?123","ABC","RU/EN")) Color.rgb(65,65,70) else Color.rgb(43,43,48))
                    }
                    setPadding(2, 0, 2, 0)
                    setOnClickListener { handle(key) }
                }
                val weight = if (key == "␣") 4f else if (key == "RU/EN" || key == "?123" || key == "ABC") 1.35f else 1f
                val lp = LinearLayout.LayoutParams(0, if (index == rows.lastIndex) 52 else 48, weight)
                lp.setMargins(2, 3, 2, 3)
                line.addView(tv, lp)
            }
            root.addView(line, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    private fun handle(key: String) {
        val ic = currentInputConnection ?: return
        when (key) {
            "⌫" -> ic.deleteSurroundingText(1, 0)
            "↵" -> {
                val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
                if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) ic.performEditorAction(action)
                else ic.commitText("\n", 1)
            }
            "⇧" -> shifted = !shifted
            "?123" -> symbols = true
            "ABC" -> symbols = false
            "RU/EN" -> english = !english
            "␣" -> ic.commitText(" ", 1)
            "," , "." -> ic.commitText(key, 1)
            else -> {
                val value = if (shifted) key.uppercase() else key
                ic.commitText(value, 1)
                if (shifted) shifted = false
            }
        }
        render()
    }
}

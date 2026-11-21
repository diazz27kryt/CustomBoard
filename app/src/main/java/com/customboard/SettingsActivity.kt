package com.customboard

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*
import android.content.Context

class SettingsActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE) }
    private val white = Color.WHITE
    private val muted = Color.rgb(165, 165, 175)
    private val panel = Color.rgb(28, 28, 33)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(26), dp(20), dp(28))
            setBackgroundColor(Color.rgb(10, 10, 13))
        }
        scroll.addView(page)
        setContentView(scroll)

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.custom_board_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        page.addView(logo, LinearLayout.LayoutParams(dp(76), dp(76)))
        page.addView(label("Custom Board", 28f, white, true))
        page.addView(label("Настройки клавиатуры", 14f, muted, false).apply {
            setPadding(0, 0, 0, dp(22))
        })

        section(page, "ВНЕШНИЙ ВИД")
        val themeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Black", "Glass", "White").forEach { theme ->
            val b = Button(this).apply {
                text = theme
                isAllCaps = false
                setTextColor(white)
                setOnClickListener {
                    prefs.edit().putString("theme", theme).apply()
                    Toast.makeText(this@SettingsActivity, "Тема: $theme", Toast.LENGTH_SHORT).show()
                }
            }
            themeRow.addView(b, LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
        page.addView(themeRow)

        section(page, "РАЗМЕР КЛАВИАТУРЫ")
        addSlider(page, "Высота клавиш", "key_height", 38, 58, 44, " dp")
        addSlider(page, "Размер букв", "text_size", 14, 24, 18, " sp")

        section(page, "РАСКЛАДКА И ОТКЛИК")
        addSwitch(page, "Цифровой ряд", "number_row", true)
        addSwitch(page, "Вибрация при нажатии", "vibration", true)
        addSwitch(page, "Звук клавиш", "sound", false)

        section(page, "ДОПОЛНИТЕЛЬНО")
        val reset = Button(this).apply {
            text = "Сбросить настройки"
            isAllCaps = false
            setTextColor(white)
            setOnClickListener {
                prefs.edit().clear().apply()
                recreate()
            }
        }
        page.addView(reset)
        page.addView(label("Custom Board • Alpha 0.2", 12f, muted, false).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(26), 0, 0)
        })
    }

    private fun label(textValue: String, size: Float, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = textValue
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun section(parent: LinearLayout, title: String) {
        parent.addView(label(title, 11f, muted, true).apply {
            setPadding(0, dp(22), 0, dp(9))
        })
    }

    private fun addSwitch(parent: LinearLayout, title: String, key: String, default: Boolean) {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(5), dp(10), dp(5))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(panel)
            }
        }
        row.addView(label(title, 15f, white, false), LinearLayout.LayoutParams(0, dp(52), 1f).apply {
            gravity = Gravity.CENTER_VERTICAL
        })
        val toggle = Switch(this).apply {
            isChecked = prefs.getBoolean(key, default)
            setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean(key, checked).apply() }
        }
        row.addView(toggle)
        parent.addView(row, LinearLayout.LayoutParams(-1, dp(62)).apply { setMargins(0, dp(3), 0, dp(3)) })
    }

    private fun addSlider(parent: LinearLayout, title: String, key: String, min: Int, max: Int, default: Int, suffix: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(panel)
            }
        }
        val value = label("", 13f, muted, false)
        val line = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        line.addView(label(title, 15f, white, false), LinearLayout.LayoutParams(0, -2, 1f))
        line.addView(value)
        card.addView(line)
        val seek = SeekBar(this).apply {
            this.max = max - min
            progress = (prefs.getInt(key, default) - min).coerceIn(0, max)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    val v = min + progress
                    value.text = "$v$suffix"
                    if (fromUser) prefs.edit().putInt(key, v).apply()
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        value.text = "${min + seek.progress}$suffix"
        card.addView(seek)
        parent.addView(card, LinearLayout.LayoutParams(-1, dp(88)).apply { setMargins(0, dp(3), 0, dp(3)) })
    }
}

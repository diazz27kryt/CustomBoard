package com.customboard

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager

class SettingsActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE) }
    private val white = Color.WHITE
    private val muted = Color.rgb(165,165,175)
    private val panelColor = Color.rgb(28,28,33)
    private fun dp(v:Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20),dp(24),dp(20),dp(30))
            setBackgroundColor(Color.rgb(10,10,13))
        }
        scroll.addView(page); setContentView(scroll)
        val logo = ImageView(this).apply { setImageResource(R.drawable.custom_board_logo); scaleType = ImageView.ScaleType.FIT_CENTER }
        page.addView(logo,LinearLayout.LayoutParams(dp(76),dp(76)))
        page.addView(label("Custom Board",28f,white,true))
        page.addView(label("Настройки клавиатуры",14f,muted,false).apply { setPadding(0,0,0,dp(18)) })

        section(page,"ПОДКЛЮЧЕНИЕ КЛАВИАТУРЫ")
        page.addView(label("Включи Custom Board в списке клавиатур Android, затем выбери её.",13f,muted,false).apply {
            setPadding(dp(14),dp(12),dp(14),dp(12))
            background=GradientDrawable().apply { cornerRadius=dp(14).toFloat(); setColor(panelColor) }
        })
        val openSettings = Button(this).apply {
            text="1. Включить Custom Board"; isAllCaps=false
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        }
        page.addView(openSettings,LinearLayout.LayoutParams(-1,dp(50)).apply { setMargins(0,dp(6),0,dp(4)) })
        val chooseKeyboard = Button(this).apply {
            text="2. Выбрать Custom Board"; isAllCaps=false
            setOnClickListener {
                val imm=getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                if(imm.enabledInputMethodList.any { it.packageName==packageName }) {
                    imm.showInputMethodPicker()
                } else {
                    Toast.makeText(this@SettingsActivity,"Сначала включи Custom Board в настройках Android",Toast.LENGTH_LONG).show()
                    startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                }
            }
        }
        page.addView(chooseKeyboard,LinearLayout.LayoutParams(-1,dp(50)).apply { setMargins(0,dp(2),0,dp(12)) })

        section(page,"ВНЕШНИЙ ВИД")
        val themes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Black","Glass","White").forEach { theme ->
            val b = Button(this).apply {
                text = theme; isAllCaps = false; setTextColor(white)
                setOnClickListener { prefs.edit().putString("theme",theme).apply(); Toast.makeText(this@SettingsActivity,"Тема: $theme",Toast.LENGTH_SHORT).show() }
            }
            themes.addView(b,LinearLayout.LayoutParams(0,dp(48),1f).apply { setMargins(dp(3),0,dp(3),0) })
        }
        page.addView(themes)
        section(page,"РАЗМЕР")
        addSlider(page,"Высота клавиатуры","keyboard_height",230,360,276," dp")
        addSlider(page,"Высота клавиш","key_height",36,58,44," dp")
        addSlider(page,"Размер букв","text_size",13,24,18," sp")
        section(page,"РАСКЛАДКА")
        addSwitch(page,"Цифровой ряд","number_row",true)
        addSwitch(page,"Автозаглавная буква","auto_caps",true)
        section(page,"ОТКЛИК")
        addSwitch(page,"Вибрация при нажатии","vibration",true)
        addSwitch(page,"Звук клавиш","sound",false)
        section(page,"БЫСТРЫЕ ФРАЗЫ")
        val phraseInput = EditText(this).apply {
            setText(prefs.getString("quick_phrases","Привет!\nСпасибо!\nСейчас отвечу\nКак дела?\nОкей, понял"))
            setTextColor(white); setHintTextColor(muted); hint = "Одна фраза на строку"
            minLines = 4; gravity = Gravity.TOP; setPadding(dp(12),dp(10),dp(12),dp(10))
            background = GradientDrawable().apply { cornerRadius=dp(12).toFloat(); setColor(panelColor) }
        }
        page.addView(phraseInput,LinearLayout.LayoutParams(-1,dp(130)))
        val savePhrases = Button(this).apply {
            text="Сохранить фразы"; isAllCaps=false
            setOnClickListener { prefs.edit().putString("quick_phrases",phraseInput.text.toString()).apply(); Toast.makeText(this@SettingsActivity,"Фразы сохранены",Toast.LENGTH_SHORT).show() }
        }
        page.addView(savePhrases)
        section(page,"ДАННЫЕ")
        val clear = Button(this).apply {
            text="Очистить историю буфера"; isAllCaps=false
            setOnClickListener { prefs.edit().remove("clipboard_history").apply(); Toast.makeText(this@SettingsActivity,"История очищена",Toast.LENGTH_SHORT).show() }
        }
        page.addView(clear)
        val reset = Button(this).apply {
            text="Сбросить настройки"; isAllCaps=false
            setOnClickListener { prefs.edit().clear().apply(); recreate() }
        }
        page.addView(reset)
        page.addView(label("Custom Board • 1.0.0",12f,muted,false).apply { gravity=Gravity.CENTER; setPadding(0,dp(24),0,0) })
    }

    private fun label(t:String,s:Float,c:Int,b:Boolean)=TextView(this).apply {
        text=t; textSize=s; setTextColor(c); if(b) typeface=Typeface.DEFAULT_BOLD
    }
    private fun section(parent:LinearLayout,t:String) {
        parent.addView(label(t,11f,muted,true).apply { setPadding(0,dp(22),0,dp(9)) })
    }
    private fun addSwitch(parent:LinearLayout,title:String,key:String,default:Boolean) {
        val row=LinearLayout(this).apply {
            gravity=Gravity.CENTER_VERTICAL; setPadding(dp(14),dp(5),dp(10),dp(5))
            background=GradientDrawable().apply { cornerRadius=dp(14).toFloat(); setColor(panelColor) }
        }
        row.addView(label(title,15f,white,false),LinearLayout.LayoutParams(0,dp(52),1f).apply { gravity=Gravity.CENTER_VERTICAL })
        val toggle=Switch(this).apply {
            isChecked=prefs.getBoolean(key,default)
            setOnCheckedChangeListener { _,checked -> prefs.edit().putBoolean(key,checked).apply() }
        }
        row.addView(toggle)
        parent.addView(row,LinearLayout.LayoutParams(-1,dp(62)).apply { setMargins(0,dp(3),0,dp(3)) })
    }
    private fun addSlider(parent:LinearLayout,title:String,key:String,min:Int,max:Int,default:Int,suffix:String) {
        val card=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL; setPadding(dp(14),dp(10),dp(14),dp(8))
            background=GradientDrawable().apply { cornerRadius=dp(14).toFloat(); setColor(panelColor) }
        }
        val value=label("",13f,muted,false)
        val line=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        line.addView(label(title,15f,white,false),LinearLayout.LayoutParams(0,-2,1f)); line.addView(value); card.addView(line)
        val seek=SeekBar(this).apply {
            this.max=max-min
            progress=(prefs.getInt(key,default)-min).coerceIn(0,max-min)
            setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb:SeekBar?,p:Int,fromUser:Boolean) {
                    val v=min+p; value.text="$v$suffix"
                    if(fromUser) prefs.edit().putInt(key,v).apply()
                }
                override fun onStartTrackingTouch(sb:SeekBar?) {}
                override fun onStopTrackingTouch(sb:SeekBar?) {}
            })
        }
        value.text="${min+seek.progress}$suffix"; card.addView(seek)
        parent.addView(card,LinearLayout.LayoutParams(-1,dp(88)).apply { setMargins(0,dp(3),0,dp(3)) })
    }
}

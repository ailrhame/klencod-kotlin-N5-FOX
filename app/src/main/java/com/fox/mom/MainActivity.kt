package com.fox.mom

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val PREFS_NAME = "n5_fox_preferences"
private const val PREF_COLOR = "lens_color_index"
private const val PREF_SIZE = "lens_size_dp"
private const val PREF_OPACITY = "lens_opacity"
private const val PREF_AUTO_COLOR = "lens_auto_color"
private const val PREF_ONBOARDING_DONE = "onboarding_done"

private data class FoxPalette(
    val name: String,
    val primary: Int,
    val secondary: Int
)

private object FoxPalettes {
    val all = listOf(
        FoxPalette("توتي", Color.rgb(255, 83, 145), Color.rgb(255, 190, 218)),
        FoxPalette("سماوي", Color.rgb(42, 180, 255), Color.rgb(177, 235, 255)),
        FoxPalette("نعناعي", Color.rgb(45, 211, 151), Color.rgb(177, 255, 222)),
        FoxPalette("ليموني", Color.rgb(255, 210, 50), Color.rgb(255, 244, 164)),
        FoxPalette("لافندر", Color.rgb(157, 111, 255), Color.rgb(220, 196, 255)),
        FoxPalette("خوخي", Color.rgb(255, 135, 91), Color.rgb(255, 211, 184)),
        FoxPalette("محيطي", Color.rgb(0, 145, 190), Color.rgb(128, 231, 239)),
        FoxPalette("فراولة", Color.rgb(239, 55, 85), Color.rgb(255, 174, 183)),
        FoxPalette("حلوى", Color.rgb(221, 93, 230), Color.rgb(255, 190, 244)),
        FoxPalette("أخضر", Color.rgb(127, 202, 53), Color.rgb(220, 255, 163)),
        FoxPalette("ليلي", Color.rgb(91, 105, 220), Color.rgb(183, 195, 255)),
        FoxPalette("قوس قزح", Color.rgb(255, 105, 180), Color.rgb(93, 205, 255))
    )
}

private object FrancoConverter {
    private val arabicToFranco = mapOf(
        'ا' to "a", 'أ' to "a", 'إ' to "e", 'آ' to "aa",
        'ب' to "b", 'ت' to "t", 'ث' to "th", 'ج' to "j",
        'ح' to "7", 'خ' to "5", 'د' to "d", 'ذ' to "dh",
        'ر' to "r", 'ز' to "z", 'س' to "s", 'ش' to "sh",
        'ص' to "9", 'ض' to "9d", 'ط' to "6", 'ظ' to "6z",
        'ع' to "3", 'غ' to "gh", 'ف' to "f", 'ق' to "q",
        'ك' to "k", 'ل' to "l", 'م' to "m", 'ن' to "n",
        'ه' to "h", 'ة' to "a", 'و' to "w", 'ي' to "y",
        'ى' to "a", 'ء' to "2", 'ئ' to "2", 'ؤ' to "2",
        'چ' to "ch", 'گ' to "g", 'پ' to "p", 'ڤ' to "v"
    )

    private val francoTokens = listOf(
        "9d" to 'ض', "6z" to 'ظ', "sh" to 'ش', "th" to 'ث',
        "kh" to 'خ', "gh" to 'غ', "dh" to 'ذ', "ch" to 'چ'
    )

    private val francoSingle = mapOf(
        'a' to 'ا', 'b' to 'ب', 't' to 'ت', 'j' to 'ج',
        'h' to 'ه', 'd' to 'د', 'r' to 'ر', 'z' to 'ز',
        's' to 'س', 'f' to 'ف', 'q' to 'ق', 'k' to 'ك',
        'l' to 'ل', 'm' to 'م', 'n' to 'ن', 'w' to 'و',
        'y' to 'ي', 'i' to 'ي', 'e' to 'إ', 'o' to 'و',
        'u' to 'و', 'p' to 'پ', 'g' to 'گ', 'v' to 'ڤ',
        'c' to 'ك', '2' to 'ء', '3' to 'ع', '5' to 'خ',
        '6' to 'ط', '7' to 'ح', '8' to 'غ', '9' to 'ص'
    )

    fun hasArabic(value: String): Boolean = value.any { it in '\u0600'..'\u06FF' }

    fun toFranco(input: String): String {
        val result = StringBuilder(input.length * 2)
        for (character in input) {
            when {
                arabicToFranco.containsKey(character) -> result.append(arabicToFranco[character])
                character == '\u0640' -> Unit
                character in '\u064B'..'\u065F' -> Unit
                character == '\u0670' -> Unit
                else -> result.append(character)
            }
        }
        return result.toString()
    }

    fun toArabic(input: String): String {
        val result = StringBuilder(input.length)
        var index = 0
        while (index < input.length) {
            val remaining = input.substring(index)
            val token = francoTokens.firstOrNull { remaining.startsWith(it.first, ignoreCase = true) }
            if (token != null) {
                result.append(token.second)
                index += token.first.length
                continue
            }

            val current = input[index]
            val mapped = francoSingle[current.lowercaseChar()]
            if (mapped != null) {
                result.append(mapped)
            } else {
                result.append(current)
            }
            index++
        }
        return result.toString()
    }
}

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var previewInput: EditText
    private lateinit var previewOutput: TextView
    private lateinit var paletteGrid: LinearLayout
    private lateinit var preferences: SharedPreferences

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        window.statusBarColor = Color.rgb(13, 15, 29)
        window.navigationBarColor = Color.rgb(13, 15, 29)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(13, 15, 29))
            isFillViewport = true
        }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(30))
        }
        scroll.addView(page)
        setContentView(scroll)

        page.addView(makeText("N5 FOX", 34f, Color.rgb(255, 210, 105), true))
        page.addView(makeText("COLOR POP · FRANCO LENS", 11f, Color.LTGRAY, true))
        addSpace(page, 18)

        statusText = makeText("", 14f, Color.WHITE, true)
        statusText.background = roundedBackground(Color.rgb(27, 31, 49), Color.rgb(80, 87, 119), 18)
        statusText.setPadding(dp(14), dp(15), dp(14), dp(15))
        page.addView(statusText)

        addSpace(page, 12)
        val enableButton = makeButton("تفعيل العدسة العائمة", Color.rgb(255, 205, 83), Color.BLACK)
        page.addView(enableButton)
        enableButton.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } catch (_: Exception) {
                toast("تعذر فتح إعدادات إمكانية الوصول")
            }
        }

        addSpace(page, 10)
        val onboarding = buildOnboarding()
        if (!preferences.getBoolean(PREF_ONBOARDING_DONE, false)) {
            page.addView(onboarding)
        }

        addSpace(page, 22)
        page.addView(makeText("اختر لون دائرتك", 20f, Color.WHITE, true))
        page.addView(makeText("ألوان مرحة — اختر اللون الذي تحبه", 12f, Color.LTGRAY, false))
        addSpace(page, 10)
        paletteGrid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(paletteGrid)
        buildPaletteGrid()

        addSpace(page, 16)
        val autoButton = makeButton("", Color.rgb(92, 101, 153), Color.WHITE)
        page.addView(autoButton)
        fun updateAutoButton() {
            val enabled = preferences.getBoolean(PREF_AUTO_COLOR, false)
            autoButton.text = if (enabled) "اللون التلقائي: مفعّل ✓" else "اللون التلقائي حسب التطبيق والوضع الليلي: متوقف"
        }
        updateAutoButton()
        autoButton.setOnClickListener {
            val enabled = !preferences.getBoolean(PREF_AUTO_COLOR, false)
            preferences.edit().putBoolean(PREF_AUTO_COLOR, enabled).apply()
            updateAutoButton()
            toast(if (enabled) "تم تفعيل الألوان التلقائية" else "تم إيقاف الألوان التلقائية")
        }

        addSpace(page, 22)
        page.addView(makeText("إعدادات الدائرة", 20f, Color.WHITE, true))
        addSpace(page, 6)
        addSeekControl(page, "حجم الدائرة", 40, 88, preferences.getInt(PREF_SIZE, 58), "dp") { value ->
            preferences.edit().putInt(PREF_SIZE, value).apply()
        }
        addSpace(page, 8)
        addSeekControl(page, "الشفافية أثناء الاستخدام", 20, 100, preferences.getInt(PREF_OPACITY, 92), "%") { value ->
            preferences.edit().putInt(PREF_OPACITY, value).apply()
        }

        addSpace(page, 24)
        page.addView(makeText("جرّب التحويل الآن", 20f, Color.WHITE, true))
        page.addView(makeText("اكتب نصاً تجريبياً، ثم اختر اتجاه التحويل.", 12f, Color.LTGRAY, false))
        addSpace(page, 10)

        val previewCard = makeCard()
        previewInput = EditText(this).apply {
            hint = "مثال: مرحبا شلونك habibi 7abibi"
            textSize = 16f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            minLines = 2
            maxLines = 4
            gravity = Gravity.TOP or Gravity.START
            textDirection = View.TEXT_DIRECTION_FIRST_STRONG
            background = roundedBackground(Color.rgb(17, 21, 37), Color.rgb(75, 84, 119), 14)
        }
        previewCard.addView(previewInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        addSpace(previewCard, 10)

        val convertRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val toFrancoButton = makeButton("عربي ← فرانكو", Color.rgb(255, 125, 174), Color.BLACK)
        val toArabicButton = makeButton("فرانكو ← عربي", Color.rgb(103, 202, 255), Color.BLACK)
        convertRow.addView(toFrancoButton, weightedParams())
        convertRow.addView(toArabicButton, weightedParams())
        previewCard.addView(convertRow)
        toFrancoButton.setOnClickListener { showPreviewResult(false) }
        toArabicButton.setOnClickListener { showPreviewResult(true) }

        addSpace(previewCard, 12)
        previewOutput = makeText("ستظهر النتيجة هنا", 16f, Color.rgb(210, 224, 255), false)
        previewOutput.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        previewOutput.textDirection = View.TEXT_DIRECTION_FIRST_STRONG
        previewOutput.background = roundedBackground(Color.rgb(22, 27, 45), Color.rgb(67, 79, 115), 14)
        previewOutput.setPadding(dp(12), dp(14), dp(12), dp(14))
        previewCard.addView(previewOutput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        addSpace(previewCard, 8)
        val copyButton = makeButton("نسخ النتيجة", Color.rgb(180, 155, 255), Color.BLACK)
        previewCard.addView(copyButton)
        copyButton.setOnClickListener {
            val output = previewOutput.text.toString()
            if (output == "ستظهر النتيجة هنا" || output.isBlank()) {
                toast("لا توجد نتيجة لنسخها")
            } else {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("N5 FOX", output))
                toast("تم نسخ النتيجة")
            }
        }
        page.addView(previewCard)

        addSpace(page, 20)
        page.addView(makeText(
            "طريقة الاستخدام: افتح أي حقل كتابة، وستظهر الدائرة عند ظهور لوحة المفاتيح. اسحبها بإصبعك لتحريكها. اضغط مرة للتحويل، واضغط مرة ثانية أو اضغط مطولاً للتراجع.",
            12f, Color.LTGRAY, false
        ))
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::statusText.isInitialized) updateStatus()
    }

    private fun buildOnboarding(): LinearLayout {
        val card = makeCard()
        card.background = roundedBackground(Color.rgb(38, 34, 58), Color.rgb(157, 125, 255), 18)
        card.addView(makeText("أهلاً بك في N5 FOX 🦊", 17f, Color.WHITE, true))
        addSpace(card, 6)
        card.addView(makeText(
            "1) اضغط تفعيل العدسة.\n2) افتح خدمات إمكانية الوصول.\n3) اختر N5 FOX Lens وفعّل السماح.\n4) ارجع للتطبيق وافتح لوحة المفاتيح في أي مكان.",
            13f, Color.rgb(232, 226, 255), false
        ))
        addSpace(card, 10)
        val understood = makeButton("فهمت، لا تعرض الشرح مجدداً", Color.rgb(183, 157, 255), Color.BLACK)
        card.addView(understood)
        understood.setOnClickListener {
            preferences.edit().putBoolean(PREF_ONBOARDING_DONE, true).apply()
            card.visibility = View.GONE
        }
        return card
    }

    private fun buildPaletteGrid() {
        paletteGrid.removeAllViews()
        val selected = preferences.getInt(PREF_COLOR, 0).coerceIn(0, FoxPalettes.all.lastIndex)
        for (rowStart in FoxPalettes.all.indices step 4) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            val end = min(rowStart + 4, FoxPalettes.all.size)
            for (index in rowStart until end) {
                val palette = FoxPalettes.all[index]
                val cell = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(3), dp(8), dp(3), dp(8))
                    background = roundedBackground(
                        if (selected == index) Color.rgb(42, 43, 65) else Color.rgb(23, 26, 42),
                        if (selected == index) palette.primary else Color.rgb(54, 61, 85), 13
                    )
                    setOnClickListener {
                        preferences.edit().putInt(PREF_COLOR, index).apply()
                        buildPaletteGrid()
                    }
                }
                val dot = PaletteDot(this, palette.primary, palette.secondary)
                cell.addView(dot, LinearLayout.LayoutParams(dp(38), dp(38)))
                val label = makeText(palette.name, 10f, Color.WHITE, selected == index)
                cell.addView(label)
                row.addView(cell, weightedParams().apply {
                    setMargins(dp(3), dp(3), dp(3), dp(3))
                })
            }
            paletteGrid.addView(row)
        }
    }

    private fun addSeekControl(
        parent: LinearLayout,
        title: String,
        minimum: Int,
        maximum: Int,
        initial: Int,
        suffix: String,
        onValue: (Int) -> Unit
    ) {
        val label = makeText("$title: ${initial.coerceIn(minimum, maximum)}$suffix", 13f, Color.WHITE, true)
        label.gravity = Gravity.START
        parent.addView(label)
        val seek = SeekBar(this).apply {
            max = maximum - minimum
            progress = initial.coerceIn(minimum, maximum) - minimum
        }
        parent.addView(seek)
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = minimum + progress
                label.text = "$title: $value$suffix"
                if (fromUser) onValue(value)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    private fun showPreviewResult(toArabic: Boolean) {
        val source = previewInput.text.toString()
        if (source.isBlank()) {
            toast("اكتب نصاً للتجربة أولاً")
            return
        }
        previewOutput.text = if (toArabic) FrancoConverter.toArabic(source) else FrancoConverter.toFranco(source)
    }

    private fun updateStatus() {
        val active = isLensEnabled()
        statusText.text = if (active) "● خدمة العدسة مفعّلة" else "● خدمة العدسة غير مفعّلة"
        statusText.setTextColor(if (active) Color.rgb(112, 239, 166) else Color.rgb(255, 183, 125))
    }

    private fun isLensEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val expected = ComponentName(this, TonFoxLensAccessibilityService::class.java).flattenToString()
        return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun makeText(value: String, size: Float, color: Int, bold: Boolean): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }

    private fun makeButton(value: String, fill: Int, textColor: Int): TextView {
        return makeText(value, 14f, textColor, true).apply {
            background = roundedBackground(fill, lighten(fill, 0.25f), 14)
            setPadding(dp(10), dp(13), dp(10), dp(13))
            isClickable = true
            isFocusable = true
        }
    }

    private fun makeCard(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(13), dp(14), dp(13), dp(14))
        background = roundedBackground(Color.rgb(25, 29, 47), Color.rgb(65, 74, 107), 17)
    }

    private fun roundedBackground(fill: Int, border: Int, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(fill)
            setStroke(dp(1), border)
        }
    }

    private fun weightedParams() = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

    private fun addSpace(parent: LinearLayout, height: Int) {
        parent.addView(View(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

private class PaletteDot(
    context: Context,
    private val first: Int,
    private val second: Int
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density = resources.displayMetrics.density

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = min(width, height) * 0.43f
        val cx = width / 2f
        val cy = height / 2f
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(cx - radius, cy - radius, cx + radius, cy + radius, first, second, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, radius, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f * density
        paint.color = Color.argb(190, 255, 255, 255)
        canvas.drawCircle(cx, cy, radius, paint)
    }
}

private class LensVisual(
    context: Context,
    primaryColor: Int,
    secondaryColor: Int
) : View(context) {
    var primary: Int = primaryColor
        set(value) { field = value; invalidate() }
    var secondary: Int = secondaryColor
        set(value) { field = value; invalidate() }

    private val density = resources.displayMetrics.density
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val side = min(width, height).toFloat()
        val cx = width / 2f
        val cy = height / 2f
        val radius = side * 0.39f

        glowPaint.style = Paint.Style.FILL
        glowPaint.color = primary
        glowPaint.setShadowLayer(13f * density, 0f, 0f, primary)
        canvas.drawCircle(cx, cy, radius * 0.96f, glowPaint)
        glowPaint.clearShadowLayer()

        fillPaint.style = Paint.Style.FILL
        fillPaint.shader = LinearGradient(
            cx - radius, cy - radius, cx + radius, cy + radius,
            primary, secondary, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius * 0.91f, fillPaint)
        fillPaint.shader = null

        ringPaint.style = Paint.Style.STROKE
        ringPaint.strokeWidth = 2f * density
        ringPaint.color = Color.argb(185, 255, 255, 255)
        canvas.drawCircle(cx, cy, radius * 0.91f, ringPaint)
    }
}

class TonFoxLensAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var lensView: LensVisual? = null
    private var lensParams: WindowManager.LayoutParams? = null
    private var undoSnapshot: UndoSnapshot? = null
    private var preferences: SharedPreferences? = null
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var initialWindowX = 0
    private var initialWindowY = 0
    private var movingLens = false
    private var longPressTriggered = false
    private val touchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop }

    private data class UndoSnapshot(val before: String, val after: String)

    private val updateRunnable = Runnable { synchronizeKeyboard() }
    private val fadeRunnable = Runnable { fadeLensToIdle() }
    private val longPressRunnable = Runnable {
        if (!movingLens) {
            longPressTriggered = true
            undoLastConversion()
        }
    }

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        handler.post { refreshLensAppearance() }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).also {
            it.registerOnSharedPreferenceChangeListener(preferenceListener)
        }

        val configuration = AccessibilityServiceInfo().apply {
            eventTypes =
                AccessibilityEvent.TYPE_VIEW_FOCUSED or
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOWS_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 80
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        setServiceInfo(configuration)
        scheduleKeyboardCheck()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event != null) scheduleKeyboardCheck()
    }

    override fun onInterrupt() {
        hideLens()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        preferences?.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        hideLens()
        super.onDestroy()
    }

    private fun scheduleKeyboardCheck() {
        handler.removeCallbacks(updateRunnable)
        handler.postDelayed(updateRunnable, 140)
    }

    private fun synchronizeKeyboard() {
        val visible = try {
            windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
        } catch (_: Exception) {
            false
        }
        if (visible) {
            if (lensView == null) showLens() else refreshLensAppearance()
        } else {
            hideLens()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private fun selectedColors(): Pair<Int, Int> {
        val prefs = preferences ?: getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val auto = prefs.getBoolean(PREF_AUTO_COLOR, false)
        if (auto) {
            val packageName = try { rootInActiveWindow?.packageName?.toString().orEmpty() } catch (_: Exception) { "" }
            val appColor = when {
                packageName.contains("whatsapp", true) -> Color.rgb(37, 211, 102)
                packageName.contains("telegram", true) -> Color.rgb(34, 158, 217)
                packageName.contains("instagram", true) -> Color.rgb(225, 48, 108)
                packageName.contains("youtube", true) -> Color.rgb(255, 48, 48)
                packageName.contains("tiktok", true) -> Color.rgb(254, 44, 85)
                packageName.contains("discord", true) -> Color.rgb(88, 101, 242)
                packageName.contains("snapchat", true) -> Color.rgb(255, 220, 40)
                packageName.contains("facebook", true) -> Color.rgb(24, 119, 242)
                packageName.contains("x.android", true) || packageName.endsWith(".twitter") -> Color.rgb(105, 112, 128)
                packageName.contains("google", true) || packageName.contains("gboard", true) -> Color.rgb(66, 133, 244)
                else -> {
                    val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                    if (night) Color.rgb(150, 112, 255) else Color.rgb(255, 160, 90)
                }
            }
            return appColor to lighten(appColor, 0.43f)
        }

        val index = prefs.getInt(PREF_COLOR, 0).coerceIn(0, FoxPalettes.all.lastIndex)
        val palette = FoxPalettes.all[index]
        return palette.primary to palette.secondary
    }

    private fun lighten(color: Int, amount: Float): Int {
        val a = amount.coerceIn(0f, 1f)
        val r = Color.red(color) + ((255 - Color.red(color)) * a).roundToInt()
        val g = Color.green(color) + ((255 - Color.green(color)) * a).roundToInt()
        val b = Color.blue(color) + ((255 - Color.blue(color)) * a).roundToInt()
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    private fun showLens() {
        if (lensView != null) return
        val manager = windowManager ?: return
        val colors = selectedColors()
        val view = LensVisual(this, colors.first, colors.second)
        view.contentDescription = "N5 FOX floating color circle"
        view.setOnTouchListener { touchedView, event -> handleLensTouch(touchedView, event) }

        val prefs = preferences ?: getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val size = prefs.getInt(PREF_SIZE, 58).coerceIn(40, 88)
        val params = WindowManager.LayoutParams(
            dp(size), dp(size),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = dp(16)
            y = dp(270)
        }

        try {
            manager.addView(view, params)
            lensView = view
            lensParams = params
            view.alpha = configuredOpacity() * 0.56f
        } catch (_: Exception) {
            lensView = null
            lensParams = null
        }
    }

    private fun refreshLensAppearance() {
        val view = lensView ?: return
        val params = lensParams ?: return
        val colors = selectedColors()
        view.primary = colors.first
        view.secondary = colors.second
        val prefs = preferences ?: return
        val size = dp(prefs.getInt(PREF_SIZE, 58).coerceIn(40, 88))
        if (params.width != size || params.height != size) {
            params.width = size
            params.height = size
            try { windowManager?.updateViewLayout(view, params) } catch (_: Exception) { }
        }
        if (!view.isPressed) view.alpha = configuredOpacity() * 0.56f
    }

    private fun configuredOpacity(): Float {
        val prefs = preferences ?: getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(PREF_OPACITY, 92).coerceIn(20, 100) / 100f
    }

    private fun handleLensTouch(view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val dx = event.x - view.width / 2f
                val dy = event.y - view.height / 2f
                if (dx * dx + dy * dy > (min(view.width, view.height) * 0.48f).let { it * it }) {
                    return false
                }
                movingLens = false
                longPressTriggered = false
                touchStartX = event.rawX
                touchStartY = event.rawY
                val params = lensParams ?: return false
                initialWindowX = params.x
                initialWindowY = params.y
                handler.removeCallbacks(longPressRunnable)
                handler.postDelayed(longPressRunnable, 570)
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                pulseLens(view)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - touchStartX
                val deltaY = event.rawY - touchStartY
                if (!movingLens && (abs(deltaX) > touchSlop || abs(deltaY) > touchSlop)) {
                    movingLens = true
                    handler.removeCallbacks(longPressRunnable)
                }
                if (movingLens) moveLens(deltaX, deltaY)
                return true
            }

            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longPressRunnable)
                if (!movingLens && !longPressTriggered) {
                    if (undoSnapshot != null) undoLastConversion() else convertFocusedText()
                }
                fadeLensSoon()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                fadeLensSoon()
                return true
            }
        }
        return true
    }

    private fun moveLens(deltaX: Float, deltaY: Float) {
        val view = lensView ?: return
        val params = lensParams ?: return
        val metrics = resources.displayMetrics
        val maxX = max(0, metrics.widthPixels - params.width)
        val maxY = max(0, metrics.heightPixels - params.height)
        params.x = (initialWindowX - deltaX.roundToInt()).coerceIn(0, maxX)
        params.y = (initialWindowY - deltaY.roundToInt()).coerceIn(0, maxY)
        try { windowManager?.updateViewLayout(view, params) } catch (_: Exception) { }
        view.alpha = configuredOpacity()
    }

    private fun pulseLens(view: View) {
        handler.removeCallbacks(fadeRunnable)
        view.animate().cancel()
        view.alpha = configuredOpacity()
        view.scaleX = 1f
        view.scaleY = 1f
        view.animate().scaleX(1.13f).scaleY(1.13f).setDuration(110).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
        }.start()
    }

    private fun fadeLensSoon() {
        handler.removeCallbacks(fadeRunnable)
        handler.postDelayed(fadeRunnable, 700)
    }

    private fun fadeLensToIdle() {
        val view = lensView ?: return
        view.animate().alpha(configuredOpacity() * 0.56f).setDuration(260).start()
    }

    private fun hideLens() {
        handler.removeCallbacks(fadeRunnable)
        handler.removeCallbacks(longPressRunnable)
        val view = lensView ?: return
        try { windowManager?.removeView(view) } catch (_: Exception) { }
        lensView = null
        lensParams = null
    }

    private fun currentEditableNode(): AccessibilityNodeInfo? {
        val root = try { rootInActiveWindow } catch (_: Exception) { null } ?: return null
        val focused = try { root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) } catch (_: Exception) { null }
        if (focused != null && focused.isEditable && !focused.isPassword) return focused
        return findFocusedEditable(root)
    }

    private fun findFocusedEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable && node.isFocused && !node.isPassword) return node
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            val result = findFocusedEditable(child)
            if (result != null) return result
        }
        return null
    }

    private fun convertFocusedText() {
        try {
            val target = currentEditableNode()
            if (target == null) {
                showMessage("حدد حقل كتابة أولاً")
                return
            }
            if (target.isPassword) {
                showMessage("لا يمكن تعديل حقول كلمات المرور")
                return
            }
            val original = target.text?.toString().orEmpty()
            if (original.isEmpty()) {
                showMessage("اكتب النص أولاً")
                return
            }

            val selectionStart = target.textSelectionStart
            val selectionEnd = target.textSelectionEnd
            val hasSelection = selectionStart >= 0 && selectionEnd >= 0 && selectionStart != selectionEnd
            val start = if (hasSelection) min(selectionStart, selectionEnd).coerceIn(0, original.length) else 0
            val end = if (hasSelection) max(selectionStart, selectionEnd).coerceIn(0, original.length) else original.length
            if (start >= end) {
                showMessage("لم يتم العثور على نص محدد صالح")
                return
            }

            val selectedText = original.substring(start, end)
            val converted = if (FrancoConverter.hasArabic(selectedText)) {
                FrancoConverter.toFranco(selectedText)
            } else {
                FrancoConverter.toArabic(selectedText)
            }
            if (converted == selectedText) {
                showMessage("لم يتغير النص")
                return
            }

            val replacement = original.substring(0, start) + converted + original.substring(end)
            if (setNodeText(target, replacement)) {
                undoSnapshot = UndoSnapshot(original, replacement)
                val cursor = (start + converted.length).coerceIn(0, replacement.length)
                setNodeSelection(target, cursor)
                showMessage(if (hasSelection) "تم تحويل الجزء المحدد" else "تم التحويل — اضغط ثانية أو مطولاً للتراجع")
            } else {
                showMessage("هذا الحقل لا يسمح بتعديل النص")
            }
        } catch (_: Exception) {
            showMessage("تعذر تحويل النص في هذا الحقل")
        }
    }

    private fun undoLastConversion() {
        val snapshot = undoSnapshot ?: run {
            showMessage("لا يوجد تحويل للتراجع عنه")
            return
        }
        try {
            val target = currentEditableNode()
            if (target == null) {
                showMessage("ارجع إلى حقل النص نفسه للتراجع")
                return
            }
            val current = target.text?.toString().orEmpty()
            if (current != snapshot.after) {
                showMessage("تغيّر النص بعد التحويل؛ لم يتم استبداله")
                undoSnapshot = null
                return
            }
            if (setNodeText(target, snapshot.before)) {
                setNodeSelection(target, snapshot.before.length)
                undoSnapshot = null
                showMessage("تم التراجع وإرجاع النص الأصلي")
            } else {
                showMessage("تعذر استرجاع النص في هذا الحقل")
            }
        } catch (_: Exception) {
            showMessage("تعذر تنفيذ التراجع")
        }
    }

    private fun setNodeText(node: AccessibilityNodeInfo, value: String): Boolean {
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun setNodeSelection(node: AccessibilityNodeInfo, position: Int) {
        val args = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, position)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, position)
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)
    }

    private fun showMessage(message: String) {
        handler.post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}

private fun lighten(color: Int, amount: Float): Int {
    val a = amount.coerceIn(0f, 1f)
    val r = Color.red(color) + ((255 - Color.red(color)) * a).roundToInt()
    val g = Color.green(color) + ((255 - Color.green(color)) * a).roundToInt()
    val b = Color.blue(color) + ((255 - Color.blue(color)) * a).roundToInt()
    return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
}

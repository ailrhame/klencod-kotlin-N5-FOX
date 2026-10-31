package com.fox.mom

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.graphics.drawable.GradientDrawable
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val PREFS_NAME = "n5_fox_preferences"
private const val PREF_THEME = "selected_lens_theme"

private data class FoxTheme(
    val name: String,
    val primary: Int,
    val secondary: Int,
    val form: Int
)

private object FoxThemes {

    val all = listOf(
        FoxTheme("ذهبي ملكي", Color.parseColor("#FFC857"), Color.parseColor("#FFF0B3"), 1),
        FoxTheme("بنفسجي فاخر", Color.parseColor("#B76EFF"), Color.parseColor("#F4C5FF"), 1),
        FoxTheme("نيوني أزرق", Color.parseColor("#277BFF"), Color.parseColor("#B8E6FF"), 0),
        FoxTheme("كريستالي سماوي", Color.parseColor("#00D5FF"), Color.parseColor("#B8FFFF"), 2),
        FoxTheme("نيوني فوشيا", Color.parseColor("#E64EFF"), Color.parseColor("#FFC0FA"), 3),
        FoxTheme("معدن ذهبي", Color.parseColor("#D6A83D"), Color.parseColor("#FFF1B8"), 0),

        FoxTheme("مضيئة ذهبية", Color.parseColor("#FFB32C"), Color.parseColor("#FFF0A0"), 0),
        FoxTheme("مضيئة زرقاء", Color.parseColor("#238BFF"), Color.parseColor("#C5E9FF"), 0),
        FoxTheme("مضيئة خضراء", Color.parseColor("#2CE66D"), Color.parseColor("#C5FFD8"), 2),
        FoxTheme("مضيئة حمراء", Color.parseColor("#FF334F"), Color.parseColor("#FFD0D8"), 4),
        FoxTheme("متعددة الألوان", Color.parseColor("#238BFF"), Color.parseColor("#FFCF42"), 3),
        FoxTheme("شكل نجمة", Color.parseColor("#FFCA28"), Color.parseColor("#FFF1A8"), 3),

        FoxTheme("شكل قلب", Color.parseColor("#FF36A8"), Color.parseColor("#FFC0E8"), 4),
        FoxTheme("شكل مربع", Color.parseColor("#A53DFF"), Color.parseColor("#E4C4FF"), 2),
        FoxTheme("شكل سداسي", Color.parseColor("#8D4DFF"), Color.parseColor("#DCC3FF"), 5),
        FoxTheme("شكل معين", Color.parseColor("#318EFF"), Color.parseColor("#C7E7FF"), 6),
        FoxTheme("دائري متحرك", Color.parseColor("#FFC400"), Color.parseColor("#54CFFF"), 7),
        FoxTheme("شكل موجات", Color.parseColor("#00BFFF"), Color.parseColor("#B4F5FF"), 7),

        FoxTheme("عدسة احترافية", Color.parseColor("#91A8C5"), Color.parseColor("#FFFFFF"), 0),
        FoxTheme("شكل روبوت", Color.parseColor("#00DFFF"), Color.parseColor("#D1F7FF"), 6),
        FoxTheme("شكل ثلج", Color.parseColor("#7CCBFF"), Color.parseColor("#FFFFFF"), 5),
        FoxTheme("شكل نار", Color.parseColor("#FF5B17"), Color.parseColor("#FFD76A"), 4),
        FoxTheme("تاج ماسي", Color.parseColor("#527BFF"), Color.parseColor("#DCCBFF"), 1),
        FoxTheme("بصمة ذهبية", Color.parseColor("#C99A22"), Color.parseColor("#FFE69A"), 7)
    )
}

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var themeGrid: LinearLayout

    private val preferences by lazy {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).roundToInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(9, 9, 15)
        window.navigationBarColor = Color.rgb(9, 9, 15)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(9, 9, 15))
            isFillViewport = true
        }

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(25), dp(18), dp(30))
        }

        scroll.addView(page)
        setContentView(scroll)

        val header = makeText(
            "N5 FOX",
            32f,
            Color.parseColor("#FFD36A"),
            true
        )
        page.addView(header)

        page.addView(
            makeText(
                "SMART FLOATING LENS",
                11f,
                Color.LTGRAY,
                true
            )
        )

        addSpace(page, 22)

        statusText = makeText(
            "",
            15f,
            Color.WHITE,
            true
        )

        statusText.background = roundedBackground(
            Color.rgb(24, 25, 36),
            Color.parseColor("#66552D")
        )

        statusText.setPadding(dp(15), dp(17), dp(15), dp(17))
        page.addView(statusText)

        addSpace(page, 14)

        val activationButton = makeButton("تفعيل خدمة العدسة")
        page.addView(activationButton)

        activationButton.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } catch (_: Exception) {
                Toast.makeText(
                    this,
                    "تعذر فتح إعدادات إمكانية الوصول",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        addSpace(page, 10)

        page.addView(
            makeText(
                "فعّل N5 FOX Lens من خدمات إمكانية الوصول. " +
                    "ستظهر العدسة تلقائياً عندما يظهر الكيبورد.",
                13f,
                Color.LTGRAY,
                false
            )
        )

        addSpace(page, 25)

        page.addView(
            makeText(
                "اختر شكل العدسة",
                21f,
                Color.WHITE,
                true
            )
        )

        addSpace(page, 5)

        page.addView(
            makeText(
                "24 ثيم مختلفاً — اختر الشكل الذي يناسبك",
                12f,
                Color.GRAY,
                false
            )
        )

        addSpace(page, 16)

        themeGrid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        page.addView(themeGrid)

        buildThemeGrid()
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::statusText.isInitialized) {
            updateStatus()
        }
    }

    private fun updateStatus() {
        val enabled = isLensEnabled()

        statusText.text = if (enabled) {
            "● العدسة مفعّلة"
        } else {
            "● العدسة غير مفعّلة"
        }

        statusText.setTextColor(
            if (enabled) Color.parseColor("#66E69A")
            else Color.parseColor("#FFB56D")
        )
    }

    private fun isLensEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val expected = ComponentName(
            this,
            TonFoxLensAccessibilityService::class.java
        ).flattenToString()

        return enabledServices.split(':').any {
            it.equals(expected, ignoreCase = true)
        }
    }

    private fun buildThemeGrid() {
        themeGrid.removeAllViews()

        val selected = preferences.getInt(PREF_THEME, 0)

        for (rowStart in FoxThemes.all.indices step 3) {

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            val rowEnd = min(rowStart + 3, FoxThemes.all.size)

            for (index in rowStart until rowEnd) {

                val theme = FoxThemes.all[index]
                val isSelected = selected == index

                val cell = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(5), dp(10), dp(5), dp(10))

                    background = roundedBackground(
                        if (isSelected) Color.rgb(38, 33, 22)
                        else Color.rgb(18, 19, 28),
                        if (isSelected) theme.primary
                        else Color.rgb(45, 46, 59)
                    )

                    setOnClickListener {
                        preferences.edit()
                            .putInt(PREF_THEME, index)
                            .apply()

                        buildThemeGrid()
                    }
                }

                val preview = LensVisual(
                    this,
                    index,
                    68
                )

                cell.addView(preview)

                val name = makeText(
                    theme.name + if (isSelected) " ✓" else "",
                    11f,
                    if (isSelected) theme.secondary else Color.WHITE,
                    isSelected
                )

                name.gravity = Gravity.CENTER
                name.setPadding(0, dp(8), 0, 0)

                cell.addView(name)

                val params = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    setMargins(dp(4), dp(4), dp(4), dp(4))
                }

                row.addView(cell, params)
            }

            themeGrid.addView(
                row,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun makeText(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            textDirection = View.TEXT_DIRECTION_RTL
            gravity = Gravity.CENTER
            typeface = if (bold) {
                Typeface.DEFAULT_BOLD
            } else {
                Typeface.DEFAULT
            }
        }
    }

    private fun makeButton(value: String): TextView {
        return makeText(
            value,
            16f,
            Color.BLACK,
            true
        ).apply {
            background = roundedBackground(
                Color.parseColor("#E9B94D"),
                Color.parseColor("#FFE5A0")
            )
            setPadding(dp(12), dp(16), dp(12), dp(16))
            isClickable = true
            isFocusable = true
        }
    }

    private fun roundedBackground(
        fill: Int,
        border: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(16).toFloat()
            setColor(fill)
            setStroke(dp(1), border)
        }
    }

    private fun addSpace(parent: LinearLayout, height: Int) {
        parent.addView(
            View(this),
            LinearLayout.LayoutParams(
                1,
                dp(height)
            )
        )
    }
}

class LensVisual(
    context: Context,
    private val themeIndex: Int,
    private val sizeDp: Int
) : View(context) {

    private val theme = FoxThemes.all[
        themeIndex.coerceIn(0, FoxThemes.all.lastIndex)
    ]

    private val density = resources.displayMetrics.density

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        val desired = (sizeDp * density).roundToInt()

        setMeasuredDimension(
            resolveSize(desired, widthMeasureSpec),
            resolveSize(desired, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) * 0.40f

        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(12, 13, 24)
            style = Paint.Style.FILL
        }

        canvas.drawCircle(
            cx,
            cy,
            radius * 0.90f,
            backgroundPaint
        )

        drawOuterShape(canvas, cx, cy, radius)
        drawMagnifier(canvas, cx, cy, radius)
    }

    private fun drawOuterShape(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.primary
            style = Paint.Style.STROKE
            strokeWidth = 3.5f * density
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            setShadowLayer(
                9f * density,
                0f,
                0f,
                theme.primary
            )
        }

        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.secondary
            style = Paint.Style.STROKE
            strokeWidth = 1.3f * density
        }

        when (theme.form) {

            0 -> {
                canvas.drawCircle(cx, cy, radius, paint)
                canvas.drawCircle(cx, cy, radius * 0.86f, innerPaint)
            }

            1 -> {
                canvas.drawCircle(cx, cy, radius * 0.88f, paint)

                val crown = Path().apply {
                    moveTo(cx - radius * 0.60f, cy - radius * 0.55f)
                    lineTo(cx - radius * 0.50f, cy - radius * 1.30f)
                    lineTo(cx - radius * 0.12f, cy - radius * 0.72f)
                    lineTo(cx, cy - radius * 1.35f)
                    lineTo(cx + radius * 0.18f, cy - radius * 0.72f)
                    lineTo(cx + radius * 0.55f, cy - radius * 1.25f)
                    lineTo(cx + radius * 0.60f, cy - radius * 0.55f)
                    close()
                }

                canvas.drawPath(crown, paint)
            }

            2 -> {
                val rect = RectF(
                    cx - radius,
                    cy - radius,
                    cx + radius,
                    cy + radius
                )

                canvas.drawRoundRect(
                    rect,
                    radius * 0.35f,
                    radius * 0.35f,
                    paint
                )
            }

            3 -> {
                canvas.drawPath(
                    polygonPath(cx, cy, radius, 10, radius * 0.48f),
                    paint
                )
            }

            4 -> {
                val heart = Path().apply {
                    moveTo(cx, cy + radius * 0.85f)
                    cubicTo(
                        cx - radius * 1.5f,
                        cy - radius * 0.15f,
                        cx - radius * 0.75f,
                        cy - radius * 1.2f,
                        cx,
                        cy - radius * 0.48f
                    )
                    cubicTo(
                        cx + radius * 0.75f,
                        cy - radius * 1.2f,
                        cx + radius * 1.5f,
                        cy - radius * 0.15f,
                        cx,
                        cy + radius * 0.85f
                    )
                    close()
                }

                canvas.drawPath(heart, paint)
            }

            5 -> {
                canvas.drawPath(
                    polygonPath(cx, cy, radius, 6),
                    paint
                )
            }

            6 -> {
                canvas.drawPath(
                    polygonPath(cx, cy, radius, 4),
                    paint
                )
            }

            7 -> {
                for (i in 0..2) {
                    val r = radius * (1f - i * 0.16f)
                    canvas.drawCircle(cx, cy, r, paint)
                }
            }
        }

        paint.clearShadowLayer()

        canvas.drawCircle(
            cx,
            cy,
            radius * 0.76f,
            innerPaint
        )
    }

    private fun polygonPath(
        cx: Float,
        cy: Float,
        radius: Float,
        points: Int,
        innerRatio: Float = 1f
    ): Path {
        val path = Path()

        for (i in 0 until points) {
            val angle = -PI / 2.0 + (2.0 * PI * i / points)
            val r = if (points == 10 && i % 2 == 1) {
                radius * innerRatio
            } else {
                radius
            }

            val x = cx + cos(angle).toFloat() * r
            val y = cy + sin(angle).toFloat() * r

            if (i == 0) path.moveTo(x, y)
            else path.lineTo(x, y)
        }

        path.close()
        return path
    }

    private fun drawMagnifier(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float
    ) {
        val lensPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.secondary
            style = Paint.Style.STROKE
            strokeWidth = 4f * density
        }

        val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.secondary
            style = Paint.Style.STROKE
            strokeWidth = 5f * density
            strokeCap = Paint.Cap.ROUND
        }

        val lensX = cx - radius * 0.10f
        val lensY = cy - radius * 0.10f
        val lensRadius = radius * 0.25f

        canvas.drawCircle(
            lensX,
            lensY,
            lensRadius,
            lensPaint
        )

        canvas.drawLine(
            lensX + lensRadius * 0.72f,
            lensY + lensRadius * 0.72f,
            cx + radius * 0.45f,
            cy + radius * 0.45f,
            handlePaint
        )
    }
}

class TonFoxLensAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    private var windowManager: WindowManager? = null
    private var lensView: LensVisual? = null

    private val updateRunnable = Runnable {
        synchronizeKeyboard()
    }

    private val arabicMap = mapOf(
        'ا' to "a",
        'أ' to "a",
        'إ' to "e",
        'آ' to "aa",
        'ب' to "b",
        'ت' to "t",
        'ث' to "th",
        'ج' to "j",
        'ح' to "7",
        'خ' to "5",
        'د' to "d",
        'ذ' to "dh",
        'ر' to "r",
        'ز' to "z",
        'س' to "s",
        'ش' to "sh",
        'ص' to "9",
        'ض' to "9d",
        'ط' to "6",
        'ظ' to "6z",
        'ع' to "3",
        'غ' to "gh",
        'ف' to "f",
        'ق' to "q",
        'ك' to "k",
        'ل' to "l",
        'م' to "m",
        'ن' to "n",
        'ه' to "h",
        'ة' to "a",
        'و' to "w",
        'ي' to "y",
        'ى' to "a",
        'ء' to "2",
        'ئ' to "2",
        'ؤ' to "2",
        'چ' to "ch",
        'گ' to "g",
        'پ' to "p",
        'ڤ' to "v"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val serviceConfiguration = AccessibilityServiceInfo().apply {
            eventTypes =
                AccessibilityEvent.TYPE_VIEW_FOCUSED or
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOWS_CHANGED

            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC

            notificationTimeout = 80

            flags =
                AccessibilityServiceInfo.FLAG_DEFAULT or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }

        setServiceInfo(serviceConfiguration)

        scheduleKeyboardCheck()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        scheduleKeyboardCheck()
    }

    override fun onInterrupt() {
        hideLens()
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateRunnable)
        hideLens()
        super.onDestroy()
    }

    private fun scheduleKeyboardCheck() {
        handler.removeCallbacks(updateRunnable)
        handler.postDelayed(updateRunnable, 160)
    }

    private fun synchronizeKeyboard() {
        val keyboardVisible = try {
            windows.any {
                it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
            }
        } catch (_: Exception) {
            false
        }

        if (keyboardVisible) {
            showLens()
        } else {
            hideLens()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).roundToInt()
    }

    private fun showLens() {
        if (lensView != null) return

        val manager = windowManager ?: return

        val selectedTheme = getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getInt(PREF_THEME, 0).coerceIn(0, FoxThemes.all.lastIndex)

        val floatingLens = LensVisual(
            this,
            selectedTheme,
            58
        ).apply {
            isClickable = true
            contentDescription = "N5 FOX Franco Lens"

            setOnClickListener {
                convertFocusedText()
            }
        }

        val parameters = WindowManager.LayoutParams(
            dp(58),
            dp(58),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = dp(15)
            y = dp(285)
        }

        try {
            manager.addView(floatingLens, parameters)
            lensView = floatingLens
        } catch (_: Exception) {
            lensView = null
        }
    }

    private fun hideLens() {
        val currentView = lensView ?: return

        try {
            windowManager?.removeView(currentView)
        } catch (_: Exception) {
        }

        lensView = null
    }

    private fun convertFocusedText() {
        try {
            val root = rootInActiveWindow

            if (root == null) {
                showMessage("لم يتم العثور على حقل الكتابة")
                return
            }

            val focused = root.findFocus(
                AccessibilityNodeInfo.FOCUS_INPUT
            )

            val target = focused?.takeIf {
                it.isEditable && !it.isPassword
            } ?: findFocusedEditable(root)

            if (target == null) {
                showMessage("حدد حقل الكتابة أولاً")
                return
            }

            if (target.isPassword) {
                showMessage("لا يمكن تعديل حقول كلمات المرور")
                return
            }

            val originalText = target.text?.toString().orEmpty()

            if (originalText.isEmpty()) {
                showMessage("اكتب النص العربي أولاً")
                return
            }

            val hasArabic = originalText.any {
                arabicMap.containsKey(it)
            }

            if (!hasArabic) {
                showMessage("لم يتم العثور على نص عربي")
                return
            }

            val convertedText = transliterate(originalText)

            val arguments = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    convertedText
                )
            }

            val success = target.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                arguments
            )

            showMessage(
                if (success) "تم التحويل إلى فرانكو"
                else "هذا الحقل لا يسمح بتعديل النص"
            )

        } catch (_: Exception) {
            showMessage("تعذر تحويل النص في هذا الحقل")
        }
    }

    private fun findFocusedEditable(
        node: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {

        if (node.isEditable && node.isFocused && !node.isPassword) {
            return node
        }

        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue

            val result = findFocusedEditable(child)

            if (result != null) {
                return result
            }
        }

        return null
    }

    private fun transliterate(input: String): String {
        val result = StringBuilder(input.length * 2)

        for (character in input) {
            when {
                arabicMap.containsKey(character) -> {
                    result.append(arabicMap[character])
                }

                character == '\u0640' -> {
                }

                character in '\u064B'..'\u065F' -> {
                }

                character == '\u0670' -> {
                }

                else -> {
                    result.append(character)
                }
            }
        }

        return result.toString()
    }

    private fun showMessage(message: String) {
        handler.post {
            Toast.makeText(
                applicationContext,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
package pl.example.translator

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.*

/**
 * Opt-in accessibility overlay. Does NOT read or upload text in the background.
 * A deliberate bubble click reads visible accessible text, then the user must
 * explicitly confirm sending the preview to OpenAI. Password nodes are skipped.
 */
class QuickTranslateService : AccessibilityService() {
    private lateinit var wm: WindowManager
    private var bubble: View? = null
    private var panel: View? = null
    private val ui = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showBubble()
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No automatic content scanning. Only the explicit bubble press triggers a read.
    }
    override fun onInterrupt() {}

    private fun showBubble() {
        if (bubble != null) return
        val view = TextView(this).apply {
            text = "T"
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            textSize = 19f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.rgb(27, 87, 180)); cornerRadius = dp(100).toFloat()
            }
            elevation = dp(6).toFloat()
            setOnClickListener { onBubblePressed() }
            contentDescription = "Przetłumacz tekst widoczny na ekranie"
        }
        val params = WindowManager.LayoutParams(
            dp(49), dp(49), WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL; x = dp(8) }
        try { wm.addView(view, params); bubble = view } catch (_: Exception) {}
    }

    private fun onBubblePressed() {
        val captured = collectText()
        if (captured.isBlank()) {
            showPanel("Nie udało się odczytać tekstu z tej aplikacji. Spróbuj zaznaczyć i udostępnić tekst do translatora.")
        } else showPreview(captured)
    }

    private fun collectText(): String {
        val root = rootInActiveWindow ?: return ""
        val items = LinkedHashSet<String>()
        val queue = java.util.ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var count = 0
        while (!queue.isEmpty() && count++ < 300 && items.sumOf { it.length } < 3500) {
            val node = queue.removeFirst()
            if (node.isVisibleToUser && !node.isPassword) {
                val t = node.text?.toString()?.trim().orEmpty()
                if (t.length in 2..1300 && t != "T") items.add(t)
                for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
            }
        }
        return items.joinToString("\n").take(3500)
    }

    private fun showPreview(original: String) {
        removePanel()
        val settings = getSharedPreferences("translator_settings", MODE_PRIVATE)
        val choice = arrayOf("Polski", "Angielski", "Rosyjski")
        val target = choice[settings.getInt("target_idx", 0).coerceIn(0, 2)]
        val container = makePanel()
        container.addView(label("Tekst z ekranu — sprawdź przed wysłaniem", 17f, true))
        container.addView(label("Wysyłany tekst będzie przekazany do OpenAI wyłącznie po dotknięciu «Tłumacz». Hasła są pomijane, ale na ekranie mogą być też dane prywatne.", 12f))
        val textPreview = EditText(this).apply {
            setText(original); textSize = 14f; setTextColor(Color.rgb(20, 42, 72))
            gravity = Gravity.TOP; minLines = 3
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        val scroll = ScrollView(this)
        scroll.addView(textPreview)
        container.addView(scroll, LinearLayout.LayoutParams(-1, dp(160)))
        val output = label("Język docelowy: $target", 14f)
        container.addView(output)
        container.addView(button("Tłumacz na $target") { pressed ->
            pressed.isEnabled = false
            output.text = "Tłumaczę…"
            val approvedText = textPreview.text.toString()
            Thread {
                val result = runCatching { ChatGptClient(this).translate(approvedText, "Automatycznie", target, "Naturalnie") }
                ui.post {
                    pressed.isEnabled = true
                    output.text = result.fold(onSuccess = { it }, onFailure = { "Nie udało się: ${it.message}. W razie potrzeby połącz konto w aplikacji." })
                }
            }.start()
        })
        container.addView(button("Zamknij") { removePanel() })
        installPanel(container)
    }

    private fun showPanel(message: String) {
        removePanel()
        val container = makePanel()
        container.addView(label(message, 14f))
        container.addView(button("Otwórz translator") {
            removePanel()
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        })
        container.addView(button("Zamknij") { removePanel() })
        installPanel(container)
    }

    private fun makePanel() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(15), dp(15), dp(15), dp(15))
        background = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dp(14).toFloat()
            setStroke(dp(2), Color.rgb(27, 87, 180))
        }
        elevation = dp(12).toFloat()
    }
    private fun installPanel(view: View) {
        val width = (resources.displayMetrics.widthPixels * .91).toInt()
        val params = WindowManager.LayoutParams(
            width, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }
        try { wm.addView(view, params); panel = view; bubble?.visibility = View.GONE } catch (_: Exception) {}
    }
    private fun removePanel() {
        panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        panel = null
        bubble?.visibility = View.VISIBLE
    }
    override fun onDestroy() {
        removePanel()
        bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        bubble = null
        super.onDestroy()
    }
    private fun label(text: String, size: Float, strong: Boolean = false) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(Color.rgb(20, 42, 72))
        if (strong) setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(4), 0, dp(8))
    }
    private fun button(text: String, action: (Button) -> Unit) = Button(this).apply {
        this.text = text; isAllCaps = false
        setOnClickListener { action(this) }
    }
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
}

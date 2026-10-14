package pl.example.translator

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var client: ChatGptClient
    private lateinit var source: Spinner
    private lateinit var target: Spinner
    private lateinit var style: Spinner
    private lateinit var input: EditText
    private lateinit var output: TextView
    private lateinit var status: TextView
    private lateinit var translateButton: Button
    private lateinit var connectButton: Button
    private var processText = false
    private var processReadOnly = false
    private var busy = false
    private val settings by lazy { getSharedPreferences("translator_settings", Context.MODE_PRIVATE) }
    private val languages = arrayOf("Automatycznie", "Polski", "Angielski", "Rosyjski")
    private val targets = arrayOf("Polski", "Angielski", "Rosyjski")
    private val modes = arrayOf("Naturalnie", "Dosłownie", "Literacko")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        client = ChatGptClient(this)
        window.statusBarColor = Color.rgb(19, 39, 70)
        window.navigationBarColor = Color.rgb(246, 249, 252)
        render()
        updateConnection()
        receiveIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveIntent(intent)
    }

    private fun receiveIntent(intent: Intent?) {
        if (intent == null) return
        val incoming = when (intent.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            else -> intent.getStringExtra("input_text")
        }
        if (!incoming.isNullOrBlank()) {
            input.setText(incoming)
            input.setSelection(input.length())
            output.text = "" // nie pokazuj tłumaczenia z poprzedniego tekstu
            status.text = "Tekst odebrany z innej aplikacji. Dotknij «Tłumacz»."
        }
        processText = intent.action == Intent.ACTION_PROCESS_TEXT
        processReadOnly = intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)
    }

    private fun render() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(28))
            setBackgroundColor(Color.rgb(246, 249, 252))
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(page) }
        setContentView(scroll)
        page.addView(text("Tłumacz PL · EN · RU", 24f, true, Color.rgb(19, 39, 70)))
        page.addView(text("Tłumaczenia z wykorzystaniem konta ChatGPT", 13f, false, Color.DKGRAY).spaced(0, 2, 0, 14))

        connectButton = button("Połącz z ChatGPT") { connect() }
        page.addView(connectButton.spaced(0, 0, 0, 8))
        page.addView(button("Rozłącz konto (tylko na tym urządzeniu)") {
            AlertDialog.Builder(this).setMessage("Usunąć lokalne tokeny dostępu? Uprawnienie aplikacji możesz również cofnąć w ustawieniach ChatGPT.")
                .setNegativeButton("Anuluj", null)
                .setPositiveButton("Rozłącz") { _, _ -> client.signOut(); updateConnection() }.show()
        }.spaced(0, 0, 0, 8))
        status = text("", 13f, false, Color.rgb(70, 86, 108))
        page.addView(status.spaced(0, 0, 0, 14))

        page.addView(text("Język oryginału", 13f, true).spaced(0, 0, 0, 5))
        source = spinner(languages)
        source.setSelection(settings.getInt("source_idx", 0).coerceIn(0, 3))
        page.addView(source.spaced(0, 0, 0, 10))
        page.addView(button("⇄ Zamień języki") {
            val from = source.selectedItem.toString()
            val to = target.selectedItem.toString()
            if (from != "Automatycznie") {
                source.setSelection(languages.indexOf(to).coerceAtLeast(0))
                target.setSelection(targets.indexOf(from).coerceAtLeast(0))
                settings.edit().putInt("target_idx", target.selectedItemPosition).apply()
            } else Toast.makeText(this, "Najpierw wybierz język źródłowy.", Toast.LENGTH_SHORT).show()
        }.spaced(0, 0, 0, 10))
        page.addView(text("Przetłumacz na", 13f, true).spaced(0, 0, 0, 5))
        target = spinner(targets)
        target.setSelection(settings.getInt("target_idx", 0).coerceIn(0, 2))
        page.addView(target.spaced(0, 0, 0, 12))
        target.onItemSelectedListener = object: android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                settings.edit().putInt("target_idx", position).apply()
            }
        }

        page.addView(text("Tryb tłumaczenia", 13f, true).spaced(0, 0, 0, 5))
        style = spinner(modes)
        page.addView(style.spaced(0, 0, 0, 14))

        page.addView(text("Tekst do przetłumaczenia", 13f, true).spaced(0, 0, 0, 5))
        input = EditText(this).apply {
            gravity = Gravity.TOP or Gravity.START
            minLines = 5; maxLines = 12
            textSize = 16f
            hint = "Wpisz lub wklej tekst po polsku, angielsku albo rosyjsku…"
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = panelBackground()
        }
        page.addView(input.spaced(0, 0, 0, 8))

        val inputControls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        inputControls.addView(button("Wklej") { paste() }.weighted())
        inputControls.addView(button("Wyczyść") { input.setText(""); output.text = "" }.weighted())
        page.addView(inputControls.spaced(0, 0, 0, 14))

        translateButton = button("PRZETŁUMACZ") { translateNow() }.apply {
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.rgb(27, 87, 180)); cornerRadius = dp(12).toFloat()
            }
            setPadding(dp(10), dp(14), dp(10), dp(14))
        }
        page.addView(translateButton.spaced(0, 0, 0, 14))
        page.addView(text("Tłumaczenie", 16f, true, Color.rgb(19, 39, 70)).spaced(0, 0, 0, 6))
        output = text("Tutaj pojawi się tłumaczenie.", 17f).apply {
            setPadding(dp(12), dp(14), dp(12), dp(14))
            background = panelBackground()
            minHeight = dp(120)
            setTextIsSelectable(true)
        }
        page.addView(output.spaced(0, 0, 0, 8))
        val outputControls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        outputControls.addView(button("Kopiuj") { copyResult() }.weighted())
        outputControls.addView(button("Udostępnij") { shareResult() }.weighted())
        page.addView(outputControls.spaced(0, 0, 0, 8))
        // Process-text is known only after receiveIntent(). The result action is always present.
        page.addView(button("Wstaw w miejsce zaznaczenia (jeśli wybrano „Przetłumacz”)") { replaceSelection() }.spaced(0, 0, 0, 12))
        page.addView(button("Historia ostatnich tłumaczeń") { showHistory() }.spaced(0, 0, 0, 8))
        page.addView(button("Włącz pływający przycisk na ekranie") {
            AlertDialog.Builder(this).setTitle("Opcjonalna Dostępność")
                .setMessage("Po włączeniu tej usługi pojawi się przycisk T. Po dotknięciu może odczytać tekst dostępny w aktualnym oknie, także w innych aplikacjach. Tekst zostanie wysłany do OpenAI dopiero po zatwierdzeniu na ekranie. Usługa ma szerokie uprawnienia: włącz ją tylko, jeśli ufasz tej aplikacji. Możesz ją wyłączyć w ustawieniach Dostępności.")
                .setNegativeButton("Anuluj", null)
                .setPositiveButton("Otwórz ustawienia") { _, _ -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                .show()
        }.spaced(0, 0, 0, 10))
        page.addView(text("Prywatność: tekst wysyłany jest do OpenAI wyłącznie po Twojej czynności. Historia pozostaje w pamięci urządzenia. Logowanie odbywa się w przeglądarce, bez podawania hasła aplikacji.", 12f, false, Color.GRAY))
    }

    private fun updateConnection() {
        connectButton.text = if (client.signedIn()) "Konto ChatGPT połączone • połącz ponownie" else "Połącz z ChatGPT"
        status.text = if (client.signedIn()) "Połączono: ${client.email().ifBlank { "konto ChatGPT" }}" else "Brak połączenia. Aby tłumaczyć, zaloguj się do ChatGPT."
    }

    private fun connect() {
        connectButton.isEnabled = false
        status.text = "Otwieram bezpieczne logowanie w przeglądarce…"
        client.beginSignIn(this) { result ->
            connectButton.isEnabled = true
            result.onSuccess { updateConnection(); Toast.makeText(this, "Konto ChatGPT połączone", Toast.LENGTH_LONG).show() }
                .onFailure { status.text = "Logowanie nieudane: ${it.message}" }
        }
    }

    private fun translateNow() {
        if (busy) return
        val from = source.selectedItem.toString()
        val to = target.selectedItem.toString()
        val mode = style.selectedItem.toString()
        val original = input.text.toString()
        if (!client.signedIn()) { status.text = "Najpierw połącz konto ChatGPT."; return }
        if (original.isBlank()) { status.text = "Wklej lub wpisz tekst."; return }
        if (from == to) { status.text = "Wybierz różne języki."; return }
        settings.edit().putInt("source_idx", source.selectedItemPosition).apply()
        busy = true; translateButton.isEnabled = false
        output.text = "" // stary wynik nie może zostać przypadkiem skopiowany
        status.text = "Trwa tłumaczenie…"
        Thread {
            val result = runCatching { client.translate(original, from, to, mode) }
            runOnUiThread {
                busy = false; translateButton.isEnabled = true
                result.onSuccess { translated ->
                    output.text = translated
                    status.text = "Tłumaczenie gotowe."
                    saveHistory(original, translated, from, to)
                }.onFailure { status.text = "Błąd: ${it.message ?: "nieznany"}" }
            }
        }.start()
    }

    private fun saveHistory(original: String, translated: String, from: String, to: String) {
        val arr = try { JSONArray(settings.getString("history", "[]")) } catch (_: Exception) { JSONArray() }
        val next = JSONArray().put(JSONObject().put("from", from).put("to", to)
            .put("original", original.take(10000)).put("translated", translated.take(10000)))
        for (i in 0 until minOf(29, arr.length())) next.put(arr.get(i))
        settings.edit().putString("history", next.toString()).apply()
    }

    private fun showHistory() {
        val arr = try { JSONArray(settings.getString("history", "[]")) } catch (_: Exception) { JSONArray() }
        if (arr.length() == 0) { Toast.makeText(this, "Historia jest pusta.", Toast.LENGTH_SHORT).show(); return }
        val items = (0 until arr.length()).map { i ->
            val it = arr.getJSONObject(i)
            "${it.optString("from")} → ${it.optString("to")}: ${it.optString("original").take(50)}"
        }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Historia tłumaczeń").setItems(items) { _, i ->
            val item = arr.getJSONObject(i)
            input.setText(item.optString("original"))
            output.text = item.optString("translated")
            source.setSelection(languages.indexOf(item.optString("from")).coerceAtLeast(0))
            target.setSelection(targets.indexOf(item.optString("to")).coerceAtLeast(0))
        }.setNegativeButton("Zamknij", null).setNeutralButton("Usuń historię") { _, _ ->
            settings.edit().remove("history").apply()
        }.show()
    }

    private fun paste() {
        val clip = (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
        val text = clip?.getItemAt(0)?.coerceToText(this)?.toString()
        if (!text.isNullOrBlank()) input.setText(text) else Toast.makeText(this, "Schowek jest pusty.", Toast.LENGTH_SHORT).show()
    }
    private fun copyResult() {
        val result = output.text.toString()
        if (result.isBlank() || result == "Tutaj pojawi się tłumaczenie.") return
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(android.content.ClipData.newPlainText("Tłumaczenie", result))
        Toast.makeText(this, "Skopiowano tłumaczenie", Toast.LENGTH_SHORT).show()
    }
    private fun shareResult() {
        val result = output.text.toString()
        if (result.isBlank() || result == "Tutaj pojawi się tłumaczenie.") return
        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, result) }
        startActivity(Intent.createChooser(send, "Udostępnij tłumaczenie"))
    }
    private fun replaceSelection() {
        if (!processText || processReadOnly) {
            Toast.makeText(this, "Ta opcja działa po zaznaczeniu tekstu w edytowalnym polu.", Toast.LENGTH_LONG).show()
            return
        }
        val result = output.text.toString()
        if (result.isNotBlank() && result != "Tutaj pojawi się tłumaczenie.") {
            setResult(RESULT_OK, Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, result)); finish()
        }
    }

    private fun spinner(items: Array<String>) = Spinner(this).apply {
        adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, items)
        minimumHeight = dp(48)
        setPadding(dp(8), 0, dp(8), 0)
        background = panelBackground()
    }
    private fun text(s: String, size: Float, bold: Boolean = false, color: Int = Color.rgb(30, 44, 62)) = TextView(this).apply {
        text = s; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun button(s: String, action: () -> Unit) = Button(this).apply {
        text = s; isAllCaps = false; textSize = 13f
        setOnClickListener { action() }
    }
    private fun panelBackground() = GradientDrawable().apply {
        setColor(Color.WHITE)
        setStroke(dp(1), Color.rgb(210, 222, 235))
        cornerRadius = dp(10).toFloat()
    }
    private fun View.weighted() = apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    }
    private fun View.spaced(l: Int, t: Int, r: Int, b: Int) = apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(dp(l), dp(t), dp(r), dp(b))
        }
    }
    private fun dp(px: Int) = (px * resources.displayMetrics.density).toInt()
}

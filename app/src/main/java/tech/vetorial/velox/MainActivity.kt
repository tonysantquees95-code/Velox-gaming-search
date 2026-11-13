package tech.vetorial.velox

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.io.ByteArrayInputStream

/** Velox Search: navegador com abas, bloqueio de anúncios, temas e downloads. */
class MainActivity : Activity() {

    private lateinit var ajustes: Ajustes
    private lateinit var tema: Tema
    private val abas = mutableListOf<Aba>()
    private var atual: Aba? = null

    private lateinit var campo: EditText
    private lateinit var progresso: ProgressBar
    private lateinit var conteudo: FrameLayout
    private lateinit var contador: TextView
    private lateinit var barraDeCima: LinearLayout
    private lateinit var barraDeBaixo: LinearLayout

    private var envio: ValueCallback<Array<Uri>>? = null
    private var telaCheia: View? = null
    private var telaCheiaRetorno: WebChromeClient.CustomViewCallback? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    override fun onCreate(estado: Bundle?) {
        super.onCreate(estado)
        ajustes = Ajustes(this)
        tema = Tema.porId(ajustes.tema)
        montarTela()
        val pedido = intent?.dataString
        val salvas = ajustes.abas
        if (pedido != null) {
            novaAba(pedido, false)
        } else if (salvas.isNotEmpty()) {
            for (u in salvas) novaAba(u, false, exibir = false)
            mostrar(abas[ajustes.abaAtual.coerceIn(0, abas.size - 1)])
        } else {
            novaAba(null, false)
        }
    }

    override fun onNewIntent(nova: Intent?) {
        super.onNewIntent(nova)
        val u = nova?.dataString
        if (u != null) novaAba(u, false)
    }

    override fun onPause() {
        super.onPause()
        val normais = abas.filter { !it.anonima }
        ajustes.abas = normais.map { if (it.url.isEmpty()) INICIO else it.url }
        val a = atual
        ajustes.abaAtual = if (a == null) 0 else normais.indexOf(a).coerceAtLeast(0)
        atual?.webView?.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onResume() {
        super.onResume()
        atual?.webView?.onResume()
    }

    override fun onDestroy() {
        for (a in abas) a.webView.destroy()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- tela

    private fun montarTela() {
        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(tema.fundo)
            fitsSystemWindows = true
        }

        barraDeCima = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setBackgroundColor(tema.superficie)
        }
        val marca = TextView(this).apply {
            text = "VX"
            setTextColor(tema.destaque)
            setTypeface(typeface, Typeface.BOLD_ITALIC)
            textSize = 18f
            letterSpacing = 0.04f
            setPadding(dp(4), 0, dp(10), 0)
            setOnClickListener { abrirTexto(INICIO) }
            contentDescription = "Página inicial do Velox"
        }
        campo = EditText(this).apply {
            hint = "Pesquisar ou digitar endereço"
            setHintTextColor(tema.suave)
            setTextColor(tema.texto)
            textSize = 15f
            isSingleLine = true
            setSelectAllOnFocus(true)
            imeOptions = EditorInfo.IME_ACTION_GO
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setPadding(dp(16), 0, dp(16), 0)
            background = GradientDrawable().apply {
                setColor(tema.campo)
                cornerRadius = dp(22).toFloat()
                setStroke(dp(1), (tema.destaque and 0x00FFFFFF) or 0x66000000)
            }
            setOnEditorActionListener { _, id, ev ->
                if (id == EditorInfo.IME_ACTION_GO || (ev != null && ev.keyCode == KeyEvent.KEYCODE_ENTER && ev.action == KeyEvent.ACTION_DOWN)) {
                    abrirTexto(text.toString())
                    true
                } else false
            }
        }
        barraDeCima.addView(marca, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(44)))
        barraDeCima.addView(campo, LinearLayout.LayoutParams(0, dp(44), 1f))

        progresso = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            visibility = View.GONE
            progressDrawable.setTint(tema.destaque)
        }

        conteudo = FrameLayout(this)

        barraDeBaixo = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(tema.superficie)
        }
        fun botao(texto: String, descricao: String, acao: () -> Unit): TextView = TextView(this).apply {
            text = texto
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(tema.texto)
            contentDescription = descricao
            isClickable = true
            isFocusable = true
            setOnClickListener { acao() }
        }
        val bVoltar = botao("◀", "Voltar") { voltar() }
        val bAvancar = botao("▶", "Avançar") { atual?.webView?.let { if (it.canGoForward()) it.goForward() } }
        val bInicio = botao("⌂", "Início") { abrirTexto(INICIO) }
        val bRecarregar = botao("↻", "Recarregar") { atual?.webView?.reload() }
        contador = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(tema.destaque)
            setTypeface(typeface, Typeface.BOLD)
            contentDescription = "Abas"
            isClickable = true
            isFocusable = true
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                cornerRadius = dp(6).toFloat()
                setStroke(dp(2), tema.destaque)
            }
            setOnClickListener { mostrarAbas() }
        }
        val bMenu = botao("⋮", "Menu") { mostrarMenu() }
        val altura = dp(52)
        for (b in listOf(bVoltar, bAvancar, bInicio, bRecarregar)) barraDeBaixo.addView(b, LinearLayout.LayoutParams(0, altura, 1f))
        val caixaAbas = FrameLayout(this)
        caixaAbas.addView(contador, FrameLayout.LayoutParams(dp(30), dp(28), Gravity.CENTER))
        barraDeBaixo.addView(caixaAbas, LinearLayout.LayoutParams(0, altura, 1f))
        barraDeBaixo.addView(bMenu, LinearLayout.LayoutParams(0, altura, 1f))

        raiz.addView(barraDeCima, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        raiz.addView(progresso, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)))
        raiz.addView(conteudo, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        raiz.addView(barraDeBaixo, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, altura))
        setContentView(raiz)

        window.statusBarColor = tema.superficie
        window.navigationBarColor = tema.superficie
        if (Build.VERSION.SDK_INT >= 30) {
            val flags = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            window.insetsController?.setSystemBarsAppearance(if (tema.claro) flags else 0, flags)
        } else if (tema.claro) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    // ---------------------------------------------------------------- abas

    private fun enderecoInicial(): String =
        INICIO + "?bg=" + tema.semJogo(tema.fundo) + "&sup=" + tema.semJogo(tema.superficie) + "&campo=" + tema.semJogo(tema.campo) +
            "&c1=" + tema.semJogo(tema.destaque) + "&c2=" + tema.semJogo(tema.secundaria) + "&tx=" + tema.semJogo(tema.texto) + "&su=" + tema.semJogo(tema.suave)

    private fun configurar(wv: WebView, anonima: Boolean) {
        wv.setBackgroundColor(tema.fundo)
        val s = wv.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = !anonima
        s.databaseEnabled = !anonima
        s.loadWithOverviewMode = true
        s.useWideViewPort = true
        s.setSupportZoom(true)
        s.builtInZoomControls = true
        s.displayZoomControls = false
        s.mediaPlaybackRequiresUserGesture = true
        s.allowFileAccess = false
        s.allowContentAccess = false
        s.saveFormData = false
        s.cacheMode = if (anonima) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
        s.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        aplicarModoComputador(wv)
        CookieManager.getInstance().setAcceptThirdPartyCookies(wv, !anonima)
    }

    private fun aplicarModoComputador(wv: WebView) {
        val s = wv.settings
        if (ajustes.siteParaComputador) {
            val ua = WebSettings.getDefaultUserAgent(this)
            s.userAgentString = ua.replace(Regex("\\(Linux; Android[^)]*\\)"), "(X11; Linux x86_64)").replace(" Mobile", "")
            s.useWideViewPort = true
        } else {
            s.userAgentString = null
        }
    }

    private fun novaAba(url: String?, anonima: Boolean, exibir: Boolean = true) {
        val wv = WebView(this)
        val aba = Aba(wv, anonima)
        configurar(wv, anonima)
        wv.webViewClient = navegador(aba)
        wv.webChromeClient = cromo(aba)
        wv.setDownloadListener { u, ua, disp, tipo, _ -> baixar(u, ua, disp, tipo) }
        abas.add(aba)
        val alvo = when {
            url == null || url == INICIO -> enderecoInicial()
            else -> url
        }
        aba.url = if (url == null || url == INICIO) "" else alvo
        if (exibir) mostrar(aba)
        wv.loadUrl(alvo)
        atualizarContador()
    }

    private fun mostrar(aba: Aba) {
        atual?.let { conteudo.removeView(it.webView) }
        atual = aba
        conteudo.addView(aba.webView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        campo.setText(Enderecos.paraMostrar(aba.url))
        progresso.visibility = View.GONE
        atualizarContador()
    }

    private fun atualizarContador() {
        contador.text = abas.size.toString()
    }

    private fun fecharAba(aba: Aba) {
        val i = abas.indexOf(aba)
        if (i < 0) return
        abas.removeAt(i)
        conteudo.removeView(aba.webView)
        aba.webView.destroy()
        if (aba.anonima && abas.none { it.anonima }) limparRastros()
        if (abas.isEmpty()) {
            atual = null
            novaAba(null, false)
        } else if (aba === atual) {
            atual = null
            mostrar(abas[(i - 1).coerceAtLeast(0)])
        }
        atualizarContador()
    }

    private fun limparRastros() {
        CookieManager.getInstance().removeAllCookies(null)
        android.webkit.WebStorage.getInstance().deleteAllData()
    }

    private fun mostrarAbas() {
        val nomes = abas.map { (if (it.anonima) "🕶 " else "") + (it.titulo.ifBlank { "Nova aba" }) + (if (it === atual) "  ●" else "") }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Abas (${abas.size})")
            .setItems(nomes) { _, i -> mostrar(abas[i]) }
            .setPositiveButton("Nova aba") { _, _ -> novaAba(null, false) }
            .setNeutralButton("Aba anônima") { _, _ -> novaAba(null, true) }
            .setNegativeButton("Fechar esta aba") { _, _ -> atual?.let { fecharAba(it) } }
            .show()
    }

    // ---------------------------------------------------------------- navegação

    private fun abrirTexto(texto: String) {
        val aba = atual ?: return
        if (texto == INICIO) {
            aba.webView.loadUrl(enderecoInicial())
            return
        }
        val url = Enderecos.resolver(texto, Buscador.porId(ajustes.buscador))
        if (url.isEmpty()) return
        aba.webView.loadUrl(url)
        aba.webView.requestFocus()
        val im = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        im.hideSoftInputFromWindow(campo.windowToken, 0)
    }

    private fun voltar() {
        val wv = atual?.webView ?: return
        if (wv.canGoBack()) wv.goBack()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (telaCheia != null) {
            fecharTelaCheia()
            return
        }
        val a = atual
        if (a != null && a.webView.canGoBack()) a.webView.goBack()
        else if (a != null && abas.size > 1) fecharAba(a)
        else super.onBackPressed()
    }

    private fun navegador(aba: Aba) = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean {
            val u = req.url
            val esquema = u.scheme ?: ""
            if (esquema == "velox") {
                val q = u.getQueryParameter("q") ?: ""
                if (q.isNotBlank()) view.loadUrl(Enderecos.resolver(q, Buscador.porId(ajustes.buscador)))
                return true
            }
            if (esquema == "http" || esquema == "https" || esquema == "file" || esquema == "about") return false
            return abrirFora(u.toString())
        }

        override fun shouldInterceptRequest(view: WebView, req: WebResourceRequest): WebResourceResponse? {
            if (ajustes.bloquearAnuncios && ListaDeAnuncios.bloqueia(req.url.host)) {
                aba.bloqueados.incrementAndGet()
                return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
            }
            return null
        }

        override fun onPageStarted(view: WebView, url: String, icone: Bitmap?) {
            aba.bloqueados.set(0)
            if (!url.startsWith("file:///android_asset/")) aba.url = url else aba.url = ""
            if (aba === atual) campo.setText(Enderecos.paraMostrar(aba.url))
        }

        override fun onPageFinished(view: WebView, url: String) {
            if (!url.startsWith("file:///android_asset/")) aba.url = url
            if (aba === atual) {
                campo.setText(Enderecos.paraMostrar(aba.url))
                progresso.visibility = View.GONE
            }
        }
    }

    private fun abrirFora(url: String): Boolean {
        return try {
            val i = if (url.startsWith("intent:")) Intent.parseUri(url, Intent.URI_INTENT_SCHEME) else Intent(Intent.ACTION_VIEW, Uri.parse(url))
            i.addCategory(Intent.CATEGORY_BROWSABLE)
            i.component = null
            i.selector = null
            startActivity(i)
            true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Nenhum app abre este link.", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            true
        }
    }

    private fun cromo(aba: Aba) = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, novo: Int) {
            if (aba !== atual) return
            progresso.progress = novo
            progresso.visibility = if (novo in 1..99) View.VISIBLE else View.GONE
        }

        override fun onReceivedTitle(view: WebView, titulo: String?) {
            aba.titulo = if (titulo.isNullOrBlank() || titulo == "Velox") "Nova aba" else titulo
        }

        override fun onShowFileChooser(view: WebView, retorno: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
            envio?.onReceiveValue(null)
            envio = retorno
            return try {
                startActivityForResult(params.createIntent(), PEDIDO_ARQUIVO)
                true
            } catch (e: Exception) {
                envio = null
                false
            }
        }

        override fun onShowCustomView(view: View, retorno: CustomViewCallback) {
            if (telaCheia != null) {
                retorno.onCustomViewHidden()
                return
            }
            telaCheia = view
            telaCheiaRetorno = retorno
            (window.decorView as ViewGroup).addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            view.setBackgroundColor(Color.BLACK)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }

        override fun onHideCustomView() {
            fecharTelaCheia()
        }
    }

    private fun fecharTelaCheia() {
        val v = telaCheia ?: return
        (window.decorView as ViewGroup).removeView(v)
        telaCheia = null
        telaCheiaRetorno?.onCustomViewHidden()
        telaCheiaRetorno = null
        window.decorView.systemUiVisibility = if (tema.claro && Build.VERSION.SDK_INT < 30) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else View.SYSTEM_UI_FLAG_VISIBLE
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(pedido: Int, resultado: Int, dados: Intent?) {
        super.onActivityResult(pedido, resultado, dados)
        if (pedido == PEDIDO_ARQUIVO) {
            envio?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultado, dados))
            envio = null
        }
    }

    // ---------------------------------------------------------------- downloads

    private fun baixar(url: String, userAgent: String?, disposicao: String?, tipo: String?) {
        try {
            val nome = URLUtil.guessFileName(url, disposicao, tipo)
            val pedido = DownloadManager.Request(Uri.parse(url))
            pedido.setMimeType(tipo)
            CookieManager.getInstance().getCookie(url)?.let { pedido.addRequestHeader("Cookie", it) }
            if (!userAgent.isNullOrEmpty()) pedido.addRequestHeader("User-Agent", userAgent)
            pedido.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            pedido.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, nome)
            (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(pedido)
            Toast.makeText(this, "Baixando $nome na pasta Download", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Não consegui baixar: ${e.message ?: "erro"}", Toast.LENGTH_LONG).show()
        }
    }

    // ---------------------------------------------------------------- menu

    private fun mostrarMenu() {
        val aba = atual
        val bloqueados = aba?.bloqueados?.get() ?: 0
        val itens = arrayOf(
            "Aba nova",
            "Aba anônima",
            "Tema: ${tema.nome}",
            "Buscador: ${Buscador.porId(ajustes.buscador).nome}",
            "Bloquear anúncios: ${if (ajustes.bloquearAnuncios) "ligado" else "desligado"} ($bloqueados nesta página)",
            "Site para computador: ${if (ajustes.siteParaComputador) "ligado" else "desligado"}",
            "Compartilhar esta página",
            "Limpar histórico, cookies e dados",
            "Sobre o Velox"
        )
        AlertDialog.Builder(this)
            .setTitle("Velox")
            .setItems(itens) { _, i ->
                when (i) {
                    0 -> novaAba(null, false)
                    1 -> novaAba(null, true)
                    2 -> escolherTema()
                    3 -> escolherBuscador()
                    4 -> { ajustes.bloquearAnuncios = !ajustes.bloquearAnuncios; aba?.webView?.reload() }
                    5 -> { ajustes.siteParaComputador = !ajustes.siteParaComputador; for (a in abas) aplicarModoComputador(a.webView); aba?.webView?.reload() }
                    6 -> compartilhar()
                    7 -> limparTudo()
                    8 -> sobre()
                }
            }
            .show()
    }

    private fun escolherTema() {
        val nomes = Tema.TODOS.map { it.nome }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Tema")
            .setItems(nomes) { _, i ->
                ajustes.tema = Tema.TODOS[i].id
                recreate()
            }
            .show()
    }

    private fun escolherBuscador() {
        val lista = Buscador.values()
        AlertDialog.Builder(this)
            .setTitle("Buscador")
            .setItems(lista.map { it.nome }.toTypedArray()) { _, i -> ajustes.buscador = lista[i].id }
            .show()
    }

    private fun compartilhar() {
        val u = atual?.url ?: ""
        if (u.isEmpty()) return
        val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, u)
        startActivity(Intent.createChooser(i, "Compartilhar"))
    }

    private fun limparTudo() {
        AlertDialog.Builder(this)
            .setTitle("Limpar dados?")
            .setMessage("Apaga histórico, cookies, cache e dados dos sites. Você sai das contas em que estiver.")
            .setPositiveButton("Limpar") { _, _ ->
                limparRastros()
                for (a in abas) { a.webView.clearHistory(); a.webView.clearCache(true); a.webView.clearFormData() }
                Toast.makeText(this, "Dados apagados.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun sobre() {
        val versao = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (e: Exception) { "" }
        AlertDialog.Builder(this)
            .setTitle("Velox Search $versao")
            .setMessage("Navegador da Vetorial Tech.\nBloqueio de anúncios: ${ListaDeAnuncios.tamanho()} domínios.\nAba anônima: não guarda histórico nem cache e apaga cookies quando a última anônima fecha.")
            .setPositiveButton("Ok", null)
            .show()
    }

    companion object {
        const val INICIO = "file:///android_asset/inicio.html"
        const val PEDIDO_ARQUIVO = 1
    }
}

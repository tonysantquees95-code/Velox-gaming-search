package tech.vetorial.velox

import android.app.Activity
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.app.AlertDialog
import android.graphics.drawable.GradientDrawable
import android.util.Base64
import android.view.WindowManager
import android.widget.ImageView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicLong
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
import android.widget.HorizontalScrollView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.ByteArrayInputStream

/** Velox Search: navegador com abas, bloqueio de anúncios, temas e downloads. */
class MainActivity : Activity() {

    private lateinit var ajustes: Ajustes
    private lateinit var tema: Tema
    private val abas = mutableListOf<Aba>()
    private var atual: Aba? = null

    private lateinit var raizApp: FrameLayout
    private lateinit var coluna: LinearLayout
    private lateinit var campo: EditText
    private lateinit var barra: BarraNeon
    private lateinit var conteudo: FrameLayout
    private lateinit var contador: TextView
    private lateinit var barraDeCima: LinearLayout
    private lateinit var barraDeBaixo: LinearLayout
    private lateinit var logo: LogoVgx
    private lateinit var flutuante: ImageView
    private val botoes = mutableMapOf<String, ImageView>()
    private val iconesProprios = HashMap<String, Bitmap?>()
    private lateinit var fundoView: FundoView
    private var fotoFundo: Bitmap? = null
    private var iconeEscolhido: String? = null
    private lateinit var bVoltar: ImageView
    private lateinit var bAvancar: ImageView
    private var painel: WebView? = null
    private lateinit var faixaFav: HorizontalScrollView
    private lateinit var faixaConteudo: LinearLayout
    private lateinit var bEstrelaCima: TextView
    private lateinit var bEstrelaBaixo: TextView
    private lateinit var bDownload: BotaoDownload
    private val baixando = HashSet<Long>()

    private val bloqueadosTotal = AtomicLong(0)
    private val bloqueadosSessao = AtomicLong(0)
    private var aberturaFeita = false

    private var envio: ValueCallback<Array<Uri>>? = null
    private var telaCheia: View? = null
    private var telaCheiaRetorno: WebChromeClient.CustomViewCallback? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    override fun onCreate(estado: Bundle?) {
        super.onCreate(estado)
        ajustes = Ajustes(this)
        try { Imagens.restaurarFundoSeSumiu(this) } catch (e: Exception) { /* segue sem fundo */ }
        tema = Tema.daString(ajustes.temaTexto)
        bloqueadosTotal.set(ajustes.bloqueadosTotal)
        montarTela()
        aplicarOpcoesSalvas()
        procurarAtualizacao(false)
        raizApp.postDelayed({ perguntarNavegadorPrincipal() }, 2600)
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
        ajustes.bloqueadosTotal = bloqueadosTotal.get()
        atual?.webView?.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onResume() {
        super.onResume()
        atual?.webView?.onResume()
        aplicarImersivo()
    }

    override fun onWindowFocusChanged(temFoco: Boolean) {
        super.onWindowFocusChanged(temFoco)
        if (temFoco) aplicarImersivo()
    }

    override fun onDestroy() {
        painel?.destroy()
        for (a in abas) a.webView.destroy()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- tela

    private fun montarTela() {
        raizApp = FrameLayout(this)
        fundoView = FundoView(this)
        raizApp.addView(fundoView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        coluna = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            fitsSystemWindows = true
        }

        barraDeCima = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        logo = LogoVgx(this).apply {
            setOnClickListener { abrirTexto(INICIO) }
            isClickable = true
            contentDescription = "Página inicial do Velox"
        }
        campo = EditText(this).apply {
            hint = "Pesquisar ou digitar endereço"
            textSize = 15f
            isSingleLine = true
            setSelectAllOnFocus(true)
            imeOptions = EditorInfo.IME_ACTION_GO
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setPadding(dp(16), 0, dp(16), 0)
            setOnFocusChangeListener { _, _ -> pintarCampo() }
            setOnEditorActionListener { _, id, ev ->
                if (id == EditorInfo.IME_ACTION_GO || (ev != null && ev.keyCode == KeyEvent.KEYCODE_ENTER && ev.action == KeyEvent.ACTION_DOWN)) {
                    abrirTexto(text.toString())
                    true
                } else false
            }
        }
        bEstrelaCima = TextView(this).apply {
            text = "★"
            textSize = 22f
            gravity = Gravity.CENTER
            contentDescription = "Favoritos"
            isClickable = true
            setOnClickListener { abrirPainel("favoritos", true) }
        }
        barraDeCima.addView(logo, LinearLayout.LayoutParams(dp(66), dp(34)).apply { rightMargin = dp(8) })
        barraDeCima.addView(campo, LinearLayout.LayoutParams(0, dp(44), 1f))
        barraDeCima.addView(bEstrelaCima, LinearLayout.LayoutParams(dp(44), dp(44)).apply { leftMargin = dp(4) })
        bDownload = BotaoDownload(this).apply {
            contentDescription = "Downloads"
            isClickable = true
            setOnClickListener { abrirDownloads() }
        }
        barraDeCima.addView(bDownload, LinearLayout.LayoutParams(dp(44), dp(44)))

        barra = BarraNeon(this)
        conteudo = FrameLayout(this)

        barraDeBaixo = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        fun botao(slot: String, descricao: String, acao: () -> Unit): ImageView = ImageView(this).apply {
            setImageDrawable(IconeTraco.criar(slot))
            botoes[slot] = this
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(15), dp(15), dp(15), dp(15))
            contentDescription = descricao
            isClickable = true
            isFocusable = true
            setOnClickListener { acao() }
        }
        bVoltar = botao("voltar", "Voltar") { voltar() }
        bAvancar = botao("avancar", "Avançar") { atual?.webView?.let { if (it.canGoForward()) it.goForward() } }
        val bInicio = botao("inicio", "Início") { abrirTexto(INICIO) }
        val bRecarregar = botao("recarregar", "Recarregar") { atual?.webView?.reload() }
        contador = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            contentDescription = "Abas"
            isClickable = true
            isFocusable = true
            setOnClickListener { abrirPainel("abas") }
        }
        bEstrelaBaixo = TextView(this).apply {
            text = "★"
            textSize = 22f
            gravity = Gravity.CENTER
            contentDescription = "Faixa de favoritos"
            isClickable = true
            isFocusable = true
            setOnClickListener { alternarFaixaFav() }
        }
        val bPainel = botao("painel", "Painel do Velox: modo jogo, visual e mais") { abrirPainel("jogo") }
        val altura = dp(54)
        for (b in listOf(bVoltar, bAvancar, bInicio, bRecarregar)) barraDeBaixo.addView(b, LinearLayout.LayoutParams(0, altura, 1f))
        val caixaAbas = FrameLayout(this)
        caixaAbas.addView(contador, FrameLayout.LayoutParams(dp(32), dp(28), Gravity.CENTER))
        barraDeBaixo.addView(bEstrelaBaixo, LinearLayout.LayoutParams(0, altura, 1f))
        barraDeBaixo.addView(caixaAbas, LinearLayout.LayoutParams(0, altura, 1f))
        barraDeBaixo.addView(bPainel, LinearLayout.LayoutParams(0, altura, 1f))

        coluna.addView(barraDeCima, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        coluna.addView(barra, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)))
        coluna.addView(conteudo, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        faixaConteudo = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(10), dp(6), dp(10), dp(6)) }
        faixaFav = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; visibility = View.GONE; addView(faixaConteudo) }
        coluna.addView(faixaFav, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        coluna.addView(barraDeBaixo, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, altura).apply { setMargins(dp(10), dp(4), dp(10), dp(8)) })

        // no modo imersivo as barras somem; este botão pequeno traz o painel de volta
        flutuante = ImageView(this).apply {
            setImageDrawable(IconeTraco.criar("painel"))
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(11), dp(11), dp(11), dp(11))
            contentDescription = "Abrir o painel do Velox"
            visibility = View.GONE
            isClickable = true
            setOnClickListener { abrirPainel("jogo") }
        }
        raizApp.addView(coluna, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        raizApp.addView(flutuante, FrameLayout.LayoutParams(dp(44), dp(44), Gravity.BOTTOM or Gravity.END).apply { rightMargin = dp(14); bottomMargin = dp(70) })
        setContentView(raizApp)
        fotoFundo = Imagens.carregarFundo(this)
        aplicarCores()
    }

    private fun raio(): Float = if (tema.redondo) dp(22).toFloat() else dp(7).toFloat()

    private fun pintarCampo() {
        val alfa = if (campo.hasFocus()) 0xFF else 0x80
        campo.background = PainelCortado((tema.campo and 0x00FFFFFF) or ((200 - (tema.transparencia * 1.6f).toInt()).coerceIn(30, 255) shl 24), (tema.destaque and 0x00FFFFFF) or (alfa shl 24), dp(if (campo.hasFocus()) 2 else 1).toFloat(),
            if (tema.redondo) dp(22).toFloat() else dp(10).toFloat(), tema.redondo)
    }

    /** Aplica as cores do tema em tudo que já está na tela (vale na hora, sem reiniciar). */
    private fun aplicarCores() {
        val comFoto = fotoFundo != null
        coluna.setBackgroundColor(Color.TRANSPARENT)
        conteudo.setBackgroundColor(Color.TRANSPARENT)
        val tr = tema.transparencia
        val alfa = (255 - (tr * 1.6f).toInt()).coerceIn(60, 255) shl 24
        val a = (tema.superficie and 0x00FFFFFF) or alfa
        val b = (LogoVgx.misturar(tema.superficie, tema.fundo, 0.55f) and 0x00FFFFFF) or alfa
        barraDeCima.background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(a, b))
        barraDeBaixo.background = PainelCortado((tema.superficie and 0x00FFFFFF) or ((255 - (tr * 1.5f).toInt()).coerceIn(70, 255) shl 24), (tema.destaque and 0x00FFFFFF) or 0xA0000000.toInt(),
            dp(1).toFloat() + 0.5f, if (tema.redondo) dp(28).toFloat() else dp(14).toFloat(), tema.redondo)
        bEstrelaCima.setTextColor(tema.destaque)
        bDownload.cor(tema.destaque)
        bEstrelaBaixo.setTextColor(tema.destaque)
        faixaFav.setBackgroundColor((tema.superficie and 0x00FFFFFF) or ((255 - (tema.transparencia * 1.6f).toInt()).coerceIn(60, 255) shl 24))
        desenharFaixaFav()
        campo.setTextColor(tema.texto)
        campo.setHintTextColor(tema.suave)
        pintarCampo()
        logo.cores(tema.destaque, tema.secundaria)
        barra.cores(tema.destaque, tema.secundaria)
        contador.setTextColor(tema.destaque)
        contador.background = GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            cornerRadius = if (tema.redondo) dp(10).toFloat() else dp(3).toFloat()
            setStroke(dp(2), tema.destaque)
        }
        flutuante.setColorFilter(tema.sobreDestaque)
        flutuante.background = GradientDrawable().apply {
            shape = if (tema.redondo) GradientDrawable.OVAL else GradientDrawable.RECTANGLE
            cornerRadius = dp(8).toFloat()
            setColor((tema.destaque and 0x00FFFFFF) or 0xD9000000.toInt())
        }
        window.statusBarColor = tema.superficie
        window.navigationBarColor = tema.fundo
        val vidro = if (ajustes.sitesNoTema) Color.TRANSPARENT else tema.fundo
        for (x in abas) x.webView.setBackgroundColor(vidro)
        aplicarIcones()
        atualizarFundo()
        atualizarSetas()
    }

    /** Foto (ou cor) de fundo atrás das barras e das páginas. Nos sites escurece um pouco mais para o texto continuar legível. */
    private fun atualizarFundo() {
        val emSite = ajustes.sitesNoTema && (atual?.url?.isNotEmpty() == true)
        val escuro = (tema.escurecer + if (emSite && fotoFundo != null) 25 else 0) / 100f
        fundoView.definir(fotoFundo, tema.fundo, escuro)
    }

    /** Ícones da barra: os seus (foto) quando existirem, senão o desenho do pacote escolhido. */
    private fun aplicarIcones() {
        for ((slot, v) in botoes) {
            val proprio = iconesProprios.getOrPut(slot) { Imagens.carregarIcone(this, slot)?.let { Imagens.arredondar(it, 0.22f) } }
            if (proprio != null) {
                v.clearColorFilter()
                v.setImageBitmap(proprio)
                v.setPadding(dp(11), dp(11), dp(11), dp(11))
            } else {
                val d = IconeTraco.criar(slot)
                d.cor = tema.texto
                d.c1 = tema.destaque
                d.c2 = tema.secundaria
                d.pacote = tema.pacote
                v.clearColorFilter()
                v.setImageDrawable(d)
                v.setPadding(dp(15), dp(15), dp(15), dp(15))
            }
        }
    }

    private fun atualizarSetas() {
        val wv = atual?.webView
        bVoltar.alpha = if (wv != null && wv.canGoBack()) 1f else 0.35f
        bAvancar.alpha = if (wv != null && wv.canGoForward()) 1f else 0.35f
        atualizarEstrela()
    }

    // ---------------------------------------------------------------- modo jogo (opções)

    private fun aplicarOpcoesSalvas() {
        aplicarTela()
        aplicarImersivo()
    }

    private fun aplicarTela() {
        if (ajustes.telaLigada) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @Suppress("DEPRECATION")
    private fun aplicarImersivo() {
        if (telaCheia != null) return
        val ligado = ajustes.imersivo
        barraDeCima.visibility = if (ligado) View.GONE else View.VISIBLE
        barraDeBaixo.visibility = if (ligado) View.GONE else View.VISIBLE
        barra.visibility = if (ligado) View.GONE else View.VISIBLE
        flutuante.visibility = if (ligado && painel == null) View.VISIBLE else View.GONE
        window.decorView.systemUiVisibility = if (ligado) {
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        } else View.SYSTEM_UI_FLAG_VISIBLE
    }

    @Suppress("DEPRECATION")
    private fun aplicarEscuro(wv: WebView) {
        val ligar = ajustes.sitesEscuros || ajustes.sitesNoTema
        try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(wv.settings, ligar)
            } else if (Build.VERSION.SDK_INT >= 29) {
                wv.settings.forceDark = if (ligar) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
            }
        } catch (e: Exception) {
            // WebView antigo: fica sem o modo escuro forçado
        }
    }

    // ---------------------------------------------------------------- painel (HTML por cima do navegador)

    private fun estadoDoPainel(): String {
        val lista = JSONArray()
        for (a in abas) {
            val host = try { Uri.parse(a.url).host ?: "" } catch (e: Exception) { "" }
            lista.put(JSONObject().put("t", a.titulo.ifBlank { "Nova aba" }).put("h", host).put("a", a.anonima).put("c", a === atual))
        }
        val bus = JSONArray()
        for (b in Buscador.values()) bus.put(JSONArray().put(b.id).put(b.nome))
        val versao = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (e: Exception) { "" }
        val o = JSONObject()
            .put("abas", lista)
            .put("op", JSONObject().put("ad", ajustes.bloquearAnuncios).put("escuro", ajustes.sitesEscuros).put("imersivo", ajustes.imersivo)
                .put("tela", ajustes.telaLigada).put("pc", ajustes.siteParaComputador).put("notema", ajustes.sitesNoTema))
            .put("bus", ajustes.buscador).put("buscadores", bus)
            .put("bl", JSONObject().put("p", atual?.bloqueados?.get() ?: 0).put("t", bloqueadosTotal.get()))
            .put("v", versao)
            .put("foto", fotoFundo != null)
            .put("vindex", vindexInstalado())
            .put("walls", JSONArray(Imagens.listarWallpapers(this)))
            .put("fav", JSONArray(ajustes.favoritosJson))
            .put("padrao", ehNavegadorPrincipal())
        fotoFundo?.let { o.put("fotoAmostra", Imagens.miniatura(it, 360, true)) }
        val ic = JSONObject()
        for (slot in Imagens.SLOTS) iconesProprios[slot]?.let { ic.put(slot, Imagens.miniatura(it, 72, false)) }
        o.put("icones", ic)
        return Base64.encodeToString(o.toString().toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun abrirPainel(aba: String, esquerda: Boolean = false) {
        fecharPainel()
        val wv = WebView(this)
        wv.setBackgroundColor(Color.TRANSPARENT)
        wv.settings.javaScriptEnabled = true
        wv.settings.allowFileAccess = false
        wv.settings.allowContentAccess = false
        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean = acaoDoPainel(req.url)
            override fun shouldInterceptRequest(view: WebView, req: WebResourceRequest): WebResourceResponse? {
                val u = req.url
                if (u.host == "velox.local" && (u.path ?: "").startsWith("/w/")) {
                    val bytes = try { Imagens.miniaturaWallpaper(this@MainActivity, (u.path ?: "").removePrefix("/w/")) } catch (e: Exception) { null }
                    if (bytes != null) return WebResourceResponse("image/jpeg", null, ByteArrayInputStream(bytes))
                }
                return null
            }
        }
        raizApp.addView(wv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        painel = wv
        flutuante.visibility = View.GONE
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(campo.windowToken, 0)
        wv.loadUrl("file:///android_asset/painel.html?aba=" + aba + (if (esquerda) "&lado=esq" else "") + "&" + tema.params() + "&s=" + estadoDoPainel())
    }

    private fun fecharPainel() {
        val wv = painel ?: return
        if (paginaDownloads === wv) paginaDownloads = null
        raizApp.removeView(wv)
        wv.destroy()
        painel = null
        aplicarImersivo()
    }

    private fun acaoDoPainel(u: Uri): Boolean {
        if (u.scheme != "velox") return true
        when (u.host) {
            "fechar" -> fecharPainel()
            "nova" -> { fecharPainel(); novaAba(null, false) }
            "anonima" -> { fecharPainel(); novaAba(null, true) }
            "aba" -> {
                val i = u.getQueryParameter("i")?.toIntOrNull()
                fecharPainel()
                if (i != null && i in abas.indices) mostrar(abas[i])
            }
            "fechar-aba" -> {
                val i = u.getQueryParameter("i")?.toIntOrNull()
                if (i != null && i in abas.indices) fecharAba(abas[i])
                abrirPainel("abas")
            }
            "opc" -> opcao(u.getQueryParameter("k"), u.getQueryParameter("v") == "1")
            "tema" -> mudarTema(u.encodedQuery)
            "buscador" -> Buscador.values().firstOrNull { it.id == u.getQueryParameter("id") }?.let { ajustes.buscador = it.id }
            "compartilhar" -> { fecharPainel(); compartilhar() }
            "limpar" -> limparDados()
            "atualizar" -> procurarAtualizacao(true)
            "aviso" -> Toast.makeText(this, (u.getQueryParameter("m") ?: "").take(120), Toast.LENGTH_SHORT).show()
            "vindex" -> abrirDownloads()
            "dl-pronto" -> enviarDownloads()
            "dl-abrir" -> u.getQueryParameter("id")?.toLongOrNull()?.let { abrirArquivoBaixado(it) }
            "dl-pasta" -> abrirVindex()
            "dl-apagar" -> { u.getQueryParameter("id")?.toLongOrNull()?.let { id -> try { (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).remove(id); baixando.remove(id) } catch (e: Exception) { /* já não existe */ }; enviarDownloads() } }
            "fav-add" -> { favoritarPaginaAtual(); if (u.getQueryParameter("fica") != "1") fecharPainel() else abrirPainel("favoritos", true) }
            "fav-del" -> { u.getQueryParameter("i")?.toIntOrNull()?.let { removerFavorito(it) }; abrirPainel("favoritos", true) }
            "fav-abrir" -> { fecharPainel(); u.getQueryParameter("u")?.let { abrirTexto(it) } }
            "padrao" -> { fecharPainel(); pedirNavegadorPrincipal() }
            "fundo-galeria" -> escolherImagem(PEDIDO_FUNDO)
            "fundo-usar" -> {
                val n = u.getQueryParameter("n") ?: ""
                if (Imagens.usarWallpaper(this, n)) { fotoFundo = Imagens.carregarFundo(this); depoisDeMudarFundo(); Toast.makeText(this, "Fundo aplicado.", Toast.LENGTH_SHORT).show() }
            }
            "fundo-apagar" -> { Imagens.apagarWallpaper(this, u.getQueryParameter("n") ?: ""); abrirPainel("fundo") }
            "fundo-remover" -> { Imagens.removerFundo(this); fotoFundo = null; depoisDeMudarFundo() }
            "fundo-url" -> imagemDaRede(u.getQueryParameter("u"), null)
            "fundo-buscar" -> buscarImagens(u.getQueryParameter("q"), null)
            "icone-galeria" -> u.getQueryParameter("s")?.takeIf { Imagens.slotValido(it) }?.let { iconeEscolhido = it; escolherImagem(PEDIDO_ICONE) }
            "icone-url" -> imagemDaRede(u.getQueryParameter("u"), u.getQueryParameter("s"))
            "icone-buscar" -> buscarImagens(u.getQueryParameter("q"), u.getQueryParameter("s"))
            "icone-restaurar" -> u.getQueryParameter("s")?.takeIf { Imagens.slotValido(it) }?.let { Imagens.removerIcone(this, it); iconesProprios.remove(it); aplicarIcones() }
        }
        return true
    }

    private fun opcao(chave: String?, ligado: Boolean) {
        when (chave) {
            "ad" -> { ajustes.bloquearAnuncios = ligado; atual?.webView?.reload() }
            "escuro" -> { ajustes.sitesEscuros = ligado; for (a in abas) aplicarEscuro(a.webView); atual?.webView?.reload() }
            "imersivo" -> { ajustes.imersivo = ligado; aplicarImersivo() }
            "tela" -> { ajustes.telaLigada = ligado; aplicarTela() }
            "notema" -> {
                ajustes.sitesNoTema = ligado
                for (a in abas) { aplicarEscuro(a.webView); a.webView.setBackgroundColor(if (ligado) Color.TRANSPARENT else tema.fundo) }
                atualizarFundo()
                atual?.webView?.reload()
            }
            "pc" -> { ajustes.siteParaComputador = ligado; for (a in abas) aplicarModoComputador(a.webView); atual?.webView?.reload() }
        }
    }

    // ---------------------------------------------------------------- favoritos

    private fun listaFavoritos(): MutableList<Pair<String, String>> {
        val r = mutableListOf<Pair<String, String>>()
        try {
            val a = JSONArray(ajustes.favoritosJson)
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                r.add(Pair(o.optString("t"), o.optString("u")))
            }
        } catch (e: Exception) {
            // lista estragada: recomeça vazia
        }
        return r
    }

    private fun gravarFavoritos(l: List<Pair<String, String>>) {
        val a = JSONArray()
        for ((t, u) in l) a.put(JSONObject().put("t", t).put("u", u))
        ajustes.favoritosJson = a.toString()
        desenharFaixaFav()
        atualizarEstrela()
    }

    private fun favoritarPaginaAtual() {
        val a = atual
        val u = a?.url ?: ""
        if (a == null || !(u.startsWith("http://") || u.startsWith("https://"))) {
            Toast.makeText(this, "Abra uma página para favoritar.", Toast.LENGTH_SHORT).show()
            return
        }
        val l = listaFavoritos()
        if (l.any { it.second == u }) {
            Toast.makeText(this, "Essa página já está nos favoritos.", Toast.LENGTH_SHORT).show()
            return
        }
        val host = try { Uri.parse(u).host ?: u } catch (e: Exception) { u }
        l.add(Pair(a.titulo.ifBlank { host }.take(60), u))
        gravarFavoritos(l)
        Toast.makeText(this, "Adicionada aos favoritos.", Toast.LENGTH_SHORT).show()
    }

    private fun removerFavorito(i: Int) {
        val l = listaFavoritos()
        if (i in l.indices) {
            l.removeAt(i)
            gravarFavoritos(l)
        }
    }

    /** A estrela de cima fica vazia (☆) até a página ser favoritada; aí fica cheia (★). */
    private fun atualizarEstrela() {
        if (!::bEstrelaCima.isInitialized) return
        val u = atual?.url ?: ""
        val fav = u.isNotEmpty() && listaFavoritos().any { it.second == u }
        bEstrelaCima.text = if (fav) "★" else "☆"
    }

    private fun alternarFaixaFav() {
        faixaFav.visibility = if (faixaFav.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        desenharFaixaFav()
    }

    /** Faixa pequena que rola para o lado: primeiro "+" para favoritar a página, depois os favoritos. */
    private fun desenharFaixaFav() {
        if (!::faixaConteudo.isInitialized) return
        faixaConteudo.removeAllViews()
        fun chip(texto: String, destaque: Boolean, acao: () -> Unit, longo: (() -> Unit)? = null) {
            val t = TextView(this).apply {
                text = texto
                textSize = 13f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(if (destaque) tema.sobreDestaque else tema.texto)
                gravity = Gravity.CENTER
                setPadding(dp(14), 0, dp(14), 0)
                background = GradientDrawable().apply {
                    cornerRadius = dp(18).toFloat()
                    setColor(if (destaque) tema.destaque else (tema.campo and 0x00FFFFFF) or 0xCC000000.toInt())
                    setStroke(dp(1), (tema.destaque and 0x00FFFFFF) or 0x80000000.toInt())
                }
                isClickable = true
                setOnClickListener { acao() }
                if (longo != null) setOnLongClickListener { longo(); true }
            }
            faixaConteudo.addView(t, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36)).apply { rightMargin = dp(8) })
        }
        chip("＋ Favoritar esta página", true, { favoritarPaginaAtual() })
        val l = listaFavoritos()
        if (l.isEmpty()) chip("Nenhum favorito ainda", false, {})
        for ((i, f) in l.withIndex()) {
            chip(f.first, false, { abrirTexto(f.second) }, {
                Toast.makeText(this, "Favorito removido.", Toast.LENGTH_SHORT).show()
                removerFavorito(i)
            })
        }
    }

    // ---------------------------------------------------------------- navegador principal

    private fun ehNavegadorPrincipal(): Boolean {
        return try {
            val r = packageManager.resolveActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")), android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            r?.activityInfo?.packageName == packageName
        } catch (e: Exception) {
            false
        }
    }

    /** Pede ao Android para tornar o Velox o navegador principal (Android 10+); senão abre os apps padrão. */
    private fun pedirNavegadorPrincipal() {
        if (ehNavegadorPrincipal()) {
            Toast.makeText(this, "O Velox já é o seu navegador principal.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                val rm = getSystemService(android.app.role.RoleManager::class.java)
                if (rm != null && rm.isRoleAvailable(android.app.role.RoleManager.ROLE_BROWSER) && !rm.isRoleHeld(android.app.role.RoleManager.ROLE_BROWSER)) {
                    startActivityForResult(rm.createRequestRoleIntent(android.app.role.RoleManager.ROLE_BROWSER), PEDIDO_PADRAO)
                    return
                }
            }
            startActivity(Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            Toast.makeText(this, "Toque em \"App de navegador\" e escolha o Velox.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            try { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) } catch (e2: Exception) { /* sem tela de ajustes */ }
        }
    }

    /** Só na primeira abertura: pergunta se o Velox deve ser o navegador principal. */
    private fun perguntarNavegadorPrincipal() {
        if (ajustes.perguntouPadrao || isFinishing || ehNavegadorPrincipal()) return
        ajustes.perguntouPadrao = true
        AlertDialog.Builder(this)
            .setTitle("Usar o Velox como navegador principal?")
            .setMessage("Os links que você abrir em outros apps vão abrir aqui, já com o bloqueio de anúncios. Você pode mudar isso depois em Painel > Mais.")
            .setPositiveButton("Sim, usar") { _, _ -> pedirNavegadorPrincipal() }
            .setNegativeButton("Agora não", null)
            .show()
    }

    // ---------------------------------------------------------------- atualização do próprio Velox

    private var atualizando = false

    /** Procura a versão nova, baixa e instala. Automática: no máximo a cada 6 horas e sem barulho. Manual: sempre responde. */
    private fun procurarAtualizacao(manual: Boolean) {
        if (atualizando) return
        if (!manual && !Atualizador.precisaConferir(ajustes.ultimaConferencia, System.currentTimeMillis())) return
        atualizando = true
        if (manual) Toast.makeText(this, "Procurando atualização…", Toast.LENGTH_SHORT).show()
        Thread {
            var aviso: String? = null
            try {
                val v = Atualizador.procurar()
                ajustes.ultimaConferencia = System.currentTimeMillis()
                val minha = Atualizador.instalada(this)
                if (v == null) {
                    if (manual) aviso = "Não consegui consultar as atualizações agora. Confira a internet."
                } else if (!Atualizador.haNova(minha, v.numero)) {
                    if (manual) aviso = "Você já está na versão mais nova (1.$minha)."
                } else if (!Atualizador.podeInstalar(this)) {
                    runOnUiThread { pedirPermissaoDeInstalar(v.numero) }
                } else {
                    runOnUiThread { Toast.makeText(this, "Baixando o Velox 1.${v.numero}…", Toast.LENGTH_LONG).show() }
                    val apk = Atualizador.baixar(this, v)
                    runOnUiThread { try { Atualizador.instalar(this, apk) } catch (e: Exception) { Toast.makeText(this, "Não consegui instalar: ${e.message ?: "erro"}", Toast.LENGTH_LONG).show() } }
                }
            } catch (e: Exception) {
                if (manual) aviso = "A atualização falhou: ${e.message ?: "erro"}"
            } finally {
                atualizando = false
            }
            if (aviso != null) { val m = aviso; runOnUiThread { Toast.makeText(this, m, Toast.LENGTH_LONG).show() } }
        }.start()
    }

    /** O Android só deixa um app se atualizar depois que a pessoa libera "instalar apps desconhecidos" para ele. */
    private fun pedirPermissaoDeInstalar(numero: Int) {
        Toast.makeText(this, "Velox 1.$numero disponível. Libere \"Permitir desta fonte\" para ele se atualizar sozinho.", Toast.LENGTH_LONG).show()
        try {
            startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
        } catch (e: Exception) {
            // sem a tela de permissão neste aparelho: segue sem atualizar
        }
    }

    private fun mudarTema(consulta: String?) {
        val antes = tema
        tema = Tema.daString(consulta)
        ajustes.temaTexto = tema.params()
        aplicarCores()
        if (antes.atalhos != tema.atalhos) recarregarInicios()
    }

    // ---------------------------------------------------------------- abas

    private fun enderecoInicial(): String {
        val abertura = if (aberturaFeita) "" else "&ab=1"
        aberturaFeita = true
        return INICIO + "?" + tema.params() + "&n1=" + bloqueadosSessao.get() + "&n2=" + bloqueadosTotal.get() +
            "&ft=" + (if (fotoFundo != null) 1 else 0) + "&ad=" + (if (ajustes.bloquearAnuncios) 1 else 0) + "&bus=" + Uri.encode(Buscador.porId(ajustes.buscador).nome) + abertura
    }

    private fun configurar(wv: WebView, anonima: Boolean) {
        wv.setBackgroundColor(tema.fundo)
        aplicarEscuro(wv)
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
        wv.setOnLongClickListener { v ->
            val r = (v as WebView).hitTestResult
            val extra = r.extra
            if ((r.type == WebView.HitTestResult.IMAGE_TYPE || r.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) && Imagens.enderecoPermitido(extra)) {
                menuDaImagem(extra!!)
                true
            } else false
        }
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
        barra.progresso = 0
        atualizarContador()
        atualizarSetas()
        atualizarFundo()
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
        if (painel != null) {
            fecharPainel()
            return
        }
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
                bloqueadosSessao.incrementAndGet()
                bloqueadosTotal.incrementAndGet()
                return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
            }
            return null
        }

        override fun onPageStarted(view: WebView, url: String, icone: Bitmap?) {
            aba.bloqueados.set(0)
            if (!url.startsWith("file:///android_asset/")) aba.url = url else aba.url = ""
            if (aba === atual) { campo.setText(Enderecos.paraMostrar(aba.url)); atualizarFundo(); atualizarEstrela() }
        }

        override fun onPageFinished(view: WebView, url: String) {
            if (!url.startsWith("file:///android_asset/")) {
                aba.url = url
                if (ajustes.sitesNoTema) {
                    view.evaluateJavascript(JS_TRANSPARENTE, null)
                    view.postDelayed({ view.evaluateJavascript(JS_TRANSPARENTE, null) }, 1200)
                    view.postDelayed({ view.evaluateJavascript(JS_TRANSPARENTE, null) }, 3500)
                    if (ehGoogle(url)) {
                        view.evaluateJavascript(JS_GOOGLE, null)
                        view.postDelayed({ view.evaluateJavascript(JS_GOOGLE, null) }, 700)
                        view.postDelayed({ view.evaluateJavascript(JS_GOOGLE, null) }, 2200)
                        view.postDelayed({ view.evaluateJavascript(JS_GOOGLE, null) }, 5000)
                    }
                }
            }
            if (aba === atual) {
                atualizarFundo()
                campo.setText(Enderecos.paraMostrar(aba.url))
                barra.progresso = 0
                atualizarSetas()
            }
        }
    }

    private fun ehGoogle(url: String): Boolean {
        val h = try { Uri.parse(url).host ?: "" } catch (e: Exception) { "" }
        return h == "google.com" || h.endsWith(".google.com") || (h.contains(".google.") && !h.endsWith("googleapis.com"))
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
            barra.progresso = if (novo >= 100) 0 else novo
            atualizarSetas()
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
        aplicarImersivo()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(pedido: Int, resultado: Int, dados: Intent?) {
        super.onActivityResult(pedido, resultado, dados)
        if ((pedido == PEDIDO_FUNDO || pedido == PEDIDO_ICONE) && resultado == RESULT_OK) {
            val uri = dados?.data ?: return
            val slot = if (pedido == PEDIDO_ICONE) iconeEscolhido else null
            Thread {
                try {
                    val bytes = contentResolver.openInputStream(uri)?.use { Imagens.lerFluxo(it) } ?: throw IllegalStateException("não abriu")
                    val ok = if (slot == null) Imagens.salvarFundo(this, bytes) else Imagens.salvarIcone(this, slot, bytes)
                    runOnUiThread { imagemSalva(ok, slot) }
                } catch (e: Exception) {
                    runOnUiThread { Toast.makeText(this, "Não consegui usar essa imagem.", Toast.LENGTH_LONG).show() }
                }
            }.start()
            return
        }
        if (pedido == PEDIDO_ARQUIVO) {
            envio?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultado, dados))
            envio = null
        }
    }

    // ---------------------------------------------------------------- foto de fundo e ícones próprios

    private fun escolherImagem(pedido: Int) {
        try {
            startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE), pedido)
        } catch (e: Exception) {
            Toast.makeText(this, "Não achei uma galeria neste aparelho.", Toast.LENGTH_LONG).show()
        }
    }

    private fun imagemDaRede(url: String?, slot: String?) {
        if (!Imagens.enderecoPermitido(url)) { Toast.makeText(this, "Use um endereço de imagem que comece com https://", Toast.LENGTH_LONG).show(); return }
        if (slot != null && !Imagens.slotValido(slot)) return
        Toast.makeText(this, "Baixando a imagem…", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val bytes = Imagens.baixar(url!!)
                val ok = if (slot == null) Imagens.salvarFundo(this, bytes) else Imagens.salvarIcone(this, slot, bytes)
                runOnUiThread { imagemSalva(ok, slot) }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Não consegui baixar a imagem.", Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun imagemSalva(ok: Boolean, slot: String?) {
        if (!ok) { Toast.makeText(this, "Essa imagem não abriu. Tente outra.", Toast.LENGTH_LONG).show(); return }
        if (slot == null) {
            fotoFundo = Imagens.carregarFundo(this)
            depoisDeMudarFundo()
        } else {
            iconesProprios.remove(slot)
            aplicarIcones()
            if (painel != null) abrirPainel("barra")
        }
        Toast.makeText(this, if (slot == null) "Fundo aplicado." else "Ícone aplicado.", Toast.LENGTH_SHORT).show()
    }

    private fun depoisDeMudarFundo() {
        aplicarCores()
        recarregarInicios()
        if (painel != null) abrirPainel("fundo")
    }

    /** As páginas iniciais (do Velox) leem o tema no endereço; recarrega para pegar foto e estilo novos. */
    private fun recarregarInicios() {
        for (a in abas) if (a.webView.url?.startsWith(INICIO) == true) a.webView.loadUrl(enderecoInicial())
    }

    /** Abre a busca de imagens numa aba nova. Segurando o dedo numa imagem aparece "usar como fundo/ícone". */
    private fun buscarImagens(consulta: String?, slot: String?) {
        iconeEscolhido = if (Imagens.slotValido(slot)) slot else null
        fecharPainel()
        val q = Uri.encode((consulta ?: "wallpaper").take(80).ifBlank { "wallpaper" })
        novaAba("https://www.bing.com/images/search?q=$q", false)
        Toast.makeText(this, "Segure o dedo numa imagem para usar no Velox.", Toast.LENGTH_LONG).show()
    }

    private fun menuDaImagem(url: String) {
        val rotulos = mapOf("voltar" to "Voltar", "avancar" to "Avançar", "inicio" to "Início", "recarregar" to "Recarregar", "painel" to "Painel")
        val opcoes = ArrayList<String>()
        val acoes = ArrayList<() -> Unit>()
        val direto = iconeEscolhido
        if (direto != null) { opcoes.add("Usar como ícone: ${rotulos[direto]}"); acoes.add { imagemDaRede(url, direto) } }
        opcoes.add("Usar como fundo do Velox"); acoes.add { imagemDaRede(url, null) }
        opcoes.add("Usar como ícone da barra…"); acoes.add { escolherSlot(url) }
        opcoes.add("Baixar imagem"); acoes.add { baixar(url, null, null, null) }
        AlertDialog.Builder(this).setItems(opcoes.toTypedArray()) { _, i -> acoes[i]() }.show()
    }

    private fun escolherSlot(url: String) {
        val nomes = arrayOf("Voltar", "Avançar", "Início", "Recarregar", "Painel")
        AlertDialog.Builder(this).setTitle("Qual botão?").setItems(nomes) { _, i -> imagemDaRede(url, Imagens.SLOTS[i]) }.show()
    }

    // ---------------------------------------------------------------- Vindex (outro app da Vetorial Tech)

    private fun vindexInstalado(): Boolean = try { packageManager.getLaunchIntentForPackage(VINDEX) != null } catch (e: Exception) { false }

    // ---- página de downloads (dentro do Velox)

    private var paginaDownloads: WebView? = null
    private val velocidadeAnterior = HashMap<Long, Triple<Long, Long, Float>>() // id -> (bytes, instante, velocidade suavizada)

    private fun abrirDownloads() {
        fecharPainel()
        val wv = WebView(this)
        wv.settings.javaScriptEnabled = true
        wv.settings.allowFileAccess = false
        wv.settings.allowContentAccess = false
        wv.setBackgroundColor(tema.fundo)
        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean = acaoDoPainel(req.url)
        }
        raizApp.addView(wv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        painel = wv
        paginaDownloads = wv
        flutuante.visibility = View.GONE
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(campo.windowToken, 0)
        wv.loadUrl("file:///android_asset/downloads.html?" + tema.params())
        val tick = object : Runnable {
            override fun run() {
                if (paginaDownloads !== wv || painel !== wv) return
                enviarDownloads()
                wv.postDelayed(this, 800)
            }
        }
        wv.postDelayed(tick, 800)
    }

    /** Lê os downloads do aparelho feitos pelo Velox e manda para a página (progresso, tamanho e velocidade). */
    private fun enviarDownloads() {
        val wv = paginaDownloads ?: return
        if (painel !== wv) return
        val itens = JSONArray()
        val agora = System.currentTimeMillis()
        try {
            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            dm.query(DownloadManager.Query())?.use { c ->
                val iId = c.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
                val iT = c.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)
                val iS = c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                val iTot = c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val iB = c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val iR = c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
                var n = 0
                while (c.moveToNext() && n < 60) {
                    n++
                    val id = c.getLong(iId)
                    val st = c.getInt(iS)
                    val bytes = c.getLong(iB).coerceAtLeast(0)
                    var vel = 0f
                    if (st == DownloadManager.STATUS_RUNNING) {
                        val ant = velocidadeAnterior[id]
                        if (ant != null && agora > ant.second) {
                            val inst = (bytes - ant.first) * 1000f / (agora - ant.second)
                            vel = if (ant.third > 0f) ant.third * 0.6f + inst.coerceAtLeast(0f) * 0.4f else inst.coerceAtLeast(0f)
                        }
                        velocidadeAnterior[id] = Triple(bytes, agora, vel)
                    } else velocidadeAnterior.remove(id)
                    val estado = when (st) {
                        DownloadManager.STATUS_RUNNING -> 0
                        DownloadManager.STATUS_SUCCESSFUL -> 2
                        DownloadManager.STATUS_FAILED -> 3
                        else -> 1
                    }
                    val motivo = c.getInt(iR)
                    val q = when {
                        estado == 1 && motivo == DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "sem rede"
                        estado == 1 && motivo == DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "esperando Wi-Fi"
                        estado == 1 && motivo == DownloadManager.PAUSED_WAITING_TO_RETRY -> "tentando de novo"
                        estado == 3 && motivo == DownloadManager.ERROR_INSUFFICIENT_SPACE -> "sem espaço no aparelho"
                        estado == 3 && motivo == DownloadManager.ERROR_CANNOT_RESUME -> "não deu para continuar"
                        estado == 3 && motivo >= 400 -> "erro do site ($motivo)"
                        else -> ""
                    }
                    itens.put(JSONObject().put("id", id).put("t", c.getString(iT) ?: "").put("s", estado).put("b", bytes)
                        .put("tot", c.getLong(iTot).coerceAtLeast(0)).put("v", vel.toLong()).put("q", q))
                }
            }
        } catch (e: Exception) {
            // sem acesso: mostra lista vazia
        }
        val b64 = Base64.encodeToString(JSONObject().put("itens", itens).toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        wv.evaluateJavascript("window.vxAtualizar && window.vxAtualizar('$b64')", null)
    }

    private fun abrirArquivoBaixado(id: Long) {
        try {
            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            val uri = dm.getUriForDownloadedFile(id)
            val tipo = dm.getMimeTypeForDownloadedFile(id)
            if (uri == null) { Toast.makeText(this, "O arquivo não está mais lá.", Toast.LENGTH_LONG).show(); return }
            startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, tipo ?: "*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Nenhum app abre esse tipo de arquivo. Use \"Abrir pasta\".", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Não consegui abrir o arquivo.", Toast.LENGTH_LONG).show()
        }
    }

    /** Abre os downloads no gerenciador que a pessoa usa (Vindex, Arquivos do Google ou outro); sem um padrão, o Android pergunta. */
    private fun abrirVindex() {
        fecharPainel()
        try {
            startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            try {
                val pasta = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload")
                startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(pasta, "vnd.android.document/directory").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e2: Exception) {
                Toast.makeText(this, "Não encontrei um app para abrir os downloads.", Toast.LENGTH_LONG).show()
            }
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
            val id = (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(pedido)
            baixando.add(id)
            acompanharDownloads()
            Toast.makeText(this, "Baixando $nome na pasta Download", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Não consegui baixar: ${e.message ?: "erro"}", Toast.LENGTH_LONG).show()
        }
    }

    private var acompanhando = false

    /** Confere de tempos em tempos os downloads em andamento e deixa o botão do topo girando enquanto houver algum. */
    private fun acompanharDownloads() {
        if (acompanhando) return
        acompanhando = true
        val tarefa = object : Runnable {
            override fun run() {
                val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                var total = 0L; var feito = 0L; var concluidos = 0
                val acabou = mutableListOf<Long>()
                try {
                    for (id in baixando.toList()) {
                        dm.query(DownloadManager.Query().setFilterById(id))?.use { c ->
                            if (!c.moveToFirst()) { acabou.add(id); return@use }
                            val st = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                            if (st == DownloadManager.STATUS_SUCCESSFUL || st == DownloadManager.STATUS_FAILED) {
                                acabou.add(id)
                                if (st == DownloadManager.STATUS_SUCCESSFUL) concluidos++
                            } else {
                                total += c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)).coerceAtLeast(0)
                                feito += c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)).coerceAtLeast(0)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // sem acesso ao gerenciador: para de acompanhar
                    baixando.clear()
                }
                baixando.removeAll(acabou.toSet())
                if (concluidos > 0) Toast.makeText(this@MainActivity, "Download concluído. Toque na seta do topo para ver.", Toast.LENGTH_LONG).show()
                bDownload.ativo = baixando.isNotEmpty()
                bDownload.progresso = if (baixando.isNotEmpty() && total > 0) (feito * 100 / total).toInt() else -1
                if (baixando.isEmpty()) acompanhando = false else bDownload.postDelayed(this, 600)
            }
        }
        bDownload.ativo = true
        bDownload.post(tarefa)
    }

    // ---------------------------------------------------------------- compartilhar e limpar

    private fun compartilhar() {
        val u = atual?.url ?: ""
        if (u.isEmpty()) return
        val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, u)
        startActivity(Intent.createChooser(i, "Compartilhar"))
    }

    private fun limparDados() {
        limparRastros()
        for (a in abas) { a.webView.clearHistory(); a.webView.clearCache(true); a.webView.clearFormData() }
        Toast.makeText(this, "Dados apagados.", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val INICIO = "file:///android_asset/inicio.html"
        const val PEDIDO_ARQUIVO = 1
        const val PEDIDO_FUNDO = 2
        const val PEDIDO_PADRAO = 9
        const val PEDIDO_ICONE = 3
        const val VINDEX = "tech.vetorial.vindex"

        /** Deixa transparentes os fundos grandes e lisos da página para aparecer o fundo do Velox por trás. */
        /** Resultados do Google: tira o fundo sólido de tudo e deixa só um vidro escuro e leve atrás de cada resultado. */
        const val JS_GOOGLE = "(function(){try{var s=document.getElementById('velox-google');if(!s){s=document.createElement('style');s.id='velox-google';" +
            "s.textContent='html,body,#main,#cnt,#center_col,#rcnt,#sfcnt,#searchform,.sfbg,.sfbgg,#appbar,#hdtb,#hdtb-msb,#top_nav,#rso,#botstuff,#foot,#fbar,header,[role=main],[role=banner]{background:transparent!important;background-image:none!important}" +
            "#rso>div,#rso .MjjYud,#rso .g,#botstuff>div{background:rgba(6,8,18,.42)!important;border-radius:14px!important;-webkit-backdrop-filter:blur(8px);backdrop-filter:blur(8px)}';" +
            "(document.head||document.documentElement).appendChild(s);}" +
            "var W=innerWidth;function l(e,d){if(!e||e.nodeType!==1||d>9)return;" +
            "if(/^(IMG|VIDEO|CANVAS|SVG|IFRAME|INPUT|BUTTON|SELECT|TEXTAREA|SCRIPT|STYLE|A|SPAN)\$/i.test(e.tagName))return;" +
            "var c=getComputedStyle(e),r=e.getBoundingClientRect();" +
            "if(r.width>=W*0.5&&c.backgroundImage==='none'&&c.position!=='fixed'){var m=c.backgroundColor.match(/[\\d.]+/g);" +
            "if(m&&(m.length<4||+m[3]>0.6))e.style.setProperty('background-color','rgba(6,8,18,0.28)','important');}" +
            "var k=e.children;for(var i=0;i<k.length&&i<40;i++)l(k[i],d+1);}" +
            "l(document.body,0);}catch(x){}})();"

        const val JS_TRANSPARENTE = "(function(){try{var s=document.getElementById('velox-tema');if(!s){s=document.createElement('style');s.id='velox-tema';" +
            "s.textContent='html,body{background:transparent!important;background-image:none!important}';(document.head||document.documentElement).appendChild(s);}" +
            "var W=innerWidth,H=innerHeight;function l(e,d){if(!e||e.nodeType!==1||d>4)return;" +
            "if(/^(IMG|VIDEO|CANVAS|SVG|IFRAME|INPUT|BUTTON|SELECT|TEXTAREA|SCRIPT|STYLE)\$/i.test(e.tagName))return;" +
            "var c=getComputedStyle(e),r=e.getBoundingClientRect();" +
            "if(r.width>=W*0.85&&r.height>=H*0.3&&c.backgroundImage==='none'){var m=c.backgroundColor.match(/[\\d.]+/g);" +
            "if(m&&(m.length<4||+m[3]>0.5))e.style.setProperty('background-color','transparent','important');}" +
            "var k=e.children;for(var i=0;i<k.length&&i<12;i++)l(k[i],d+1);}" +
            "l(document.body,0);}catch(x){}})();"
    }
}

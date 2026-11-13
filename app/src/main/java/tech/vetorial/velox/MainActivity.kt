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
        tema = Tema.daString(ajustes.temaTexto)
        bloqueadosTotal.set(ajustes.bloqueadosTotal)
        montarTela()
        aplicarOpcoesSalvas()
        procurarAtualizacao(false)
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
        barraDeCima.addView(logo, LinearLayout.LayoutParams(dp(66), dp(34)).apply { rightMargin = dp(8) })
        barraDeCima.addView(campo, LinearLayout.LayoutParams(0, dp(44), 1f))

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
        val bPainel = botao("painel", "Painel do Velox: modo jogo, visual e mais") { abrirPainel("jogo") }
        val altura = dp(54)
        for (b in listOf(bVoltar, bAvancar, bInicio, bRecarregar)) barraDeBaixo.addView(b, LinearLayout.LayoutParams(0, altura, 1f))
        val caixaAbas = FrameLayout(this)
        caixaAbas.addView(contador, FrameLayout.LayoutParams(dp(32), dp(28), Gravity.CENTER))
        barraDeBaixo.addView(caixaAbas, LinearLayout.LayoutParams(0, altura, 1f))
        barraDeBaixo.addView(bPainel, LinearLayout.LayoutParams(0, altura, 1f))

        coluna.addView(barraDeCima, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        coluna.addView(barra, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)))
        coluna.addView(conteudo, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
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
        campo.background = PainelCortado(tema.campo, (tema.destaque and 0x00FFFFFF) or (alfa shl 24), dp(if (campo.hasFocus()) 2 else 1).toFloat(),
            if (tema.redondo) dp(22).toFloat() else dp(10).toFloat(), tema.redondo)
    }

    /** Aplica as cores do tema em tudo que já está na tela (vale na hora, sem reiniciar). */
    private fun aplicarCores() {
        val comFoto = fotoFundo != null
        coluna.setBackgroundColor(Color.TRANSPARENT)
        conteudo.setBackgroundColor(Color.TRANSPARENT)
        val alfa = if (comFoto) 0xE6000000.toInt() else 0xFF000000.toInt()
        val a = (tema.superficie and 0x00FFFFFF) or alfa
        val b = (LogoVgx.misturar(tema.superficie, tema.fundo, 0.55f) and 0x00FFFFFF) or alfa
        barraDeCima.background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(a, b))
        barraDeBaixo.background = PainelCortado((tema.superficie and 0x00FFFFFF) or 0xF2000000.toInt(), (tema.destaque and 0x00FFFFFF) or 0xA0000000.toInt(),
            dp(1).toFloat() + 0.5f, if (tema.redondo) dp(28).toFloat() else dp(14).toFloat(), tema.redondo)
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
        fotoFundo?.let { o.put("fotoAmostra", Imagens.miniatura(it, 360, true)) }
        val ic = JSONObject()
        for (slot in Imagens.SLOTS) iconesProprios[slot]?.let { ic.put(slot, Imagens.miniatura(it, 72, false)) }
        o.put("icones", ic)
        return Base64.encodeToString(o.toString().toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun abrirPainel(aba: String) {
        fecharPainel()
        val wv = WebView(this)
        wv.setBackgroundColor(Color.TRANSPARENT)
        wv.settings.javaScriptEnabled = true
        wv.settings.allowFileAccess = false
        wv.settings.allowContentAccess = false
        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean = acaoDoPainel(req.url)
        }
        raizApp.addView(wv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        painel = wv
        flutuante.visibility = View.GONE
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(campo.windowToken, 0)
        wv.loadUrl("file:///android_asset/painel.html?aba=" + aba + "&" + tema.params() + "&s=" + estadoDoPainel())
    }

    private fun fecharPainel() {
        val wv = painel ?: return
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
            "vindex" -> abrirVindex()
            "fundo-galeria" -> escolherImagem(PEDIDO_FUNDO)
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
            if (aba === atual) { campo.setText(Enderecos.paraMostrar(aba.url)); atualizarFundo() }
        }

        override fun onPageFinished(view: WebView, url: String) {
            if (!url.startsWith("file:///android_asset/")) {
                aba.url = url
                if (ajustes.sitesNoTema) {
                    view.evaluateJavascript(JS_TRANSPARENTE, null)
                    view.postDelayed({ view.evaluateJavascript(JS_TRANSPARENTE, null) }, 1200)
                    view.postDelayed({ view.evaluateJavascript(JS_TRANSPARENTE, null) }, 3500)
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

    private fun abrirVindex() {
        val i = try { packageManager.getLaunchIntentForPackage(VINDEX) } catch (e: Exception) { null }
        if (i == null) { Toast.makeText(this, "O Vindex não está instalado neste aparelho.", Toast.LENGTH_LONG).show(); return }
        fecharPainel()
        startActivity(i)
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
        const val PEDIDO_ICONE = 3
        const val VINDEX = "tech.vetorial.vindex"

        /** Deixa transparentes os fundos grandes e lisos da página para aparecer o fundo do Velox por trás. */
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

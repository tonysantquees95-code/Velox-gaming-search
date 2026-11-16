package tech.vetorial.velox

import android.webkit.WebView
import java.util.concurrent.atomic.AtomicInteger

/** Uma aba: o WebView, o que ela mostra e quantos anúncios já barrou nesta página. */
class Aba(val webView: WebView, val anonima: Boolean) {
    var titulo: String = "Nova aba"
    var url: String = ""
    val bloqueados = AtomicInteger(0)
    /** A pessoa tocou em algo que rola por dentro da página (menu, lista, painel de um app web) e já está longe do topo: puxar para baixo é rolar, não recarregar. */
    @Volatile var rolagemInterna: Boolean = false
}

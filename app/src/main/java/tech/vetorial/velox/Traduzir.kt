package tech.vetorial.velox

import java.net.URLEncoder

/** Traduzir página: abre a mesma página pelo Google Tradutor. O endereço da página só vai ao Google quando a pessoa pede (os termos explicam). */
object Traduzir {
    private fun codifica(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    private val AUTORIDADE = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://([^/?#]*)")

    /** Nome do site (sem porta). Não usa java.net.URI para aguentar endereços com caracteres soltos. */
    private fun host(url: String): String {
        val a = AUTORIDADE.find(url)?.groupValues?.get(1) ?: return ""
        return a.substringAfterLast('@').substringBefore(':').lowercase()
    }

    /** A página que aparece já é uma tradução do Google? */
    fun ehTraduzida(url: String): Boolean {
        val h = host(url)
        return h.endsWith(".translate.goog") || h == "translate.google.com" || h == "translate.googleusercontent.com"
    }

    /** Endereço da tradução para o português, ou null se a página não é http/https (ou já é uma tradução). */
    fun urlTraduzida(url: String): String? {
        val ok = (url.startsWith("http://") || url.startsWith("https://")) && host(url).isNotEmpty()
        if (!ok || ehTraduzida(url)) return null
        return "https://translate.google.com/translate?sl=auto&tl=pt-BR&hl=pt-BR&u=" + codifica(url)
    }

    /** O endereço original, quando o da tradução ainda leva o parâmetro u (senão null: use o que o app guardou). */
    fun urlOriginal(url: String): String? {
        if (host(url) != "translate.google.com") return null
        val q = url.substringAfter('?', "")
        for (par in q.split('&')) {
            if (par.startsWith("u=")) {
                val v = try { java.net.URLDecoder.decode(par.substring(2), "UTF-8") } catch (e: Exception) { return null }
                return v.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            }
        }
        return null
    }
}

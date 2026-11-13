package tech.vetorial.velox

import java.net.URLEncoder

/** Buscadores que o Velox oferece. */
enum class Buscador(val id: String, val nome: String, private val modelo: String) {
    GOOGLE("google", "Google", "https://www.google.com/search?q=%s"),
    DUCK("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q=%s"),
    BING("bing", "Bing", "https://www.bing.com/search?q=%s");

    fun url(consulta: String): String = modelo.replace("%s", URLEncoder.encode(consulta, "UTF-8"))

    companion object {
        fun porId(id: String?): Buscador = values().firstOrNull { it.id == id } ?: GOOGLE
    }
}

/** Transforma o que a pessoa digitou na barra em um endereço ou em uma busca. */
object Enderecos {
    private val dominio = Regex("^([\\p{L}\\p{N}-]+\\.)+\\p{L}{2,}(:\\d+)?([/?#].*)?$")
    private val local = Regex("^(localhost|\\d{1,3}(\\.\\d{1,3}){3})(:\\d+)?([/?#].*)?$")

    fun resolver(entrada: String, buscador: Buscador): String {
        val t = entrada.trim()
        if (t.isEmpty()) return ""
        val minusculo = t.lowercase()
        if (minusculo.startsWith("http://") || minusculo.startsWith("https://")) return t
        if (t.contains(' ')) return buscador.url(t)
        if (local.matches(t)) return "http://$t"
        if (dominio.matches(t)) return "https://$t"
        return buscador.url(t)
    }

    /** "https://www.exemplo.com:8080/a?b" → "www.exemplo.com" */
    fun hospedeDe(url: String?): String? {
        if (url.isNullOrEmpty()) return null
        var s = url
        val esquema = s.indexOf("://")
        if (esquema >= 0) s = s.substring(esquema + 3)
        val fim = s.indexOfAny(charArrayOf('/', '?', '#'))
        if (fim >= 0) s = s.substring(0, fim)
        val arroba = s.lastIndexOf('@')
        if (arroba >= 0) s = s.substring(arroba + 1)
        val porta = s.lastIndexOf(':')
        if (porta >= 0) s = s.substring(0, porta)
        return s.lowercase().ifEmpty { null }
    }

    /** O que mostrar na barra: só o domínio quando é https (como os outros navegadores). */
    fun paraMostrar(url: String?): String {
        if (url.isNullOrEmpty() || url.startsWith("file:///android_asset/")) return ""
        return url
    }
}

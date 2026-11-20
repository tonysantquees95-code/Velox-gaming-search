package tech.vetorial.velox

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Icon

/** Atalho de uma página na tela inicial: nome, identificador e ícone (a logo do site sobre o fundo do tema). */
object Atalhos {
    fun enderecoValido(url: String?): Boolean = url != null && Regex("^https?://\\S+$", RegexOption.IGNORE_CASE).matches(url)

    /** Nome sugerido: o título da página ou, sem ele, o site. */
    fun nomeSugerido(titulo: String?, url: String): String {
        val t = titulo?.trim().orEmpty()
        if (t.isNotEmpty() && !t.equals("Velox", true) && !t.equals("Nova aba", true)) return t.take(40)
        return try { java.net.URI(url).host?.removePrefix("www.") ?: "Página" } catch (e: Exception) { "Página" }
    }

    /** O que a pessoa digitou, sem espaços sobrando e curto o bastante para caber sob o ícone. */
    fun nomeCurto(digitado: String, padrao: String): String {
        val n = digitado.replace(Regex("\\s+"), " ").trim().take(30)
        return n.ifEmpty { padrao.take(30) }
    }

    /** Mesmo site (e mesmo endereço) = mesmo atalho: criar de novo atualiza em vez de duplicar. */
    fun idDe(url: String): String = "site-" + Integer.toHexString(url.hashCode())

    /** Ícone adaptável (108 dp): fundo na cor do tema e, no centro, a logo do site ou a inicial do nome. */
    fun iconeAdaptavel(logo: Bitmap?, nome: String, cor: Int, densidade: Float): Icon {
        val lado = (108 * densidade).toInt().coerceIn(216, 432)
        val b = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val fundo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = escurecer(cor) }
        c.drawRect(0f, 0f, lado.toFloat(), lado.toFloat(), fundo)
        // a zona segura do ícone adaptável é o círculo central de 66 dp (61%)
        val miolo = lado * 0.52f
        val esq = (lado - miolo) / 2f
        if (logo != null && logo.width > 0) {
            val placa = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
            c.drawRoundRect(esq - lado * 0.04f, esq - lado * 0.04f, esq + miolo + lado * 0.04f, esq + miolo + lado * 0.04f, lado * 0.08f, lado * 0.08f, placa)
            val destino = android.graphics.RectF(esq, esq, esq + miolo, esq + miolo)
            c.drawBitmap(logo, null, destino, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        } else {
            val letra = nome.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "V"
            val t = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cor or 0xFF000000.toInt(); textSize = miolo * 0.95f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
            c.drawText(letra, lado / 2f, lado / 2f - (t.descent() + t.ascent()) / 2f, t)
        }
        return Icon.createWithAdaptiveBitmap(b)
    }

    private fun escurecer(c: Int): Int {
        val r = ((c shr 16) and 0xFF) / 5; val g = ((c shr 8) and 0xFF) / 5; val b = (c and 0xFF) / 5
        return (0xFF shl 24) or ((r + 10) shl 16) or ((g + 12) shl 8) or (b + 24)
    }
}

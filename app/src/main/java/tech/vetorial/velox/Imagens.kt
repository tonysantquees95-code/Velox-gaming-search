package tech.vetorial.velox

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/** Foto de fundo e ícones próprios da barra: ficam em arquivos privados do app, já reduzidos. */
object Imagens {
    const val LIMITE_BYTES = 15 * 1024 * 1024
    val SLOTS = listOf("voltar", "avancar", "inicio", "recarregar", "painel")

    fun slotValido(s: String?): Boolean = s != null && s in SLOTS

    /** Só endereços https (a foto vem da internet, nunca de arquivo ou http aberto). */
    fun enderecoPermitido(u: String?): Boolean {
        if (u.isNullOrBlank() || u.length > 2000) return false
        return try {
            val uri = java.net.URI(u)
            uri.scheme == "https" && !uri.host.isNullOrEmpty()
        } catch (e: Exception) { false }
    }

    /** Maior potência de 2 que ainda deixa o lado maior com pelo menos `alvo` pixels. */
    fun reducao(largura: Int, altura: Int, alvo: Int): Int {
        var r = 1
        val maior = maxOf(largura, altura)
        while (maior / (r * 2) >= alvo) r *= 2
        return r
    }

    fun decodificar(dados: ByteArray, ladoMaximo: Int): Bitmap? {
        val medida = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(dados, 0, dados.size, medida)
        if (medida.outWidth <= 0 || medida.outHeight <= 0) return null
        val o = BitmapFactory.Options().apply { inSampleSize = reducao(medida.outWidth, medida.outHeight, ladoMaximo) }
        val b = BitmapFactory.decodeByteArray(dados, 0, dados.size, o) ?: return null
        val maior = maxOf(b.width, b.height)
        if (maior <= ladoMaximo) return b
        val f = ladoMaximo.toFloat() / maior
        return Bitmap.createScaledBitmap(b, (b.width * f).toInt().coerceAtLeast(1), (b.height * f).toInt().coerceAtLeast(1), true)
    }

    fun lerFluxo(f: InputStream): ByteArray {
        val saida = ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        while (true) {
            val n = f.read(buf)
            if (n < 0) break
            saida.write(buf, 0, n)
            if (saida.size() > LIMITE_BYTES) throw IllegalStateException("imagem grande demais")
        }
        return saida.toByteArray()
    }

    fun baixar(url: String): ByteArray {
        if (!enderecoPermitido(url)) throw IllegalArgumentException("endereço não permitido")
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 12000
        c.readTimeout = 20000
        c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) Velox")
        try {
            if (c.responseCode != 200) throw IllegalStateException("resposta ${c.responseCode}")
            if (c.url.protocol != "https") throw IllegalStateException("redirecionou para http")
            return c.inputStream.use { lerFluxo(it) }
        } finally {
            c.disconnect()
        }
    }

    private fun arquivoFundo(ctx: Context) = File(ctx.filesDir, "fundo.jpg")
    private fun arquivoIcone(ctx: Context, slot: String) = File(ctx.filesDir, "icone_$slot.png")

    fun temFundo(ctx: Context): Boolean = arquivoFundo(ctx).exists()

    /** Biblioteca de wallpapers: todo fundo usado fica guardado aqui (sobrevive a atualizações) e pode ser escolhido de novo. */
    fun pastaWallpapers(ctx: Context) = File(ctx.filesDir, "wallpapers").also { it.mkdirs() }

    fun listarWallpapers(ctx: Context): List<String> =
        (pastaWallpapers(ctx).listFiles { f -> f.name.matches(Regex("w_[0-9a-f]{12}\\.jpg")) } ?: emptyArray())
            .sortedByDescending { it.lastModified() }.map { it.name }

    fun arquivoWallpaper(ctx: Context, nome: String): File? =
        if (nome.matches(Regex("w_[0-9a-f]{12}\\.jpg"))) File(pastaWallpapers(ctx), nome).takeIf { it.exists() } else null

    /** Guarda o fundo na biblioteca e devolve o nome do arquivo. Também deixa uma cópia na galeria (Imagens/Velox), que sobrevive até a desinstalar o app. */
    private fun guardarNaBiblioteca(ctx: Context, jpeg: ByteArray): String {
        val hash = java.security.MessageDigest.getInstance("SHA-1").digest(jpeg).joinToString("") { "%02x".format(it) }.take(12)
        val nome = "w_$hash.jpg"
        val f = File(pastaWallpapers(ctx), nome)
        if (!f.exists()) {
            f.writeBytes(jpeg)
            try {
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    val v = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "Velox-$hash.jpg")
                        put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Velox")
                    }
                    ctx.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v)?.let { u -> ctx.contentResolver.openOutputStream(u)?.use { it.write(jpeg) } }
                }
            } catch (e: Exception) {
                // sem a cópia na galeria: a da biblioteca continua valendo
            }
        } else f.setLastModified(System.currentTimeMillis())
        return nome
    }

    fun usarWallpaper(ctx: Context, nome: String): Boolean {
        val f = arquivoWallpaper(ctx, nome) ?: return false
        f.copyTo(arquivoFundo(ctx), overwrite = true)
        f.setLastModified(System.currentTimeMillis())
        return true
    }

    fun apagarWallpaper(ctx: Context, nome: String) { arquivoWallpaper(ctx, nome)?.delete() }

    /** Miniatura (JPEG) de um wallpaper da biblioteca, para a lista do painel. */
    fun miniaturaWallpaper(ctx: Context, nome: String): ByteArray? {
        val f = arquivoWallpaper(ctx, nome) ?: return null
        val b = decodificar(f.readBytes(), 360) ?: return null
        return ByteArrayOutputStream().also { b.compress(Bitmap.CompressFormat.JPEG, 80, it) }.toByteArray()
    }

    /** Se o arquivo do fundo atual sumiu mas há wallpapers guardados, volta o mais recente. */
    fun restaurarFundoSeSumiu(ctx: Context): Boolean {
        if (temFundo(ctx)) {
            // fundo de versões antigas ainda não estava na biblioteca: guarda agora
            if (listarWallpapers(ctx).isEmpty()) guardarNaBiblioteca(ctx, arquivoFundo(ctx).readBytes())
            return false
        }
        val nome = listarWallpapers(ctx).firstOrNull() ?: return false
        return usarWallpaper(ctx, nome)
    }

    fun salvarFundo(ctx: Context, dados: ByteArray): Boolean {
        val b = decodificar(dados, 1600) ?: return false
        val saida = ByteArrayOutputStream()
        b.compress(Bitmap.CompressFormat.JPEG, 86, saida)
        val jpeg = saida.toByteArray()
        arquivoFundo(ctx).writeBytes(jpeg)
        guardarNaBiblioteca(ctx, jpeg)
        return true
    }

    fun removerFundo(ctx: Context) { arquivoFundo(ctx).delete() }

    fun carregarFundo(ctx: Context): Bitmap? = try { BitmapFactory.decodeFile(arquivoFundo(ctx).path) } catch (e: Exception) { null }

    fun salvarIcone(ctx: Context, slot: String, dados: ByteArray): Boolean {
        if (!slotValido(slot)) return false
        val b = decodificar(dados, 192) ?: return false
        val quadrado = recortarQuadrado(b)
        arquivoIcone(ctx, slot).outputStream().use { quadrado.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return true
    }

    fun removerIcone(ctx: Context, slot: String) { if (slotValido(slot)) arquivoIcone(ctx, slot).delete() }

    fun carregarIcone(ctx: Context, slot: String): Bitmap? {
        val f = arquivoIcone(ctx, slot)
        return if (f.exists()) try { BitmapFactory.decodeFile(f.path) } catch (e: Exception) { null } else null
    }

    fun recortarQuadrado(b: Bitmap): Bitmap {
        val lado = minOf(b.width, b.height)
        val x = (b.width - lado) / 2
        val y = (b.height - lado) / 2
        val saida = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
        Canvas(saida).drawBitmap(b, android.graphics.Rect(x, y, x + lado, y + lado), android.graphics.Rect(0, 0, lado, lado), null)
        return saida
    }

    /** Cantos arredondados (fração do lado) para os ícones feitos de foto. */
    fun arredondar(b: Bitmap, fracao: Float): Bitmap {
        val saida = Bitmap.createBitmap(b.width, b.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(saida)
        val pincel = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        val r = minOf(b.width, b.height) * fracao
        c.drawRoundRect(android.graphics.RectF(0f, 0f, b.width.toFloat(), b.height.toFloat()), r, r, pincel)
        pincel.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        c.drawBitmap(b, 0f, 0f, pincel)
        return saida
    }

    /** Miniatura em texto (data:) só para mostrar no painel. */
    fun miniatura(b: Bitmap, lado: Int, jpeg: Boolean): String {
        val f = lado.toFloat() / maxOf(b.width, b.height)
        val m = if (f < 1f) Bitmap.createScaledBitmap(b, (b.width * f).toInt().coerceAtLeast(1), (b.height * f).toInt().coerceAtLeast(1), true) else b
        val s = ByteArrayOutputStream()
        m.compress(if (jpeg) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG, 70, s)
        return "data:" + (if (jpeg) "image/jpeg" else "image/png") + ";base64," + Base64.encodeToString(s.toByteArray(), Base64.NO_WRAP)
    }
}

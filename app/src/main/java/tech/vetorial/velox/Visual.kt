package tech.vetorial.velox

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.view.View

/** Marca VGX: V e G ligados e um X grande, em tubos de neon inclinados (mesmo desenho da página inicial). */
class LogoVgx(contexto: Context) : View(contexto) {
    private var c1 = 0xFF22D3EE.toInt()
    private var c2 = 0xFFA78BFA.toInt()
    private val tubo = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val brilho = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; color = 0x57FFFFFF; strokeWidth = 4f }
    private val contorno = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; color = 0xE603050C.toInt() }
    private val g = Path().apply {
        moveTo(262.8f, 64.4f)
        cubicTo(251.2f, 47.9f, 232.2f, 38f, 212f, 38f)
        cubicTo(177.8f, 38f, 150f, 65.8f, 150f, 100f)
        cubicTo(150f, 134.2f, 177.8f, 162f, 212f, 162f)
        cubicTo(246.2f, 162f, 274f, 134.2f, 274f, 100f)
        lineTo(226f, 100f)
    }
    private val v = Path().apply { moveTo(20f, 38f); lineTo(84f, 160f); lineTo(156f, 36f) }
    private val x1 = Path().apply { moveTo(312f, 6f); lineTo(412f, 196f) }
    private val x2 = Path().apply { moveTo(412f, 6f); lineTo(312f, 196f) }

    fun cores(destaque: Int, secundaria: Int) {
        c1 = destaque
        c2 = secundaria
        tubo.shader = LinearGradient(0f, 0f, 440f, 200f, c1, c2, Shader.TileMode.CLAMP)
        invalidate()
    }

    private fun traco(canvas: Canvas, caminho: Path, largura: Float, halo: Float) {
        if (halo > 0f) { contorno.strokeWidth = largura + halo; canvas.drawPath(caminho, contorno) }
        tubo.strokeWidth = largura
        canvas.drawPath(caminho, tubo)
        canvas.drawPath(caminho, brilho)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (tubo.shader == null) tubo.shader = LinearGradient(0f, 0f, 440f, 200f, c1, c2, Shader.TileMode.CLAMP)
        val s = minOf(width / 480f, height / 246f)
        canvas.save()
        canvas.translate((width - 480f * s) / 2f, (height - 246f * s) / 2f)
        canvas.scale(s, s)
        canvas.translate(8f, 22f)
        canvas.translate(14f, 0f)
        canvas.skew(-0.1944f, 0f)
        traco(canvas, g, 28f, 0f)
        traco(canvas, v, 28f, 7f)
        traco(canvas, x1, 34f, 0f)
        traco(canvas, x2, 34f, 7f)
        canvas.restore()
    }

    companion object {
        fun misturar(a: Int, b: Int, parteDeB: Float): Int {
            fun m(x: Int, y: Int) = (x + (y - x) * parteDeB).toInt().coerceIn(0, 255)
            return (0xFF shl 24) or (m((a shr 16) and 255, (b shr 16) and 255) shl 16) or (m((a shr 8) and 255, (b shr 8) and 255) shl 8) or m(a and 255, b and 255)
        }
    }
}

/** Linha neon no pé da barra de cima: fica acesa de leve e enche com o progresso da página. */
class BarraNeon(contexto: Context) : View(contexto) {
    private var c1 = 0xFF22D3EE.toInt()
    private var c2 = 0xFFA78BFA.toInt()
    private val pincel = Paint(Paint.ANTI_ALIAS_FLAG)
    var progresso: Int = 0
        set(v) { field = v.coerceIn(0, 100); invalidate() }

    fun cores(destaque: Int, secundaria: Int) {
        c1 = destaque
        c2 = secundaria
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        pincel.shader = LinearGradient(0f, 0f, w, 0f, c2, c1, Shader.TileMode.CLAMP)
        pincel.alpha = 70
        canvas.drawRect(0f, 0f, w, h, pincel)
        if (progresso in 1..99) {
            pincel.alpha = 255
            canvas.drawRect(0f, 0f, w * progresso / 100f, h, pincel)
        } else if (progresso == 0) {
            pincel.alpha = 120
            canvas.drawRect(0f, 0f, w, h, pincel)
        }
    }
}

/** Foto de fundo atrás de tudo: preenche a tela (corta o excesso) e escurece para o texto continuar legível. */
class FundoView(contexto: Context) : View(contexto) {
    private var foto: Bitmap? = null
    private var base = 0xFF070B14.toInt()
    private var escuro = 0.35f
    private val pincel = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val veu = Paint()
    private val matriz = Matrix()

    fun definir(b: Bitmap?, corBase: Int, escurecer: Float) {
        foto = b
        base = corBase
        escuro = escurecer.coerceIn(0f, 0.95f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(base)
        val b = foto ?: return
        if (width == 0 || height == 0) return
        val f = maxOf(width / b.width.toFloat(), height / b.height.toFloat())
        matriz.reset()
        matriz.postScale(f, f)
        matriz.postTranslate((width - b.width * f) / 2f, (height - b.height * f) / 2f)
        canvas.drawBitmap(b, matriz, pincel)
        veu.color = ((escuro * 255).toInt() shl 24) or (base and 0x00FFFFFF)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), veu)
    }
}

/** Caixa com cantos cortados (futurista) ou arredondados, com fundo e contorno; serve para a barra de baixo e o campo de busca. */
class PainelCortado(private val preenchimento: Int, private val contorno: Int, private val espessura: Float, private val corte: Float, private val redondo: Boolean) : Drawable() {
    private val cheio = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = preenchimento }
    private val linha = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = contorno; strokeWidth = espessura; strokeJoin = Paint.Join.MITER }
    private val caminho = Path()

    override fun onBoundsChange(b: android.graphics.Rect) {
        caminho.reset()
        val m = espessura / 2f
        val r = RectF(b.left + m, b.top + m, b.right - m, b.bottom - m)
        if (redondo) {
            caminho.addRoundRect(r, corte, corte, Path.Direction.CW)
        } else {
            val c = minOf(corte, r.height() / 2f)
            caminho.moveTo(r.left + c, r.top)
            caminho.lineTo(r.right, r.top)
            caminho.lineTo(r.right, r.bottom - c)
            caminho.lineTo(r.right - c, r.bottom)
            caminho.lineTo(r.left, r.bottom)
            caminho.lineTo(r.left, r.top + c)
            caminho.close()
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawPath(caminho, cheio)
        canvas.drawPath(caminho, linha)
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(cf: ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/** Ícone da barra desenhado em traço (24x24), em três pacotes: linha, grossa e duo (gradiente do tema). */
class IconeTraco(private val tracos: List<Path>, private val pontos: List<FloatArray>) : Drawable() {
    var pacote: String = "linha"
        set(v) { field = v; invalidateSelf() }
    var cor: Int = 0xFFFFFFFF.toInt()
        set(v) { field = v; invalidateSelf() }
    var c1: Int = 0xFF22D3EE.toInt()
    var c2: Int = 0xFFA78BFA.toInt()
    private val tinta = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        val lado = minOf(b.width(), b.height()).toFloat()
        canvas.save()
        canvas.translate(b.left + (b.width() - lado) / 2f, b.top + (b.height() - lado) / 2f)
        canvas.scale(lado / 24f, lado / 24f)
        tinta.shader = if (pacote == "duo") LinearGradient(2f, 2f, 22f, 22f, c1, c2, Shader.TileMode.CLAMP) else null
        tinta.color = cor
        tinta.strokeWidth = when (pacote) { "grossa" -> 2.9f; "duo" -> 2.3f; else -> 1.9f }
        for (t in tracos) canvas.drawPath(t, tinta)
        for (p in pontos) canvas.drawPoint(p[0], p[1], tinta)
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) { tinta.alpha = alpha }
    override fun setColorFilter(cf: ColorFilter?) { tinta.colorFilter = cf }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        private fun p(bloco: Path.() -> Unit): Path = Path().apply(bloco)

        fun criar(slot: String): IconeTraco = when (slot) {
            "voltar" -> IconeTraco(listOf(p { moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f) }), emptyList())
            "avancar" -> IconeTraco(listOf(p { moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f) }), emptyList())
            "inicio" -> IconeTraco(listOf(
                p { moveTo(4f, 11f); lineTo(12f, 4f); lineTo(20f, 11f) },
                p { moveTo(6f, 10f); lineTo(6f, 20f); lineTo(18f, 20f); lineTo(18f, 10f) }), emptyList())
            "recarregar" -> IconeTraco(listOf(
                p { arcTo(RectF(5f, 5f, 19f, 19f), 0f, 315f) },
                p { moveTo(19f, 4f); lineTo(19f, 8f); lineTo(15f, 8f) }), emptyList())
            else -> IconeTraco(listOf(
                p {
                    moveTo(7f, 8f); lineTo(17f, 8f); quadTo(21f, 8f, 21f, 12f); lineTo(21f, 14f)
                    cubicTo(21f, 17f, 17f, 18f, 15f, 15f); lineTo(14f, 14f); lineTo(10f, 14f); lineTo(9f, 15f)
                    cubicTo(7f, 18f, 3f, 17f, 3f, 14f); lineTo(3f, 12f); quadTo(3f, 8f, 7f, 8f); close()
                },
                p { moveTo(8f, 10.5f); lineTo(8f, 13.5f) },
                p { moveTo(6.5f, 12f); lineTo(9.5f, 12f) }),
                listOf(floatArrayOf(16f, 11.5f), floatArrayOf(18f, 13f)))
        }
    }
}

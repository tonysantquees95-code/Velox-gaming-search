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
import android.animation.ValueAnimator
import android.graphics.DashPathEffect
import android.graphics.PathMeasure
import android.view.animation.LinearInterpolator

/** Marca VX: V e X em letras chanfradas (corpo escuro, borda interna acesa e uma luz na segunda cor do tema), igual à página inicial. */
class LogoVgx(contexto: Context) : View(contexto) {
    private var c1 = 0xFF22D3EE.toInt()
    private var c2 = 0xFFA78BFA.toInt()
    private val v = Path().apply { moveTo(0f, 0f); lineTo(64f, 0f); lineTo(110f, 124f); lineTo(156f, 0f); lineTo(220f, 0f); lineTo(142f, 190f); lineTo(78f, 190f); close() }
    private val x1 = Path().apply { moveTo(190f, 0f); lineTo(254f, 0f); lineTo(400f, 190f); lineTo(336f, 190f); close() }
    private val x2 = Path().apply { moveTo(336f, 0f); lineTo(400f, 0f); lineTo(254f, 190f); lineTo(190f, 190f); close() }
    private val pincel = Paint(Paint.ANTI_ALIAS_FLAG)
    private val letras = Path().apply { addPath(x1); addPath(x2); addPath(v) }

    fun cores(destaque: Int, secundaria: Int) {
        c1 = destaque
        c2 = secundaria
        invalidate()
    }

    /** Neons andando em volta das letras (como na página inicial). */
    private var fase = 0f
    private var anim: ValueAnimator? = null
    private val comprimentos = listOf(x1, x2, v).map { PathMeasure(it, true).length }.toFloatArray()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3400; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
            addUpdateListener { fase = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        anim?.cancel(); anim = null
        super.onDetachedFromWindow()
    }

    private fun corredores(canvas: Canvas) {
        val ps = arrayOf(x1, x2, v)
        pincel.style = Paint.Style.STROKE; pincel.shader = null; pincel.strokeJoin = Paint.Join.ROUND; pincel.strokeCap = Paint.Cap.ROUND
        for (volta in 0..1) {
            for (i in ps.indices) {
                val l = comprimentos[i]
                val atraso = (if (volta == 0) i * 0.26f else 0.47f + i * 0.26f)
                val deslocamento = ((fase + atraso) % 1f) * l
                pincel.pathEffect = DashPathEffect(floatArrayOf(l * 0.13f, l * 0.87f), -deslocamento)
                if (volta == 0) { pincel.color = 0xF2FFFFFF.toInt(); pincel.strokeWidth = 5f } else { pincel.color = c2; pincel.strokeWidth = 4f }
                canvas.drawPath(ps[i], pincel)
            }
        }
        pincel.pathEffect = null; pincel.strokeCap = Paint.Cap.BUTT; pincel.strokeJoin = Paint.Join.MITER
    }

    private fun todas(canvas: Canvas, f: (Path) -> Unit) { f(x1); f(x2); f(v) }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val s = minOf(width / 428f, height / 218f)
        canvas.save()
        canvas.translate((width - 428f * s) / 2f, (height - 218f * s) / 2f)
        canvas.scale(s, s)
        canvas.translate(14f, 14f)
        // luz da segunda cor, deslocada para baixo e para a esquerda
        canvas.save(); canvas.translate(-8f, 8f)
        pincel.style = Paint.Style.FILL_AND_STROKE; pincel.color = c2; pincel.strokeWidth = 4f; pincel.strokeJoin = Paint.Join.MITER; pincel.shader = null
        todas(canvas) { canvas.drawPath(it, pincel) }
        canvas.restore()
        // moldura escura
        pincel.color = 0xFF03050C.toInt(); pincel.strokeWidth = 16f
        todas(canvas) { canvas.drawPath(it, pincel) }
        // corpo
        pincel.style = Paint.Style.FILL; pincel.shader = LinearGradient(0f, 0f, 0f, 190f, intArrayOf(0xFF2A3F63.toInt(), 0xFF101A30.toInt(), 0xFF070C18.toInt()), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        todas(canvas) { canvas.drawPath(it, pincel) }
        pincel.shader = null
        // borda interna acesa
        canvas.save(); canvas.clipPath(letras)
        pincel.style = Paint.Style.STROKE; pincel.color = c1; pincel.strokeWidth = 9f
        todas(canvas) { canvas.drawPath(it, pincel) }
        pincel.color = 0x66FFFFFF; pincel.strokeWidth = 3f
        todas(canvas) { canvas.drawPath(it, pincel) }
        canvas.restore()
        corredores(canvas)
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
        when (pacote) {
            "neon" -> { passe(canvas, c1, 5.2f, 0x2E); passe(canvas, c1, 3.6f, 0x59); passe(canvas, cor, 1.9f) }
            "sombra" -> {
                canvas.save(); canvas.translate(1.1f, 1.1f); passe(canvas, c2, 2.3f); canvas.restore()
                passe(canvas, cor, 2.3f)
            }
            "duo" -> passe(canvas, cor, 2.3f, 0xFF, LinearGradient(2f, 2f, 22f, 22f, c1, c2, Shader.TileMode.CLAMP))
            "aurora" -> passe(canvas, cor, 2.7f, 0xFF, LinearGradient(2f, 2f, 22f, 22f, c2, c1, Shader.TileMode.CLAMP))
            "cor" -> passe(canvas, c1, 2.4f)
            "reto" -> passe(canvas, cor, 2.3f, 0xFF, null, Paint.Cap.SQUARE, Paint.Join.MITER)
            "grossa" -> passe(canvas, cor, 2.9f)
            "fino" -> passe(canvas, cor, 1.3f)
            else -> passe(canvas, cor, 1.9f)
        }
        canvas.restore()
    }

    /** Uma passada de traço com a cor, a espessura e o estilo de ponta pedidos (o neon e a sombra usam várias). */
    private fun passe(canvas: Canvas, cor: Int, largura: Float, alfa: Int = 0xFF, gradiente: Shader? = null, ponta: Paint.Cap = Paint.Cap.ROUND, junta: Paint.Join = Paint.Join.ROUND) {
        tinta.shader = gradiente
        tinta.color = (cor and 0x00FFFFFF) or (alfa shl 24)
        tinta.strokeWidth = largura
        tinta.strokeCap = ponta
        tinta.strokeJoin = junta
        for (t in tracos) canvas.drawPath(t, tinta)
        for (pt in pontos) canvas.drawPoint(pt[0], pt[1], tinta)
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


/** Botão de downloads: seta que balança e anel que gira (ou enche) enquanto algo está baixando; parado, fica discreto. */
class BotaoDownload(contexto: Context) : View(contexto) {
    private val pincel = Paint(Paint.ANTI_ALIAS_FLAG)
    private var cor = 0xFF22D3EE.toInt()
    private var fase = 0f
    private var anim: ValueAnimator? = null
    var ativo = false
        set(v) { if (field != v) { field = v; if (v) iniciar() else parar(); invalidate() } }
    /** -1 = não sei (anel girando); 0..100 = anel enchendo. */
    var progresso = -1
        set(v) { field = v; invalidate() }

    fun cor(c: Int) { cor = c; invalidate() }

    private fun iniciar() {
        if (anim != null) return
        anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1100; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
            addUpdateListener { fase = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    private fun parar() { anim?.cancel(); anim = null; fase = 0f }

    override fun onDetachedFromWindow() { parar(); super.onDetachedFromWindow() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = minOf(width, height).toFloat()
        val cx = width / 2f; val cy = height / 2f
        val raio = d * 0.36f
        pincel.style = Paint.Style.STROKE; pincel.strokeCap = Paint.Cap.ROUND
        pincel.strokeWidth = d * 0.07f
        pincel.color = (cor and 0x00FFFFFF) or (if (ativo) 0x55000000 else 0x40000000)
        canvas.drawCircle(cx, cy, raio, pincel)
        val caixa = RectF(cx - raio, cy - raio, cx + raio, cy + raio)
        if (ativo) {
            pincel.color = cor
            if (progresso in 0..100) canvas.drawArc(caixa, -90f, 360f * progresso / 100f, false, pincel)
            else canvas.drawArc(caixa, fase * 360f, 100f, false, pincel)
        }
        // seta para baixo; balança enquanto baixa
        val balanco = if (ativo) Math.sin(fase * 2 * Math.PI).toFloat() * d * 0.05f else 0f
        pincel.color = if (ativo) cor else (cor and 0x00FFFFFF) or 0xB0000000.toInt()
        pincel.strokeWidth = d * 0.085f
        val topo = cy - d * 0.14f + balanco; val base = cy + d * 0.14f + balanco
        canvas.drawLine(cx, topo, cx, base, pincel)
        val p = Path().apply { moveTo(cx - d * 0.11f, base - d * 0.11f); lineTo(cx, base); lineTo(cx + d * 0.11f, base - d * 0.11f) }
        canvas.drawPath(p, pincel)
    }
}

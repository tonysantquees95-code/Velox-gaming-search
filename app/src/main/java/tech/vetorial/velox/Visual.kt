package tech.vetorial.velox

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.view.View

/** Monograma VGX (V, G e X em ângulos) com relevo, desenhado no próprio app para a barra de cima. */
class LogoVgx(contexto: Context) : View(contexto) {
    private var c1 = 0xFF22D3EE.toInt()
    private var c2 = 0xFFA78BFA.toInt()
    private val face = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val borda = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0x66FFFFFF; strokeWidth = 1.2f }
    private val relevo = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val forma = Path().apply {
        poligono(this, floatArrayOf(0f, 0f, 56f, 0f, 100f, 112f, 144f, 0f, 200f, 0f, 128f, 170f, 72f, 170f))
        poligono(this, floatArrayOf(232f, 0f, 392f, 0f, 392f, 42f, 250f, 42f, 250f, 128f, 334f, 128f, 334f, 104f, 296f, 104f, 296f, 70f, 392f, 70f, 392f, 170f, 232f, 170f, 192f, 130f, 192f, 40f))
        poligono(this, floatArrayOf(424f, 0f, 482f, 0f, 524f, 50.3f, 566f, 0f, 624f, 0f, 553f, 85f, 624f, 170f, 566f, 170f, 524f, 119.7f, 482f, 170f, 424f, 170f, 495f, 85f))
    }

    fun cores(destaque: Int, secundaria: Int) {
        c1 = destaque
        c2 = secundaria
        relevo.color = misturar(secundaria, 0xFF04060D.toInt(), 0.62f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val larg = 652f
        val alt = 180f
        val s = minOf(width / larg, height / alt)
        val dx = (width - larg * s) / 2f
        val dy = (height - alt * s) / 2f
        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(s, s)
        for (i in 4 downTo 1) {
            canvas.save()
            canvas.translate(i * 2.4f, i * 2.4f)
            canvas.drawPath(forma, relevo)
            canvas.restore()
        }
        face.shader = LinearGradient(0f, 0f, 640f, 170f, c1, c2, Shader.TileMode.CLAMP)
        canvas.drawPath(forma, face)
        canvas.drawPath(forma, borda)
        canvas.restore()
    }

    companion object {
        private fun poligono(p: Path, v: FloatArray) {
            p.moveTo(v[0], v[1])
            var i = 2
            while (i < v.size) { p.lineTo(v[i], v[i + 1]); i += 2 }
            p.close()
        }

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

package tech.vetorial.velox

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** A caixa de diálogo do Velox: mesmo desenho do resto do app (cantos recortados, cores e brilho do tema). Usa a mesma ordem de chamadas do AlertDialog.Builder. */
class CaixaDialogo(a: Activity) : Dialog(a) {
    internal val botoes = HashMap<Int, Button>()
    fun getButton(qual: Int): Button? = botoes[qual]
}

class CaixaVelox(private val a: Activity, private val tema: Tema) {
    private var titulo: CharSequence? = null
    private var mensagem: CharSequence? = null
    private var visao: View? = null
    private var itens: Array<out CharSequence>? = null
    private var aoEscolher: DialogInterface.OnClickListener? = null
    private var cancelavel = true
    private var aoCancelar: DialogInterface.OnCancelListener? = null
    private class Botao(val texto: CharSequence, val ao: DialogInterface.OnClickListener?)
    private val bt = HashMap<Int, Botao>()

    private fun dp(v: Int): Int = (v * a.resources.displayMetrics.density + 0.5f).toInt()
    private fun com(c: Int, alfa: Int): Int = (c and 0x00FFFFFF) or (alfa shl 24)

    fun setTitle(t: CharSequence?) = apply { titulo = t }
    fun setMessage(t: CharSequence?) = apply { mensagem = t }
    fun setView(v: View?) = apply { visao = v }
    fun setCancelable(v: Boolean) = apply { cancelavel = v }
    fun setOnCancelListener(l: DialogInterface.OnCancelListener?) = apply { aoCancelar = l }
    fun setItems(lista: Array<out CharSequence>, l: DialogInterface.OnClickListener?) = apply { itens = lista; aoEscolher = l }
    fun setPositiveButton(t: CharSequence, l: DialogInterface.OnClickListener?) = apply { bt[DialogInterface.BUTTON_POSITIVE] = Botao(t, l) }
    fun setNegativeButton(t: CharSequence, l: DialogInterface.OnClickListener?) = apply { bt[DialogInterface.BUTTON_NEGATIVE] = Botao(t, l) }
    fun setNeutralButton(t: CharSequence, l: DialogInterface.OnClickListener?) = apply { bt[DialogInterface.BUTTON_NEUTRAL] = Botao(t, l) }

    private fun moldura(preenche: Int, contorno: Int, esp: Int, corte: Int) = PainelCortado(preenche, contorno, esp.toFloat(), corte.toFloat(), tema.redondo)

    /** Deixa campos de texto e rótulos de uma tela montada por fora no mesmo visual. */
    private fun pintar(v: View) {
        if (v is EditText) {
            v.setTextColor(tema.texto); v.setHintTextColor(tema.suave)
            v.background = moldura(com(tema.campo, 235), com(tema.destaque, 150), dp(1), dp(8))
            v.setPadding(dp(12), dp(10), dp(12), dp(10))
        } else if (v is TextView) { v.setTextColor(tema.texto) }
        if (v is ViewGroup) for (i in 0 until v.childCount) pintar(v.getChildAt(i))
    }

    fun create(): CaixaDialogo {
        val d = CaixaDialogo(a)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val painel = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            background = moldura(com(tema.superficie, 250), com(tema.destaque, 210), dp(2), dp(16))
            setPadding(dp(20), dp(18), dp(20), dp(14))
        }
        titulo?.let {
            painel.addView(TextView(a).apply {
                text = it.toString().uppercase(); textSize = 17f; setTextColor(tema.destaque)
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD); letterSpacing = 0.06f
            })
            // filete de neon sob o título, como nas barras do app
            painel.addView(View(a).apply { background = ColorDrawable(com(tema.destaque, 140)) }, LinearLayout.LayoutParams(dp(56), dp(2)).apply { topMargin = dp(8); bottomMargin = dp(12) })
        }
        val corpo = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        mensagem?.let { corpo.addView(TextView(a).apply { text = it; textSize = 15f; setTextColor(tema.texto); setLineSpacing(0f, 1.15f) }) }
        visao?.let { v -> pintar(v); (v.parent as? ViewGroup)?.removeView(v); corpo.addView(v, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) }) }
        itens?.forEachIndexed { i, t ->
            corpo.addView(TextView(a).apply {
                text = t; textSize = 15f; setTextColor(tema.texto); setPadding(dp(14), dp(12), dp(14), dp(12))
                background = moldura(com(tema.campo, 220), com(tema.destaque, 90), dp(1), dp(8))
                setOnClickListener { aoEscolher?.onClick(d, i); d.dismiss() }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) })
        }
        painel.addView(ScrollView(a).apply { addView(corpo) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        if (bt.isNotEmpty()) {
            // com três botões a linha não cabe no celular e o texto quebrava ("Salv / ar"): eles ficam um embaixo do outro, o principal em cima
            val empilhar = bt.size >= 3
            val linha = LinearLayout(a).apply { orientation = if (empilhar) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL; gravity = Gravity.END }
            val ordem = listOf(DialogInterface.BUTTON_NEUTRAL, DialogInterface.BUTTON_NEGATIVE, DialogInterface.BUTTON_POSITIVE)
            for (qual in if (empilhar) ordem.reversed() else ordem) {
                val b = bt[qual] ?: continue
                val principal = qual == DialogInterface.BUTTON_POSITIVE
                val botao = Button(a).apply {
                    text = b.texto; isAllCaps = false; textSize = 14f; minHeight = dp(44); minimumHeight = dp(44)
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(if (principal) tema.sobreDestaque else tema.destaque)
                    background = moldura(if (principal) tema.destaque else com(tema.campo, 120), tema.destaque, dp(1), dp(10))
                    setPadding(dp(16), 0, dp(16), 0); maxLines = 1
                    setOnClickListener { b.ao?.onClick(d, qual); d.dismiss() }
                }
                d.botoes[qual] = botao
                linha.addView(botao, LinearLayout.LayoutParams(if (empilhar) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { if (empilhar) topMargin = dp(8) else leftMargin = dp(8) })
            }
            painel.addView(linha, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) })
        }
        d.setContentView(painel)
        d.setCancelable(cancelavel)
        d.setOnCancelListener(aoCancelar)
        d.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val largura = minOf((a.resources.displayMetrics.widthPixels * 0.92f).toInt(), dp(420))
            setLayout(largura, ViewGroup.LayoutParams.WRAP_CONTENT)
            setDimAmount(0.65f)
            setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        return d
    }

    fun show(): CaixaDialogo = create().also { it.show() }
}

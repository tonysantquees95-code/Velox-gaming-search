package tech.vetorial.velox

/** Cores do navegador. Cada tema muda barras, destaque e a página inicial. */
class Tema(
    val id: String,
    val nome: String,
    val fundo: Int,
    val superficie: Int,
    val campo: Int,
    val destaque: Int,
    val secundaria: Int,
    val texto: Int,
    val suave: Int,
    val claro: Boolean
) {
    fun hex(cor: Int): String = String.format("#%06X", cor and 0xFFFFFF)

    /** Para a página inicial: o "#" não pode ir no endereço (começaria o fragmento). */
    fun semJogo(cor: Int): String = String.format("%06X", cor and 0xFFFFFF)

    companion object {
        val NEON = Tema("neon", "Neon", 0xFF070B14.toInt(), 0xFF0D1530.toInt(), 0xFF141E40.toInt(), 0xFF22D3EE.toInt(), 0xFFA78BFA.toInt(), 0xFFFFFFFF.toInt(), 0xFF8FA0CF.toInt(), false)
        val POR_DO_SOL = Tema("sol", "Pôr do sol", 0xFF1A0716.toInt(), 0xFF2B0B24.toInt(), 0xFF3C1232.toInt(), 0xFFFF4D6D.toInt(), 0xFFFFB020.toInt(), 0xFFFFF3EA.toInt(), 0xFFD9A08A.toInt(), false)
        val CROMADO = Tema("cromado", "Cromado (claro)", 0xFFF4F6FA.toInt(), 0xFFE5E9F0.toInt(), 0xFFFFFFFF.toInt(), 0xFF3B6FD4.toInt(), 0xFF5B6B86.toInt(), 0xFF1E293B.toInt(), 0xFF566179.toInt(), true)
        val TODOS = listOf(NEON, POR_DO_SOL, CROMADO)
        fun porId(id: String?): Tema = TODOS.firstOrNull { it.id == id } ?: NEON
    }
}

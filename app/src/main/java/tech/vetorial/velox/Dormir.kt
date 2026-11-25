package tech.vetorial.velox

/** Abas dormindo: a aba parada em segundo plano é esvaziada para poupar memória e volta ao endereço de antes quando a pessoa abre. Só a regra fica aqui (pura e testada). */
object Dormir {
    const val MINUTOS = 15

    data class Candidata(val id: Int, val anonima: Boolean, val carregando: Boolean, val dormindo: Boolean, val url: String, val ultimoUso: Long)

    /** Quais abas podem dormir agora. Nunca dorme a que está na tela, a anônima, a que ainda carrega, a que já dorme e a que não é site. */
    fun escolher(abas: List<Candidata>, agora: Long, naTela: Set<Int>, minutos: Int = MINUTOS): List<Int> {
        val limite = minutos.coerceIn(5, 240) * 60_000L
        return abas.filter {
            it.id !in naTela && !it.anonima && !it.carregando && !it.dormindo &&
                (it.url.startsWith("http://") || it.url.startsWith("https://")) && agora - it.ultimoUso >= limite
        }.map { it.id }
    }

    /** "1 aba dormindo" / "3 abas dormindo" (vazio se nenhuma). */
    fun frase(n: Int): String = when {
        n <= 0 -> ""
        n == 1 -> "1 aba dormindo"
        else -> "$n abas dormindo"
    }
}

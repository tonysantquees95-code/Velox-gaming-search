package tech.vetorial.velox

import org.junit.Assert.assertEquals
import org.junit.Test

class DormirTest {
    private val min = 60_000L
    private fun aba(id: Int, uso: Long, anonima: Boolean = false, carregando: Boolean = false, dormindo: Boolean = false, url: String = "https://exemplo.com/a") =
        Dormir.Candidata(id, anonima, carregando, dormindo, url, uso)

    @Test fun soDormeQuemFicouParadoAlemDoLimite() {
        val agora = 100 * min
        val r = Dormir.escolher(listOf(aba(1, agora - 20 * min), aba(2, agora - 5 * min)), agora, emptySet(), 15)
        assertEquals(listOf(1), r)
    }

    @Test fun nuncaDormeAbaNaTelaAnonimaCarregandoJaDormindoOuQueNaoESite() {
        val agora = 500 * min
        val abas = listOf(
            aba(1, 0), aba(2, 0, anonima = true), aba(3, 0, carregando = true), aba(4, 0, dormindo = true),
            aba(5, 0, url = "file:///android_asset/inicio.html"), aba(6, 0)
        )
        assertEquals(listOf(1), Dormir.escolher(abas, agora, setOf(6), 15))
    }

    @Test fun minutosForaDaFaixaViramSeguros() {
        val agora = 10 * min
        // 1 minuto vira 5; a aba parada há 6 minutos dorme, a parada há 4 não
        val r = Dormir.escolher(listOf(aba(1, agora - 6 * min), aba(2, agora - 4 * min)), agora, emptySet(), 1)
        assertEquals(listOf(1), r)
    }

    @Test fun fraseEmPortugues() {
        assertEquals("", Dormir.frase(0))
        assertEquals("1 aba dormindo", Dormir.frase(1))
        assertEquals("3 abas dormindo", Dormir.frase(3))
    }
}

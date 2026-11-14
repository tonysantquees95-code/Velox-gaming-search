package tech.vetorial.velox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApresentacaoTest {
    @Test fun novidadesTemIdsUnicosCrescentesETextos() {
        val ids = Apresentacao.NOVIDADES.map { it.id }
        assertEquals(ids.distinct(), ids)
        assertEquals(ids.sorted(), ids)
        for (n in Apresentacao.NOVIDADES) {
            assertTrue("tipo inválido ${n.id}", n.tipo in listOf("novo", "melhoria", "correcao"))
            assertTrue(Regex("\\d{4}-\\d{2}-\\d{2}").matches(n.data))
            assertTrue(n.titulo.trim().length >= 4 && n.texto.trim().length >= 10)
        }
    }

    @Test fun mostraSoOQueAindaNaoViu() {
        val l = (1..4).map { Apresentacao.Novidade(it, "2026-11-14", "novo", "titulo $it", "texto longo $it") }
        assertEquals(listOf(4, 3), Apresentacao.novasDesde(2, l).map { it.id })
        assertTrue(Apresentacao.novasDesde(4, l).isEmpty())
        assertEquals(4, Apresentacao.ultimoId(l))
        assertEquals(0, Apresentacao.ultimoId(emptyList()))
    }

    @Test fun apresentacaoCompleta() {
        assertTrue(Apresentacao.TOUR.size >= 4 && Apresentacao.TERMOS.size >= 5)
        assertTrue(Apresentacao.precisaApresentar(0, true))
        assertTrue(Apresentacao.precisaApresentar(Apresentacao.VERSAO_DOS_TERMOS, false))
        assertFalse(Apresentacao.precisaApresentar(Apresentacao.VERSAO_DOS_TERMOS, true))
    }
}

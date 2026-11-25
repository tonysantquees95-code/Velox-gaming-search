package tech.vetorial.velox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AbasEnviadasTest {
    private val t = 1_700_000_000_000L

    @Test fun soAceitaSiteHttpSemUsuarioESenha() {
        assertNotNull(AbasEnviadas.urlValida("https://exemplo.com/a?b=1"))
        assertNull(AbasEnviadas.urlValida("javascript:alert(1)"))
        assertNull(AbasEnviadas.urlValida("file:///sdcard/x"))
        assertNull(AbasEnviadas.urlValida("https://user:pass@exemplo.com"))
        assertNull(AbasEnviadas.urlValida("https://localhost/"))
        assertNull(AbasEnviadas.urlValida("https://exemplo.com/" + "a".repeat(2100)))
    }

    @Test fun quemEnviouNaoRecebeDeVoltaEOOutroRecebeUmaVez() {
        val d = AbasEnviadas.enviar(AbasEnviadas.Doc(emptyList()), "https://exemplo.com/", "Exemplo", "cel", "Android", t)
        assertEquals(0, AbasEnviadas.pendentes(d, "cel", t).size)
        assertEquals(1, AbasEnviadas.pendentes(d, "pc", t).size)
        val visto = AbasEnviadas.marcarVisto(d, d.itens[0].id, "pc", t)
        assertEquals(0, AbasEnviadas.pendentes(visto, "pc", t).size)
    }

    @Test fun reenviarNaoDuplicaEItensVelhosSomemEmSeteDias() {
        var d = AbasEnviadas.enviar(AbasEnviadas.Doc(emptyList()), "https://exemplo.com/", "", "cel", "Android", t)
        d = AbasEnviadas.enviar(d, "https://exemplo.com/", "", "cel", "Android", t + 1000)
        assertEquals(1, d.itens.size)
        assertEquals(0, AbasEnviadas.limpar(d, t + 8 * 86_400_000L).itens.size)
    }

    @Test fun listaTemLimiteEEnderecoRuimEIgnorado() {
        var d = AbasEnviadas.Doc(emptyList())
        for (i in 0 until AbasEnviadas.MAXIMO + 5) d = AbasEnviadas.enviar(d, "https://exemplo.com/$i", "", "cel", "Android", t + i)
        assertEquals(AbasEnviadas.MAXIMO, d.itens.size)
        assertTrue(AbasEnviadas.enviar(d, "javascript:1", "", "cel", "Android", t + 99).itens.size == AbasEnviadas.MAXIMO)
    }
}

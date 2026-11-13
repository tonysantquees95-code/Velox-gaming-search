package tech.vetorial.velox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemaTest {
    @Test fun textoVazioDaOPadraoNeon() {
        val t = Tema.daString("")
        assertEquals(0xFF22D3EE.toInt(), t.destaque)
        assertEquals(0xFFA78BFA.toInt(), t.secundaria)
        assertEquals(0xFF070B14.toInt(), t.fundo)
        assertEquals(70, t.brilho)
        assertFalse(t.redondo)
        assertTrue(t.animacoes)
    }

    @Test fun leOQueOPainelMandou() {
        val t = Tema.daString("c1=ff2a3d&c2=b0001a&bg=12060b&br=40&fo=redondo&an=0&fu=grade")
        assertEquals(0xFFFF2A3D.toInt(), t.destaque)
        assertEquals(0xFFB0001A.toInt(), t.secundaria)
        assertEquals(0xFF12060B.toInt(), t.fundo)
        assertEquals(40, t.brilho)
        assertTrue(t.redondo)
        assertFalse(t.animacoes)
        assertTrue(t.params().contains("fu=grade"))
    }

    @Test fun valorEstranhoVoltaAoPadrao() {
        val t = Tema.daString("c1=<script>&c2=12&br=999&fo=xyz&fu=qualquer&an=2&x=1")
        assertEquals(0xFF22D3EE.toInt(), t.destaque)
        assertEquals(0xFFA78BFA.toInt(), t.secundaria)
        assertEquals(100, t.brilho)
        assertFalse(t.redondo)
        assertTrue(t.params().contains("fu=aurora"))
        assertFalse(t.params().contains("script"))
    }

    @Test fun textoLegivelSobreDestaque() {
        assertEquals(0xFF06090F.toInt(), Tema.daString("c1=ffc21a").sobreDestaque) // dourado claro: texto escuro
        assertEquals(0xFFFFFFFF.toInt(), Tema.daString("c1=1b3dbd").sobreDestaque) // azul escuro: texto branco
    }
}

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

    @Test fun leEstiloPacoteEEscurecimento() {
        val t = Tema.daString("ti=neon&pk=duo&dim=60")
        assertEquals("neon", t.atalhos)
        assertEquals("duo", t.pacote)
        assertEquals(60, t.escurecer)
        assertTrue(t.params().contains("pk=duo"))
        val ruim = Tema.daString("ti=x&pk=<b>&dim=abc")
        assertEquals("marca", ruim.atalhos)
        assertEquals("linha", ruim.pacote)
        assertEquals(35, ruim.escurecer)
        assertEquals(80, Tema.daString("dim=500").escurecer)
    }

    @Test fun textoLegivelSobreDestaque() {
        assertEquals(0xFF06090F.toInt(), Tema.daString("c1=ffc21a").sobreDestaque) // dourado claro: texto escuro
        assertEquals(0xFFFFFFFF.toInt(), Tema.daString("c1=1b3dbd").sobreDestaque) // azul escuro: texto branco
    }
}

class AtualizadorTest {
    @Test fun soBaixaDoGithubPorHttps() {
        assertTrue(Atualizador.enderecoPermitido("https://github.com/x/y/releases/download/v1.2/Velox.apk"))
        assertTrue(Atualizador.enderecoPermitido("https://objects.githubusercontent.com/abc"))
        assertFalse(Atualizador.enderecoPermitido("http://github.com/x"))
        assertFalse(Atualizador.enderecoPermitido("https://github.com.mal.com/x"))
        assertFalse(Atualizador.enderecoPermitido("https://exemplo.com/Velox.apk"))
    }

    @Test fun comparaVersoes() {
        assertTrue(Atualizador.haNova(5, 6))
        assertFalse(Atualizador.haNova(6, 6))
        assertFalse(Atualizador.haNova(0, 9)) // versão de desenvolvimento não se atualiza
        assertFalse(Atualizador.haNova(5, null))
    }

    @Test fun conferePeloIntervalo() {
        val h = 3_600_000L
        assertTrue(Atualizador.precisaConferir(0, 1000))
        assertFalse(Atualizador.precisaConferir(10 * h, 12 * h))
        assertTrue(Atualizador.precisaConferir(10 * h, 17 * h))
        assertTrue(Atualizador.precisaConferir(10 * h, 9 * h)) // relógio voltou
    }
}

class ImagensTest {
    @Test fun soAceitaHttps() {
        assertTrue(Imagens.enderecoPermitido("https://exemplo.com/foto.jpg"))
        assertFalse(Imagens.enderecoPermitido("http://exemplo.com/foto.jpg"))
        assertFalse(Imagens.enderecoPermitido("file:///sdcard/x.jpg"))
        assertFalse(Imagens.enderecoPermitido("javascript:alert(1)"))
        assertFalse(Imagens.enderecoPermitido(""))
        assertFalse(Imagens.enderecoPermitido(null))
    }

    @Test fun reduzSemPassarDoAlvo() {
        assertEquals(1, Imagens.reducao(800, 600, 1600))
        assertEquals(2, Imagens.reducao(4000, 3000, 1600))
        assertEquals(4, Imagens.reducao(8000, 6000, 1600))
    }

    @Test fun slotsConhecidos() {
        assertTrue(Imagens.slotValido("voltar"))
        assertFalse(Imagens.slotValido("../x"))
        assertFalse(Imagens.slotValido(null))
    }
}

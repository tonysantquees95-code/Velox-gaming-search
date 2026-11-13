package tech.vetorial.velox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnderecosTest {
    private val g = Buscador.GOOGLE

    @Test fun enderecoCompletoFicaComoEsta() {
        assertEquals("https://exemplo.com/a?b=1", Enderecos.resolver("https://exemplo.com/a?b=1", g))
        assertEquals("http://exemplo.com", Enderecos.resolver("  http://exemplo.com ", g))
    }

    @Test fun dominioSemEsquemaViraHttps() {
        assertEquals("https://velox.app", Enderecos.resolver("velox.app", g))
        assertEquals("https://www.uol.com.br/esporte", Enderecos.resolver("www.uol.com.br/esporte", g))
    }

    @Test fun enderecoLocalVaiPorHttp() {
        assertEquals("http://localhost:3000", Enderecos.resolver("localhost:3000", g))
        assertEquals("http://192.168.0.1", Enderecos.resolver("192.168.0.1", g))
    }

    @Test fun textoComEspacoOuSemPontoViraBusca() {
        assertEquals("https://www.google.com/search?q=receita+de+bolo", Enderecos.resolver("receita de bolo", g))
        assertEquals("https://duckduckgo.com/?q=velox", Enderecos.resolver("velox", Buscador.DUCK))
    }

    @Test fun numeroComPontoNaoEDominio() {
        assertTrue(Enderecos.resolver("3.14", g).startsWith("https://www.google.com/search"))
    }

    @Test fun vazioContinuaVazio() {
        assertEquals("", Enderecos.resolver("   ", g))
    }

    @Test fun hospedeDaUrl() {
        assertEquals("www.exemplo.com", Enderecos.hospedeDe("https://www.exemplo.com:8080/a?b#c"))
        assertEquals("exemplo.com", Enderecos.hospedeDe("https://usuario@exemplo.com/x"))
        assertNull(Enderecos.hospedeDe(""))
    }

    @Test fun paginaInicialNaoMostraEnderecoFeio() {
        assertEquals("", Enderecos.paraMostrar("file:///android_asset/inicio.html?bg=070B14"))
        assertEquals("https://a.com", Enderecos.paraMostrar("https://a.com"))
    }

    @Test fun buscadorPorIdUsaGoogleSeNaoAcha() {
        assertEquals(Buscador.GOOGLE, Buscador.porId("nao-existe"))
        assertEquals(Buscador.BING, Buscador.porId("bing"))
    }

    @Test fun anunciosBarramSubdominios() {
        assertTrue(ListaDeAnuncios.bloqueia("doubleclick.net"))
        assertTrue(ListaDeAnuncios.bloqueia("ad.doubleclick.net"))
        assertTrue(ListaDeAnuncios.bloqueia("AD.DoubleClick.NET"))
        assertFalse(ListaDeAnuncios.bloqueia("net"))
        assertFalse(ListaDeAnuncios.bloqueia("exemplo.com"))
        assertFalse(ListaDeAnuncios.bloqueia("notdoubleclick.net.exemplo.com"))
        assertFalse(ListaDeAnuncios.bloqueia(null))
    }
}

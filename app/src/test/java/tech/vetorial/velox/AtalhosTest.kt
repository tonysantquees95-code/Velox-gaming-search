package tech.vetorial.velox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AtalhosTest {
    @Test fun soHttpViraAtalho() {
        assertTrue(Atalhos.enderecoValido("https://exemplo.com/a?b=1"))
        assertTrue(Atalhos.enderecoValido("http://x.org"))
        assertFalse(Atalhos.enderecoValido("file:///sdcard/a.html"))
        assertFalse(Atalhos.enderecoValido("javascript:alert(1)"))
        assertFalse(Atalhos.enderecoValido(""))
        assertFalse(Atalhos.enderecoValido(null))
    }

    @Test fun nomeSugeridoUsaTituloOuSite() {
        assertEquals("Minha loja", Atalhos.nomeSugerido("Minha loja", "https://a.com"))
        assertEquals("youtube.com", Atalhos.nomeSugerido("", "https://www.youtube.com/watch?v=1"))
        assertEquals("bing.com", Atalhos.nomeSugerido("Nova aba", "https://www.bing.com"))
        assertEquals(40, Atalhos.nomeSugerido("a".repeat(90), "https://a.com").length)
    }

    @Test fun nomeCurtoLimpaEEncurta() {
        assertEquals("Meu site", Atalhos.nomeCurto("  Meu    site ", "x"))
        assertEquals("padrao", Atalhos.nomeCurto("   ", "padrao"))
        assertEquals(30, Atalhos.nomeCurto("b".repeat(80), "x").length)
    }

    @Test fun mesmoEnderecoMesmoIdEEnderecosDiferentesNao() {
        assertEquals(Atalhos.idDe("https://a.com/x"), Atalhos.idDe("https://a.com/x"))
        assertNotEquals(Atalhos.idDe("https://a.com/x"), Atalhos.idDe("https://a.com/y"))
        assertTrue(Atalhos.idDe("https://a.com").startsWith("site-"))
    }
}

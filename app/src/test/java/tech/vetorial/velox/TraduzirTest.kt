package tech.vetorial.velox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TraduzirTest {
    @Test fun montaOEnderecoEVoltaAoOriginal() {
        val t = Traduzir.urlTraduzida("https://exemplo.com/a%20b?x=1")!!
        assertTrue(t.startsWith("https://translate.google.com/translate?"))
        assertTrue(Traduzir.ehTraduzida(t))
        assertEquals("https://exemplo.com/a%20b?x=1", Traduzir.urlOriginal(t))
        assertTrue(Traduzir.ehTraduzida("https://www-exemplo-com.translate.goog/a?_x_tr_sl=auto"))
    }

    @Test fun naoTraduzOQueNaoESiteNemOQueJaETraducao() {
        assertNull(Traduzir.urlTraduzida("file:///android_asset/inicio.html"))
        assertNull(Traduzir.urlTraduzida(""))
        assertNull(Traduzir.urlTraduzida(Traduzir.urlTraduzida("https://exemplo.com/")!!))
        assertNull(Traduzir.urlOriginal("https://exemplo.com/"))
        assertFalse(Traduzir.ehTraduzida("https://exemplo.com/"))
    }
}

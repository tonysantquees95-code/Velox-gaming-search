package tech.vetorial.velox

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnunciosTest {
    @Test fun bloqueiaDominioESubdominio() {
        assertTrue(ListaDeAnuncios.bloqueia("doubleclick.net"))
        assertTrue(ListaDeAnuncios.bloqueia("static.doubleclick.net"))
        assertTrue(ListaDeAnuncios.bloqueia("pagead2.googlesyndication.com"))
        assertTrue(ListaDeAnuncios.bloqueia("tracker.teads.tv"))
        assertTrue(ListaDeAnuncios.bloqueia("px.mathtag.com"))
    }

    @Test fun naoBloqueiaSitesNormais() {
        assertFalse(ListaDeAnuncios.bloqueia("exemplo.com"))
        assertFalse(ListaDeAnuncios.bloqueia("www.google.com"))
        assertFalse(ListaDeAnuncios.bloqueia("notdoubleclick.net.exemplo.com"))
        assertFalse(ListaDeAnuncios.bloqueia(null))
    }

    @Test fun listaCresceuEACssEscondeAnunciosEAvisosDeCookie() {
        assertTrue(ListaDeAnuncios.tamanho() >= 150)
        val css = LimpezaDePagina.css()
        assertTrue(css.contains(".adsbygoogle"))
        assertTrue(css.contains("#onetrust-banner-sdk"))
        assertTrue(css.endsWith("{display:none!important}"))
        assertFalse(css.contains("\n"))
    }

    @Test fun scriptDeLimpezaNaoQuebraComAspas() {
        val js = LimpezaDePagina.js()
        assertTrue(js.startsWith("(function(){"))
        assertTrue(js.contains("__vxcss"))
    }
}

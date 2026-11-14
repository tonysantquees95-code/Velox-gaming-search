package tech.vetorial.velox

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CofreTest {
    @Test fun cifraEDecifraComAMesmaChave() {
        val sal = ByteArray(16) { it.toByte() }
        val k = Cofre.derivar("senha-mestra", sal)
        val claro = "segredo-bem-unico".toByteArray()
        val pacote = Cofre.cifrar(k, claro)
        assertFalse(String(pacote, Charsets.ISO_8859_1).contains("segredo-bem-unico"))
        assertArrayEquals(claro, Cofre.decifrar(k, pacote))
    }

    @Test fun senhaErradaNaoAbre() {
        val sal = ByteArray(16) { (it * 3).toByte() }
        val pacote = Cofre.cifrar(Cofre.derivar("certa-123", sal), "x".toByteArray())
        try {
            Cofre.decifrar(Cofre.derivar("errada-123", sal), pacote)
            fail("não podia abrir")
        } catch (e: Exception) {
            assertTrue(true)
        }
    }

    @Test fun chaveTem32Bytes() {
        assertEquals(32, Cofre.derivar("abc123", ByteArray(16)).size)
    }
}

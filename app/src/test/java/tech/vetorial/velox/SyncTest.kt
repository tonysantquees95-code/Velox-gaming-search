package tech.vetorial.velox

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncTest {
    private fun f(u: String, t: Long) = Sync.Fav(u, u, t)

    @Test fun unePorEnderecoSemRepetir() {
        val r = Sync.mesclar(Sync.Doc(listOf(f("a", 1), f("b", 2)), emptyMap()), Sync.Doc(listOf(f("b", 2), f("c", 3)), emptyMap()))
        assertEquals(listOf("a", "b", "c"), r.favoritos.map { it.u })
    }

    @Test fun apagadoDepoisDeCriadoSome() {
        val agora = System.currentTimeMillis()
        val r = Sync.mesclar(Sync.Doc(emptyList(), mapOf("b" to agora)), Sync.Doc(listOf(f("a", 1), f("b", agora - 1000)), emptyMap()), agora)
        assertEquals(listOf("a"), r.favoritos.map { it.u })
    }

    @Test fun favoritadoDeNovoDepoisDeApagadoVolta() {
        val agora = System.currentTimeMillis()
        val r = Sync.mesclar(Sync.Doc(listOf(f("b", agora)), emptyMap()), Sync.Doc(emptyList(), mapOf("b" to agora - 5000)), agora)
        assertEquals(listOf("b"), r.favoritos.map { it.u })
    }

    @Test fun apagadosVelhosSaemDaLista() {
        val agora = System.currentTimeMillis()
        val r = Sync.mesclar(Sync.Doc(emptyList(), mapOf("x" to agora - 70L * 86_400_000, "y" to agora)), null, agora)
        assertEquals(setOf("y"), r.apagados.keys)
    }
}

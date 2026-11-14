package tech.vetorial.velox

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cofre de senhas do Velox: arquivo local cifrado com AES-256-GCM; a chave vem da senha mestra (PBKDF2-HMAC-SHA256).
 * A senha mestra nunca é guardada. Tranca sozinho depois de 15 minutos sem uso.
 */
class Cofre(private val arquivo: File, private val minutosAteTravar: Long = 15) {
    data class Item(val id: String, val origem: String, val usuario: String, var senha: String, var atualizado: Long)

    private var chave: ByteArray? = null
    private var sal: ByteArray? = null
    private var itens = mutableListOf<Item>()
    private var nunca = mutableSetOf<String>()
    /** "origem\nusuario" -> quando foi apagado (a sincronização usa para apagar nos outros aparelhos). */
    private var apagados = mutableMapOf<String, Long>()
    /** Só enquanto aberto: a sincronização das senhas cifra com ela. */
    private var senhaMem: String? = null
    private var ultimoUso = 0L

    fun existe() = arquivo.exists()

    fun aberto(): Boolean {
        if (chave == null) return false
        if (System.currentTimeMillis() - ultimoUso > minutosAteTravar * 60_000) { travar(); return false }
        return true
    }

    private fun exigir() {
        if (!aberto()) throw IllegalStateException("O cofre está trancado.")
        ultimoUso = System.currentTimeMillis()
    }

    fun travar() {
        chave = null
        sal = null
        senhaMem = null
        itens = mutableListOf()
        nunca = mutableSetOf()
        apagados = mutableMapOf()
    }

    fun criar(senha: String) {
        if (senha.length < 6) throw IllegalArgumentException("A senha mestra precisa ter pelo menos 6 caracteres.")
        if (existe()) throw IllegalStateException("Já existe um cofre neste aparelho.")
        val s = ByteArray(16).also { SecureRandom().nextBytes(it) }
        sal = s
        chave = derivar(senha, s)
        senhaMem = senha
        itens = mutableListOf()
        nunca = mutableSetOf()
        apagados = mutableMapOf()
        ultimoUso = System.currentTimeMillis()
        gravar()
    }

    fun abrir(senha: String) {
        val o = JSONObject(arquivo.readText(Charsets.UTF_8))
        val s = Base64.getDecoder().decode(o.getString("sal"))
        val k = derivar(senha, s)
        val claro = try { decifrar(k, Base64.getDecoder().decode(o.getString("dados"))) } catch (e: Exception) { throw IllegalArgumentException("Senha mestra incorreta.") }
        val d = JSONObject(String(claro, Charsets.UTF_8))
        val lista = mutableListOf<Item>()
        val a = d.optJSONArray("itens") ?: JSONArray()
        for (i in 0 until a.length()) {
            val x = a.getJSONObject(i)
            lista.add(Item(x.getString("id"), x.getString("origem"), x.optString("usuario"), x.getString("senha"), x.optLong("atualizado")))
        }
        val n = mutableSetOf<String>()
        val na = d.optJSONArray("nunca") ?: JSONArray()
        for (i in 0 until na.length()) n.add(na.getString(i))
        val ap = mutableMapOf<String, Long>()
        val apObj = d.optJSONObject("apagados")
        if (apObj != null) for (k2 in apObj.keys()) ap[k2] = apObj.optLong(k2, 0L)
        sal = s
        chave = k
        senhaMem = senha
        itens = lista
        nunca = n
        apagados = ap
        ultimoUso = System.currentTimeMillis()
    }

    private fun gravar() {
        val d = JSONObject()
        val a = JSONArray()
        for (i in itens) a.put(JSONObject().put("id", i.id).put("origem", i.origem).put("usuario", i.usuario).put("senha", i.senha).put("atualizado", i.atualizado))
        val apJson = JSONObject()
        for ((k, v) in apagados) apJson.put(k, v)
        d.put("itens", a).put("nunca", JSONArray(nunca.toList())).put("apagados", apJson)
        val pacote = JSONObject().put("v", 1)
            .put("sal", Base64.getEncoder().encodeToString(sal))
            .put("dados", Base64.getEncoder().encodeToString(cifrar(chave!!, d.toString().toByteArray(Charsets.UTF_8))))
        val tmp = File(arquivo.path + ".tmp")
        tmp.writeText(pacote.toString(), Charsets.UTF_8)
        if (!tmp.renameTo(arquivo)) { arquivo.writeText(pacote.toString(), Charsets.UTF_8); tmp.delete() }
    }

    /** Logins guardados para um site (origem como https://exemplo.com). */
    fun doSite(origem: String): List<Item> { exigir(); return itens.filter { it.origem == origem }.sortedByDescending { it.atualizado } }

    /** Tudo, para a tela de senhas (a senha não sai daqui sem pedir). */
    fun lista(): List<Item> { exigir(); return itens.sortedBy { it.origem } }

    fun senhaDe(id: String): String? { exigir(); return itens.firstOrNull { it.id == id }?.senha }

    /** "novo", "atualizado" ou "igual". */
    fun salvar(origem: String, usuario: String, senha: String): String {
        exigir()
        val i = itens.firstOrNull { it.origem == origem && it.usuario == usuario }
        if (i != null) {
            if (i.senha == senha) return "igual"
            i.senha = senha
            i.atualizado = System.currentTimeMillis()
            gravar()
            return "atualizado"
        }
        val id = ByteArray(8).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        itens.add(Item(id, origem, usuario, senha, System.currentTimeMillis()))
        gravar()
        return "novo"
    }

    fun apagar(id: String) {
        exigir()
        itens.firstOrNull { it.id == id }?.let { apagados[it.origem + "\n" + it.usuario] = System.currentTimeMillis() }
        itens.removeAll { it.id == id }
        gravar()
    }

    /** Para a sincronização (com as senhas; só com o cofre aberto). */
    fun paraSincronia(): Pair<List<Item>, Map<String, Long>> { exigir(); return Pair(itens.toList(), apagados.toMap()) }

    fun senhaMestra(): String { exigir(); return senhaMem ?: throw IllegalStateException("O cofre está trancado.") }

    /** Troca o conteúdo pelo resultado da mescla, mantendo os ids dos itens que já existiam. */
    fun aplicarSincronia(novos: List<Item>, ap: Map<String, Long>) {
        exigir()
        val antes = itens.associate { (it.origem + "\n" + it.usuario) to it.id }
        itens = novos.map { n ->
            val id = antes[n.origem + "\n" + n.usuario] ?: ByteArray(8).also { SecureRandom().nextBytes(it) }.joinToString("") { b -> "%02x".format(b) }
            Item(id, n.origem, n.usuario, n.senha, n.atualizado)
        }.toMutableList()
        apagados = ap.toMutableMap()
        gravar()
    }

    fun jaPerguntou(origem: String): Boolean { exigir(); return origem in nunca }
    fun nuncaOferecer(origem: String) { exigir(); if (nunca.add(origem)) gravar() }

    companion object {
        private const val ITERACOES = 210_000

        fun derivar(senha: String, sal: ByteArray): ByteArray {
            val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            return f.generateSecret(PBEKeySpec(senha.toCharArray(), sal, ITERACOES, 256)).encoded
        }

        /** iv (12 bytes) + texto cifrado com a etiqueta de autenticação. */
        fun cifrar(chave: ByteArray, claro: ByteArray): ByteArray {
            val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(chave, "AES"), GCMParameterSpec(128, iv))
            return iv + c.doFinal(claro)
        }

        fun decifrar(chave: ByteArray, pacote: ByteArray): ByteArray {
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, SecretKeySpec(chave, "AES"), GCMParameterSpec(128, pacote.copyOfRange(0, 12)))
            return c.doFinal(pacote, 12, pacote.size - 12)
        }

        fun doContexto(ctx: Context) = Cofre(File(ctx.filesDir, "cofre.json"))
    }
}

package tech.vetorial.velox

import org.json.JSONArray
import org.json.JSONObject

/** "Enviar aba para o outro aparelho": uma lista pequena guardada no Vetorial ID (chave velox-abas). Cada item é visto por aparelho; quem enviou nunca recebe de volta. */
object AbasEnviadas {
    private const val SETE_DIAS = 7L * 86_400_000
    const val MAXIMO = 20

    data class Item(val id: String, val url: String, val titulo: String, val de: String, val deNome: String, val t: Long, val vistoPor: List<String>)
    data class Doc(val itens: List<Item>)

    private val ENDERECO = Regex("^(https?)://([^/?#]*)([/?#].*)?$", RegexOption.IGNORE_CASE)

    /** Só http(s), sem usuário:senha, com ponto no nome, sem espaço, até 2000 caracteres. Devolve o endereço limpo ou null. */
    fun urlValida(u: String?): String? {
        val s = (u ?: "").trim()
        if (s.isEmpty() || s.length > 2000 || s.any { it.isWhitespace() }) return null
        val m = ENDERECO.find(s) ?: return null
        val autoridade = m.groupValues[2]
        if (autoridade.contains('@')) return null
        val host = autoridade.substringBefore(':')
        return if (host.contains('.')) s else null
    }

    fun limpar(doc: Doc, agora: Long = System.currentTimeMillis()): Doc =
        Doc(doc.itens.filter { urlValida(it.url) != null && agora - it.t < SETE_DIAS }.sortedByDescending { it.t }.take(MAXIMO))

    fun enviar(doc: Doc, url: String, titulo: String, de: String, deNome: String, agora: Long = System.currentTimeMillis()): Doc {
        val u = urlValida(url)
        if (u == null || de.isBlank()) return limpar(doc, agora)
        val base = limpar(doc, agora).itens.filterNot { it.url == u && it.de == de } // reenviar a mesma página não duplica
        val novo = Item(agora.toString(36) + "-" + (1..5).map { "abcdefghijklmnopqrstuvwxyz0123456789".random() }.joinToString(""), u, titulo.take(120), de, deNome.take(30), agora, listOf(de))
        return limpar(Doc(listOf(novo) + base), agora)
    }

    /** O que este aparelho ainda não viu e que veio de outro. */
    fun pendentes(doc: Doc, meuId: String, agora: Long = System.currentTimeMillis()): List<Item> =
        limpar(doc, agora).itens.filter { it.de != meuId && meuId !in it.vistoPor }

    fun marcarVisto(doc: Doc, id: String, meuId: String, agora: Long = System.currentTimeMillis()): Doc =
        limpar(Doc(doc.itens.map { if (it.id == id && meuId !in it.vistoPor) it.copy(vistoPor = it.vistoPor + meuId) else it }), agora)

    fun paraJson(d: Doc): JSONObject {
        val a = JSONArray()
        for (i in d.itens) a.put(JSONObject().put("id", i.id).put("url", i.url).put("titulo", i.titulo).put("de", i.de).put("deNome", i.deNome).put("t", i.t).put("vistoPor", JSONArray(i.vistoPor)))
        return JSONObject().put("itens", a)
    }

    fun deJson(o: JSONObject?): Doc {
        val lista = mutableListOf<Item>()
        val a = o?.optJSONArray("itens")
        if (a != null) for (k in 0 until a.length()) {
            val x = a.optJSONObject(k) ?: continue
            val id = x.optString("id")
            if (id.isBlank()) continue
            val vistos = mutableListOf<String>()
            x.optJSONArray("vistoPor")?.let { v -> for (j in 0 until minOf(v.length(), 10)) vistos.add(v.optString(j)) }
            lista.add(Item(id, x.optString("url"), x.optString("titulo"), x.optString("de"), x.optString("deNome"), x.optLong("t", 0L), vistos))
        }
        return Doc(lista)
    }
}

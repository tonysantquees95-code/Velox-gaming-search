package tech.vetorial.velox

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Favoritos sincronizados com o Vetorial ID (celular e computador): mescla por endereço, com marca de apagado. */
object Sync {
    const val SERVIDOR = "https://venus-servidor.tonysantquees95.workers.dev"
    private const val SESSENTA_DIAS = 60L * 86_400_000

    data class Fav(val n: String, val u: String, val t: Long)
    data class Doc(val favoritos: List<Fav>, val apagados: Map<String, Long>)
    class Falha(val status: Int, mensagem: String) : Exception(mensagem)

    /** Vence quem mexeu por último em cada endereço; apagado depois de criado some nos dois lados. */
    fun mesclar(local: Doc, remoto: Doc?, agora: Long = System.currentTimeMillis()): Doc {
        val apagados = HashMap<String, Long>()
        for ((k, v) in (remoto?.apagados ?: emptyMap())) apagados[k] = maxOf(apagados[k] ?: 0L, v)
        for ((k, v) in local.apagados) apagados[k] = maxOf(apagados[k] ?: 0L, v)
        val porUrl = LinkedHashMap<String, Fav>()
        for (f in (remoto?.favoritos ?: emptyList()) + local.favoritos) {
            if (f.u.isBlank()) continue
            val atual = porUrl[f.u]
            if (atual == null || f.t >= atual.t) porUrl[f.u] = f
        }
        val vivos = porUrl.values.filter { f -> val a = apagados[f.u]; a == null || a < f.t }.sortedBy { it.t }
        return Doc(vivos, apagados.filterValues { it >= agora - SESSENTA_DIAS })
    }

    private fun igual(a: Doc, b: Doc) = a.favoritos.sortedBy { it.u } == b.favoritos.sortedBy { it.u } && a.apagados == b.apagados

    fun paraJson(d: Doc): JSONObject {
        val f = JSONArray()
        for (x in d.favoritos) f.put(JSONObject().put("n", x.n).put("u", x.u).put("t", x.t))
        val a = JSONObject()
        for ((k, v) in d.apagados) a.put(k, v)
        return JSONObject().put("favoritos", f).put("apagados", a)
    }

    fun deJson(o: JSONObject?): Doc? {
        if (o == null) return null
        val lista = mutableListOf<Fav>()
        val f = o.optJSONArray("favoritos")
        if (f != null) for (i in 0 until f.length()) {
            val x = f.optJSONObject(i) ?: continue
            val u = x.optString("u")
            if (u.isNotBlank()) lista.add(Fav(x.optString("n"), u, x.optLong("t", 0L)))
        }
        val mapa = HashMap<String, Long>()
        val a = o.optJSONObject("apagados")
        if (a != null) for (k in a.keys()) mapa[k] = a.optLong(k, 0L)
        return Doc(lista, mapa)
    }

    private fun chamar(token: String, metodo: String, caminho: String, corpo: JSONObject?): JSONObject {
        val c = URL(SERVIDOR + caminho).openConnection() as HttpURLConnection
        try {
            c.requestMethod = metodo
            c.connectTimeout = 15_000
            c.readTimeout = 20_000
            c.setRequestProperty("content-type", "application/json")
            if (token.isNotEmpty()) c.setRequestProperty("authorization", "Bearer $token")
            if (corpo != null) {
                c.doOutput = true
                c.outputStream.use { it.write(corpo.toString().toByteArray(Charsets.UTF_8)) }
            }
            val cod = c.responseCode
            val texto = (if (cod in 200..299) c.inputStream else c.errorStream)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            val j = try { JSONObject(texto) } catch (e: Exception) { JSONObject() }
            if (cod !in 200..299) throw Falha(cod, j.optString("error", "Erro $cod"))
            return j
        } finally {
            c.disconnect()
        }
    }

    /** Devolve (token, e-mail). */
    fun entrar(email: String, senha: String, aparelho: String): Pair<String, String> {
        val disp = JSONObject().put("id", aparelho).put("nome", "Velox (Android)").put("plataforma", "android")
        val r = chamar("", "POST", "/id/entrar", JSONObject().put("email", email).put("senha", senha).put("dispositivo", disp))
        val mail = r.optJSONObject("usuario")?.optString("email").orEmpty().ifBlank { email }
        return Pair(r.optString("token"), mail)
    }

    /** Baixa o que está no servidor, mescla e sobe se mudou. Devolve (documento final, mudou o local?). */
    fun sincronizar(token: String, local: Doc): Pair<Doc, Boolean> {
        for (tentativa in 0..1) {
            val lista = chamar(token, "GET", "/id/sync", null)
            var remoto: Doc? = null
            var versao = 0
            val docs = lista.optJSONArray("docs")
            if (docs != null) for (i in 0 until docs.length()) {
                val d = docs.optJSONObject(i) ?: continue
                if (d.optString("chave") == "velox") { remoto = deJson(d.optJSONObject("valor")); versao = d.optInt("versao", 0) }
            }
            val final = mesclar(local, remoto)
            val mudouLocal = !igual(final, local)
            if (remoto != null && igual(final, remoto)) return Pair(final, mudouLocal)
            try {
                chamar(token, "PUT", "/id/sync/velox", JSONObject().put("valor", paraJson(final)).put("versao", versao))
                return Pair(final, mudouLocal)
            } catch (e: Falha) {
                if (e.status != 409) throw e
            }
        }
        throw Falha(409, "Outro aparelho está mudando os favoritos agora. Tente de novo.")
    }
}

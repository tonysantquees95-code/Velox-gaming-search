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
        val r = chamar("", "POST", "/id/entrar", JSONObject().put("email", email).put("senha", senha).put("dispositivo", disp).put("app", "velox"))
        val mail = r.optJSONObject("usuario")?.optString("email").orEmpty().ifBlank { email }
        return Pair(r.optString("token"), mail)
    }

    /** Cria o Vetorial ID na hora (grátis) e já entra. Devolve (token, e-mail). */
    fun criarConta(nome: String, email: String, senha: String, aparelho: String): Pair<String, String> {
        val disp = JSONObject().put("id", aparelho).put("nome", "Velox (Android)").put("plataforma", "android")
        val r = chamar("", "POST", "/id/cadastrar", JSONObject().put("nome", nome).put("email", email).put("senha", senha).put("dispositivo", disp).put("app", "velox"))
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

    // ---------------------------------------------------------------- tema (um documento por assunto)
    class SenhaDiferente : Exception("A senha mestra deste aparelho é diferente da do outro. Use a mesma nos dois para sincronizar as senhas.")

    data class TemaRes(val mudouLocal: Boolean, val t: Long, val tema: String, val acao: String = "")

    private fun docRemoto(token: String, chave: String): Pair<JSONObject?, Int> {
        val lista = chamar(token, "GET", "/id/sync", null)
        val docs = lista.optJSONArray("docs")
        if (docs != null) for (i in 0 until docs.length()) {
            val d = docs.optJSONObject(i) ?: continue
            if (d.optString("chave") == chave) return Pair(d.optJSONObject("valor"), d.optInt("versao", 0))
        }
        return Pair(null, 0)
    }

    /** Vence a mudança mais recente. t = quando a pessoa mexeu no tema pela última vez (milissegundos). */
    fun sincronizarTema(token: String, t: Long, tema: String): TemaRes {
        for (tentativa in 0..1) {
            val (valor, versao) = docRemoto(token, "velox-tema")
            val rTema = valor?.optString("tema").orEmpty()
            val rT = valor?.optLong("t", 0L) ?: 0L
            val remotoOk = valor != null && rTema.isNotBlank()
            if (remotoOk && rT > t) return TemaRes(true, rT, rTema, "recebido do outro aparelho")
            if (remotoOk && rT == t) return TemaRes(false, rT, rTema, "igual nos dois")
            if (t <= 0L || tema.isBlank()) return TemaRes(false, t, tema, "nada para enviar ainda")
            try {
                chamar(token, "PUT", "/id/sync/velox-tema", JSONObject().put("valor", JSONObject().put("t", t).put("tema", tema)).put("versao", versao))
                return TemaRes(false, t, tema, "enviado")
            } catch (e: Falha) {
                if (e.status != 409) throw e
            }
        }
        throw Falha(409, "Outro aparelho está mudando o tema agora. Tente de novo.")
    }

    // ---------------------------------------------------------------- senhas (cifradas aqui; o servidor só guarda texto embaralhado)
    data class SenhaItem(val origem: String, val usuario: String, val senha: String, val atualizado: Long)
    data class SenhaDoc(val itens: List<SenhaItem>, val apagados: Map<String, Long>)

    private fun chaveItem(i: SenhaItem) = i.origem + "\n" + i.usuario

    fun mesclarSenhas(local: SenhaDoc, remoto: SenhaDoc?, agora: Long = System.currentTimeMillis()): SenhaDoc {
        val apagados = HashMap<String, Long>()
        for ((k, v) in (remoto?.apagados ?: emptyMap())) apagados[k] = maxOf(apagados[k] ?: 0L, v)
        for ((k, v) in local.apagados) apagados[k] = maxOf(apagados[k] ?: 0L, v)
        val por = LinkedHashMap<String, SenhaItem>()
        for (i in (remoto?.itens ?: emptyList()) + local.itens) {
            val k = chaveItem(i)
            val a = por[k]
            if (a == null || i.atualizado >= a.atualizado) por[k] = i
        }
        val vivos = por.values.filter { i -> val a = apagados[chaveItem(i)]; a == null || a < i.atualizado }.sortedBy { chaveItem(it) }
        return SenhaDoc(vivos, apagados.filterValues { it >= agora - SESSENTA_DIAS })
    }

    private fun igualSenhas(a: SenhaDoc, b: SenhaDoc) =
        a.itens.sortedBy { chaveItem(it) } == b.itens.sortedBy { chaveItem(it) } && a.apagados == b.apagados

    private fun senhasParaJson(d: SenhaDoc): JSONObject {
        val a = JSONArray()
        for (i in d.itens) a.put(JSONObject().put("origem", i.origem).put("usuario", i.usuario).put("senha", i.senha).put("atualizado", i.atualizado))
        val ap = JSONObject()
        for ((k, v) in d.apagados) ap.put(k, v)
        return JSONObject().put("itens", a).put("apagados", ap)
    }

    private fun senhasDeJson(o: JSONObject): SenhaDoc {
        val lista = mutableListOf<SenhaItem>()
        val a = o.optJSONArray("itens")
        if (a != null) for (i in 0 until a.length()) {
            val x = a.optJSONObject(i) ?: continue
            val origem = x.optString("origem")
            if (origem.isBlank() || !x.has("senha")) continue
            lista.add(SenhaItem(origem, x.optString("usuario"), x.optString("senha"), x.optLong("atualizado", 0L)))
        }
        val mapa = HashMap<String, Long>()
        val ap = o.optJSONObject("apagados")
        if (ap != null) for (k in ap.keys()) mapa[k] = ap.optLong(k, 0L)
        return SenhaDoc(lista, mapa)
    }

    /** Devolve (documento final, mudou o local?). Lança SenhaDiferente se a senha mestra daqui não abre o que o outro aparelho guardou. */
    fun sincronizarSenhas(token: String, local: SenhaDoc, senhaMestra: String): Pair<SenhaDoc, Boolean> {
        val b64 = java.util.Base64.getEncoder()
        val d64 = java.util.Base64.getDecoder()
        for (tentativa in 0..1) {
            val (valor, versao) = docRemoto(token, "velox-senhas")
            var remoto: SenhaDoc? = null
            var sal: ByteArray? = null
            if (valor != null && valor.optString("dados").isNotBlank()) {
                try {
                    val s = d64.decode(valor.getString("sal"))
                    val claro = Cofre.decifrar(Cofre.derivar(senhaMestra, s), d64.decode(valor.getString("dados")))
                    remoto = senhasDeJson(JSONObject(String(claro, Charsets.UTF_8)))
                    sal = s
                } catch (e: Exception) {
                    throw SenhaDiferente()
                }
            }
            val final = mesclarSenhas(local, remoto)
            val mudouLocal = !igualSenhas(final, local)
            if (remoto != null && igualSenhas(final, remoto)) return Pair(final, mudouLocal)
            val salUsado = sal ?: ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
            val dados = Cofre.cifrar(Cofre.derivar(senhaMestra, salUsado), senhasParaJson(final).toString().toByteArray(Charsets.UTF_8))
            val pacote = JSONObject().put("v", 1).put("sal", b64.encodeToString(salUsado)).put("dados", b64.encodeToString(dados))
            try {
                chamar(token, "PUT", "/id/sync/velox-senhas", JSONObject().put("valor", pacote).put("versao", versao))
                return Pair(final, mudouLocal)
            } catch (e: Falha) {
                if (e.status != 409) throw e
            }
        }
        throw Falha(409, "Outro aparelho está mudando as senhas agora. Tente de novo.")
    }
}

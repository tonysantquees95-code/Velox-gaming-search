package tech.vetorial.velox

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

/** Atualização do próprio Velox: pergunta ao servidor da Vênus (ou direto ao GitHub) qual é a versão mais nova, baixa, confere e instala. */
object Atualizador {
    const val SERVIDOR = "https://venus-servidor.tonysantquees95.workers.dev"
    private const val REPOSITORIO = "tonysantquees95-code/velox-gaming-search"

    class Versao(val numero: Int, val tag: String, val url: String, val sha256: String, val tamanho: Long)

    /** O app só baixa de endereços do GitHub, por https. */
    fun enderecoPermitido(url: String): Boolean {
        if (!url.startsWith("https://")) return false
        val host = try { URL(url).host.lowercase(Locale.ROOT) } catch (e: Exception) { return false }
        return host == "github.com" || host.endsWith(".github.com") || host.endsWith(".githubusercontent.com")
    }

    fun haNova(instalada: Int, nova: Int?): Boolean = instalada > 0 && nova != null && nova > instalada

    /** Passou tempo suficiente desde a última conferência? */
    fun precisaConferir(ultimo: Long, agora: Long, horas: Int = 6): Boolean = ultimo <= 0L || agora < ultimo || agora - ultimo >= horas * 3_600_000L

    fun instalada(c: Context): Int = try {
        val i = c.packageManager.getPackageInfo(c.packageName, 0)
        if (Build.VERSION.SDK_INT >= 28) i.longVersionCode.toInt() else @Suppress("DEPRECATION") i.versionCode
    } catch (e: Exception) { 0 }

    private fun ler(endereco: String): String? {
        val h = URL(endereco).openConnection() as HttpURLConnection
        h.connectTimeout = 15_000
        h.readTimeout = 20_000
        h.setRequestProperty("Accept", "application/json")
        return try { if (h.responseCode == 200) h.inputStream.bufferedReader().readText() else null } finally { h.disconnect() }
    }

    private fun daResposta(o: JSONObject): Versao? {
        val numero = o.optInt("numero", 0)
        val url = o.optString("url", "")
        if (numero <= 0 || !enderecoPermitido(url)) return null
        val sha = o.optString("sha256", "").lowercase(Locale.ROOT)
        return Versao(numero, o.optString("tag", ""), url, if (Regex("[0-9a-f]{64}").matches(sha)) sha else "", o.optLong("tamanho", 0L))
    }

    /** Versão mais nova publicada. null se nada foi encontrado. Chamar fora da tela principal (usa a rede). */
    fun procurar(): Versao? {
        try {
            val t = ler("$SERVIDOR/atualizacao?app=velox-android")
            if (t != null) daResposta(JSONObject(t))?.let { return it }
        } catch (e: Exception) { /* cai para o GitHub */ }
        try {
            val t = ler("https://api.github.com/repos/$REPOSITORIO/releases/latest") ?: return null
            val o = JSONObject(t)
            val m = Regex("^v?1\\.(\\d+)$").find(o.optString("tag_name", "")) ?: return null
            val ativos = o.optJSONArray("assets") ?: return null
            for (i in 0 until ativos.length()) {
                val a = ativos.getJSONObject(i)
                if (a.optString("name") != "Velox.apk") continue
                val dig = a.optString("digest", "")
                val dados = JSONObject().put("numero", m.groupValues[1].toInt()).put("tag", o.optString("tag_name"))
                    .put("url", a.optString("browser_download_url")).put("tamanho", a.optLong("size"))
                    .put("sha256", if (dig.startsWith("sha256:")) dig.substring(7) else "")
                return daResposta(dados)
            }
        } catch (e: Exception) { /* sem internet */ }
        return null
    }

    /** Baixa o APK para a pasta de cache e confere o SHA-256 publicado. */
    fun baixar(c: Context, v: Versao): File {
        val apk = File(c.cacheDir, "velox-atualizacao.apk")
        val h = URL(v.url).openConnection() as HttpURLConnection
        h.connectTimeout = 20_000
        h.readTimeout = 60_000
        h.instanceFollowRedirects = true
        try {
            if (h.responseCode != 200) throw Exception("O download falhou (${h.responseCode}).")
            val md = MessageDigest.getInstance("SHA-256")
            h.inputStream.use { entrada ->
                apk.outputStream().use { saida ->
                    val b = ByteArray(64 * 1024)
                    while (true) {
                        val n = entrada.read(b)
                        if (n <= 0) break
                        saida.write(b, 0, n)
                        md.update(b, 0, n)
                    }
                }
            }
            val hex = md.digest().joinToString("") { String.format("%02x", it) }
            if (v.sha256.isNotEmpty() && v.sha256 != hex) {
                apk.delete()
                throw Exception("O arquivo baixado não confere com o publicado. Nada foi instalado.")
            }
        } finally { h.disconnect() }
        return apk
    }

    fun podeInstalar(c: Context): Boolean = Build.VERSION.SDK_INT < 26 || c.packageManager.canRequestPackageInstalls()

    /** Entrega o APK ao instalador do Android. No Android 12+, depois da primeira vez, a atualização pode sair sem pedir confirmação. */
    fun instalar(c: Context, apk: File) {
        val instalador = c.packageManager.packageInstaller
        val p = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        p.setAppPackageName(c.packageName)
        if (Build.VERSION.SDK_INT >= 31) p.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        val id = instalador.createSession(p)
        instalador.openSession(id).use { s ->
            apk.inputStream().use { entrada ->
                s.openWrite("velox.apk", 0, apk.length()).use { saida ->
                    entrada.copyTo(saida)
                    s.fsync(saida)
                }
            }
            val retorno = Intent(c, InstalacaoReceiver::class.java).setPackage(c.packageName)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
            s.commit(PendingIntent.getBroadcast(c, id, retorno, flags).intentSender)
        }
    }
}

/** Recebe o aviso do instalador: se o Android precisar da confirmação da pessoa, abre a tela de instalação. */
class InstalacaoReceiver : BroadcastReceiver() {
    @Suppress("DEPRECATION")
    override fun onReceive(c: Context, i: Intent) {
        if (i.getIntExtra(PackageInstaller.EXTRA_STATUS, -1) != PackageInstaller.STATUS_PENDING_USER_ACTION) return
        val confirmar: Intent? = if (Build.VERSION.SDK_INT >= 33) i.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java) else i.getParcelableExtra(Intent.EXTRA_INTENT)
        if (confirmar != null) {
            confirmar.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try { c.startActivity(confirmar) } catch (e: Exception) { /* app em segundo plano: tenta de novo na próxima abertura */ }
        }
    }
}

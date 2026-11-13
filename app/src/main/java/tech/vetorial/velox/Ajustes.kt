package tech.vetorial.velox

import android.content.Context
import android.content.SharedPreferences

/** Preferências guardadas no aparelho. */
class Ajustes(contexto: Context) {
    private val p: SharedPreferences = contexto.getSharedPreferences("velox", Context.MODE_PRIVATE)

    /** Texto do tema ("c1=..&c2=.."). Quem usava os temas antigos (neon/sol/cromado) volta ao padrão. */
    var temaTexto: String
        get() = p.getString("tema_texto", "") ?: ""
        set(v) { p.edit().putString("tema_texto", v).apply() }

    var buscador: String
        get() = p.getString("buscador", "google") ?: "google"
        set(v) { p.edit().putString("buscador", v).apply() }

    var bloquearAnuncios: Boolean
        get() = p.getBoolean("bloquear", true)
        set(v) { p.edit().putBoolean("bloquear", v).apply() }

    var siteParaComputador: Boolean
        get() = p.getBoolean("computador", false)
        set(v) { p.edit().putBoolean("computador", v).apply() }

    var sitesEscuros: Boolean
        get() = p.getBoolean("escuros", false)
        set(v) { p.edit().putBoolean("escuros", v).apply() }

    var imersivo: Boolean
        get() = p.getBoolean("imersivo", false)
        set(v) { p.edit().putBoolean("imersivo", v).apply() }

    var telaLigada: Boolean
        get() = p.getBoolean("tela_ligada", false)
        set(v) { p.edit().putBoolean("tela_ligada", v).apply() }

    /** Quantos anúncios e rastreadores o Velox já barrou desde que foi instalado. */
    var bloqueadosTotal: Long
        get() = p.getLong("bloqueados_total", 0L)
        set(v) { p.edit().putLong("bloqueados_total", v).apply() }

    /** Endereços das abas abertas (sem as anônimas), separados por quebra de linha. */
    var abas: List<String>
        get() = (p.getString("abas", "") ?: "").split('\n').filter { it.isNotBlank() }
        set(v) { p.edit().putString("abas", v.joinToString("\n")).apply() }

    var abaAtual: Int
        get() = p.getInt("aba_atual", 0)
        set(v) { p.edit().putInt("aba_atual", v).apply() }
}

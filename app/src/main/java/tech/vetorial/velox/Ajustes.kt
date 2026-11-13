package tech.vetorial.velox

import android.content.Context
import android.content.SharedPreferences

/** Preferências guardadas no aparelho. */
class Ajustes(contexto: Context) {
    private val p: SharedPreferences = contexto.getSharedPreferences("velox", Context.MODE_PRIVATE)

    var tema: String
        get() = p.getString("tema", "neon") ?: "neon"
        set(v) { p.edit().putString("tema", v).apply() }

    var buscador: String
        get() = p.getString("buscador", "google") ?: "google"
        set(v) { p.edit().putString("buscador", v).apply() }

    var bloquearAnuncios: Boolean
        get() = p.getBoolean("bloquear", true)
        set(v) { p.edit().putBoolean("bloquear", v).apply() }

    var siteParaComputador: Boolean
        get() = p.getBoolean("computador", false)
        set(v) { p.edit().putBoolean("computador", v).apply() }

    /** Endereços das abas abertas (sem as anônimas), separados por quebra de linha. */
    var abas: List<String>
        get() = (p.getString("abas", "") ?: "").split('\n').filter { it.isNotBlank() }
        set(v) { p.edit().putString("abas", v.joinToString("\n")).apply() }

    var abaAtual: Int
        get() = p.getInt("aba_atual", 0)
        set(v) { p.edit().putInt("aba_atual", v).apply() }
}

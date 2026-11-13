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

    /** Páginas ficam transparentes e mostram o fundo do Velox (ligado por padrão). */
    var sitesNoTema: Boolean
        get() = p.getBoolean("sites_no_tema", true)
        set(v) { p.edit().putBoolean("sites_no_tema", v).apply() }

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

    /** Quando o Velox procurou atualização pela última vez (milissegundos). */
    var ultimaConferencia: Long
        get() = p.getLong("ultima_conferencia", 0L)
        set(v) { p.edit().putLong("ultima_conferencia", v).apply() }

    /** Endereços das abas abertas (sem as anônimas), separados por quebra de linha. */
    var abas: List<String>
        get() = (p.getString("abas", "") ?: "").split('\n').filter { it.isNotBlank() }
        set(v) { p.edit().putString("abas", v.joinToString("\n")).apply() }

    /** Favoritos: lista JSON de {t: título, u: endereço}. */
    var favoritosJson: String
        get() = p.getString("favoritos", "[]") ?: "[]"
        set(v) { p.edit().putString("favoritos", v).apply() }

    /** Já perguntou na primeira abertura se o Velox deve ser o navegador principal. */
    var perguntouPadrao: Boolean
        get() = p.getBoolean("perguntou_padrao", false)
        set(v) { p.edit().putBoolean("perguntou_padrao", v).apply() }

    var abaAtual: Int
        get() = p.getInt("aba_atual", 0)
        set(v) { p.edit().putInt("aba_atual", v).apply() }
}

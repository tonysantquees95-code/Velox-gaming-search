package tech.vetorial.velox

/**
 * Aparência do navegador. O painel (HTML) monta o tema completo e o app só guarda o texto ("c1=22d3ee&c2=a78bfa&bg=...")
 * e aplica nas barras; a página inicial e o painel leem o mesmo texto.
 */
class Tema private constructor(private val campos: Map<String, String>) {
    val fundo: Int = cor("bg", 0x070B14)
    val superficie: Int = cor("sup", 0x0D1530)
    val campo: Int = cor("campo", 0x141E40)
    val destaque: Int = cor("c1", 0x22D3EE)
    val secundaria: Int = cor("c2", 0xA78BFA)
    val texto: Int = cor("tx", 0xFFFFFF)
    val suave: Int = cor("su", 0x8FA0CF)
    val brilho: Int = (campos["br"]?.toIntOrNull() ?: 70).coerceIn(0, 100)
    val formato: String = if (campos["fo"] == "redondo") "redondo" else "futurista"
    val animacoes: Boolean = campos["an"] != "0"
    /** Estilo dos atalhos da página inicial: marca, neon ou mono. */
    val atalhos: String = campos["ti"]?.takeIf { it in ESTILOS } ?: "marca"
    /** Pacote de ícones da barra de baixo: linha, fino, grossa, duo, aurora, cor, neon, reto ou sombra. */
    val pacote: String = campos["pk"]?.takeIf { it in PACOTES } ?: "linha"
    /** Quanto escurecer a foto de fundo (0 a 80). */
    val escurecer: Int = (campos["dim"]?.toIntOrNull() ?: 35).coerceIn(0, 80)
    /** Transparência das barras, caixas e ícones (0 = firme, 100 = bem transparente). */
    val transparencia: Int = (campos["tr"]?.toIntOrNull() ?: 30).coerceIn(0, 100)
    val claro: Boolean = false
    val nome: String get() = "Velox"

    val redondo: Boolean get() = formato == "redondo"

    /** Cor de texto legível sobre o destaque. */
    val sobreDestaque: Int get() = if (luminosidade(destaque) > 150) 0xFF06090F.toInt() else 0xFFFFFFFF.toInt()

    private fun cor(chave: String, padrao: Int): Int {
        val v = campos[chave]
        val n = if (v != null && HEX.matches(v)) v.toInt(16) else padrao
        return 0xFF000000.toInt() or n
    }

    /** O texto que as páginas do Velox recebem no endereço (só chaves e valores conhecidos). */
    fun params(): String = CHAVES.joinToString("&") { it + "=" + (campos[it] ?: PADRAO[it]) }

    companion object {
        private val HEX = Regex("[0-9a-fA-F]{6}")
        private val CHAVES = listOf("c1", "c2", "bg", "sup", "campo", "su", "tx", "br", "fu", "fo", "an", "base", "ti", "pk", "dim", "tr")
        private val PADRAO = mapOf("c1" to "22d3ee", "c2" to "a78bfa", "bg" to "070b14", "sup" to "0d1530", "campo" to "141e40", "su" to "8fa0cf", "tx" to "ffffff",
            "br" to "70", "fu" to "aurora", "fo" to "futurista", "an" to "1", "base" to "noite", "ti" to "marca", "pk" to "linha", "dim" to "35", "tr" to "30")
        private val FUNDOS = setOf("aurora", "grade", "circuito", "pontos", "liso")
        private val BASES = setOf("noite", "carvao", "vinho", "floresta")
        private val ESTILOS = setOf("marca", "neon", "mono")
        private val PACOTES = setOf("linha", "fino", "grossa", "duo", "aurora", "cor", "neon", "reto", "sombra")

        /** Lê o texto guardado, corrigindo qualquer valor estranho (nunca quebra). */
        fun daString(texto: String?): Tema {
            val m = HashMap<String, String>()
            for (par in (texto ?: "").split('&')) {
                val i = par.indexOf('=')
                if (i <= 0) continue
                val k = par.substring(0, i)
                val v = par.substring(i + 1)
                if (k !in CHAVES) continue
                val bom = when (k) {
                    "c1", "c2", "bg", "sup", "campo", "su", "tx" -> HEX.matches(v)
                    "br" -> v.toIntOrNull() != null
                    "fu" -> v in FUNDOS
                    "fo" -> v == "futurista" || v == "redondo"
                    "an" -> v == "0" || v == "1"
                    "base" -> v in BASES
                    "ti" -> v in ESTILOS
                    "pk" -> v in PACOTES
                    "dim" -> v.toIntOrNull() != null
                    "tr" -> v.toIntOrNull() != null
                    else -> false
                }
                if (bom) m[k] = v
            }
            return Tema(m)
        }

        val PADRAO_TEMA: Tema get() = daString("")

        fun luminosidade(cor: Int): Int {
            val r = (cor shr 16) and 255
            val g = (cor shr 8) and 255
            val b = cor and 255
            return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }
    }
}

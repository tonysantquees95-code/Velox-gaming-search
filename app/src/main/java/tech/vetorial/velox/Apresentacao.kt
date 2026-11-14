package tech.vetorial.velox

/**
 * Primeira abertura (tour → permissões explicadas → termos) e "O que há de novo".
 * REGRA: toda mudança que altere o que o app faz ou como se usa ganha uma linha em NOVIDADES, com id = o maior id + 1.
 * A tela mostra só o que a pessoa ainda não viu, então não depende do número da versão.
 */
object Apresentacao {
    /** Mudou o texto dos termos: suba a versão e a pessoa aceita de novo. */
    const val VERSAO_DOS_TERMOS = 1

    data class Passo(val icone: String, val titulo: String, val texto: String)
    data class Novidade(val id: Int, val data: String, val tipo: String, val titulo: String, val texto: String)

    val TOUR = listOf(
        Passo("🚀", "Bem-vindo ao Velox", "O navegador gamer da Vetorial Tech: rápido, com bloqueio de anúncios e feito para quem joga.\n\nDeslize pelas telas para conhecer as funções. Você pode rever tudo em Ajustes › Sobre."),
        Passo("🛡️", "Bloqueio de anúncios", "O Velox barra anúncios e rastreadores sozinho e mostra quantos bloqueou na aba. Dá para desligar em Ajustes."),
        Passo("🗂️", "Abas, anônima e favoritos", "Abra várias abas, use a aba anônima quando quiser e guarde seus sites favoritos para voltar rápido."),
        Passo("🔐", "Cofre de senhas", "O Velox oferece guardar logins e preenche para você. O cofre fica protegido no aparelho e só sincroniza com a sua conta se você ligar."),
        Passo("🎮", "Game Bar e janelinha", "A barra de jogo dá atalhos rápidos, e a janelinha mantém o vídeo rodando por cima de outros apps."),
        Passo("☁️", "Sincronização", "Entrando com o Vetorial ID você leva favoritos, tema e (se quiser) senhas para os seus outros aparelhos."),
        Passo("🔄", "Sempre atualizado", "O Velox procura versão nova sozinho ao abrir e, depois de atualizar, mostra o que mudou.")
    )

    /** Texto das permissões, mostrado antes de qualquer pedido do Android. */
    const val PERMISSOES = "O Velox pede o mínimo, e só na hora em que você usa a função:\n\n" +
        "🔔 Notificações — só se você ligar o modo segundo plano, para mostrar o aviso de que o Velox continua rodando.\n\n" +
        "🎤 Microfone e 📷 câmera — só quando um site que você abriu pedir (videochamada, por exemplo). Antes, o Velox pergunta se você permite aquele site.\n\n" +
        "📍 Localização — só se um site pedir, e sempre com a sua autorização.\n\n" +
        "📦 Instalar atualizações — para instalar uma versão nova do Velox, o Android pede a sua confirmação.\n\n" +
        "Você pode mudar o que cada site pode usar nas configurações do Velox."

    val TERMOS = listOf(
        "1. O que é o Velox" to "O Velox é um navegador da Vetorial Tech. Nesta fase ele está em testes (beta): pode ter erros e mudar sem aviso.",
        "2. Navegação e sites" to "Os sites que você abre são de terceiros, com regras próprias. A Vetorial Tech não controla o conteúdo deles. O bloqueio de anúncios reduz, mas não elimina, rastreadores e riscos.",
        "3. Privacidade" to "O histórico, os favoritos e o cofre ficam no seu aparelho. Só são enviados à sua conta Vetorial ID se você ligar a sincronização, e apenas o que você escolher sincronizar. A aba anônima não grava histórico.",
        "4. Senhas" to "O cofre guarda logins protegidos neste aparelho. Se você esquecer a senha do cofre, a Vetorial Tech não consegue recuperá-los.",
        "5. Permissões dos sites" to "Câmera, microfone e localização só são liberados para um site quando você permite. Você pode revogar quando quiser.",
        "6. Garantias" to "O Velox é oferecido como está, sem garantia de funcionamento contínuo. A Vetorial Tech não se responsabiliza por perdas decorrentes do uso nem por conteúdo de sites.",
        "7. Mudanças" to "Estes termos podem mudar. Quando mudarem, o Velox pede que você aceite de novo antes de continuar."
    )

    val NOVIDADES = listOf(
        Novidade(1, "2026-11-14", "novo", "Apresentação e novidades", "Na primeira abertura o Velox mostra as funções, explica as permissões e pede o aceite dos termos. Depois de cada atualização, esta tela conta o que mudou.")
    )

    fun rotulo(tipo: String): String = when (tipo) { "novo" -> "Novo"; "melhoria" -> "Melhorou"; "correcao" -> "Corrigido"; else -> tipo }
    fun ultimoId(lista: List<Novidade> = NOVIDADES): Int = lista.maxOfOrNull { it.id } ?: 0
    /** O que a pessoa ainda não viu, da mais nova para a mais antiga. */
    fun novasDesde(visto: Int, lista: List<Novidade> = NOVIDADES): List<Novidade> = lista.filter { it.id > visto }.sortedByDescending { it.id }
    /** Precisa mostrar a apresentação (tour + permissões + termos)? */
    fun precisaApresentar(termosAceitos: Int, tourVisto: Boolean): Boolean = termosAceitos != VERSAO_DOS_TERMOS || !tourVisto
}

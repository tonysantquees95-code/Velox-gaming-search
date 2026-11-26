package tech.vetorial.velox

/**
 * Primeira abertura (tour → permissões explicadas → termos) e "O que há de novo".
 * REGRA: toda mudança que altere o que o app faz ou como se usa ganha uma linha em NOVIDADES, com id = o maior id + 1.
 * A tela mostra só o que a pessoa ainda não viu, então não depende do número da versão.
 */
object Apresentacao {
    /** Mudou o texto dos termos: suba a versão e a pessoa aceita de novo. */
    const val VERSAO_DOS_TERMOS = 3

    data class Passo(val icone: String, val titulo: String, val texto: String)
    data class Novidade(val id: Int, val data: String, val tipo: String, val titulo: String, val texto: String)

    val TOUR = listOf(
        Passo("🚀", "Bem-vindo ao Velox", "O navegador da Vetorial Tech: rápido, com bloqueio de anúncios, em duas edições (Gaming e Pro).\n\nDeslize pelas telas para conhecer as funções. Você pode rever tudo em Ajustes › Sobre."),
        Passo("🛡️", "Bloqueio de anúncios", "O Velox barra anúncios e rastreadores sozinho e mostra quantos bloqueou na aba. Dá para desligar em Ajustes."),
        Passo("🗂️", "Abas, anônima e favoritos", "Abra várias abas, use a aba anônima quando quiser e guarde seus sites favoritos para voltar rápido. E, em Ajustes, “Colocar na tela inicial” transforma a página aberta num ícone, como um aplicativo."),
        Passo("🔐", "Cofre de senhas", "O Velox oferece guardar logins e preenche para você. O cofre fica protegido no aparelho e só sincroniza com a sua conta se você ligar."),
        Passo("🌐", "Enviar aba, traduzir e abas dormindo", "Envie a página que você está vendo para o outro aparelho, traduza para o português com um toque e deixe o Velox pôr para dormir as abas paradas, para poupar memória."),
        Passo("📄", "PDF direto no Vindex", "Baixou um PDF? Na lista de downloads, toque em “Abrir no Vindex” para ler, anotar e preencher."),
        Passo("🎮", "Game Bar e janelinha", "A barra de jogo dá atalhos rápidos, e a janelinha mantém o vídeo rodando por cima de outros apps."),
        Passo("☁️", "Sincronização", "O Velox é grátis e pede o seu Vetorial ID (você cria na hora, se não tiver). Com ele, favoritos, tema e (se quiser) senhas vão com você para os seus outros aparelhos."),
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
        "1. O que é o Velox" to "O Velox é um navegador gratuito da Vetorial Tech. Para usar, é preciso entrar com o seu Vetorial ID (a conta única da Vetorial Tech, grátis e criada na hora). Nesta fase ele está em testes (beta): pode ter erros e mudar sem aviso.",
        "2. Navegação e sites" to "Os sites que você abre são de terceiros, com regras próprias. A Vetorial Tech não controla o conteúdo deles. O bloqueio de anúncios reduz, mas não elimina, rastreadores e riscos.",
        "3. Privacidade" to "O histórico, os favoritos e o cofre ficam no seu aparelho. Só são enviados à sua conta Vetorial ID se você ligar a sincronização, e apenas o que você escolher sincronizar. A aba anônima não grava histórico. Se você enviar uma aba para o outro aparelho, o endereço e o título dela ficam guardados na sua conta por até 7 dias. Se tocar em “Traduzir página”, o endereço da página é enviado ao Google Tradutor.",
        "4. Senhas" to "O cofre guarda logins protegidos neste aparelho. Se você esquecer a senha do cofre, a Vetorial Tech não consegue recuperá-los.",
        "5. Permissões dos sites" to "Câmera, microfone e localização só são liberados para um site quando você permite. Você pode revogar quando quiser.",
        "6. Garantias" to "O Velox é oferecido como está, sem garantia de funcionamento contínuo. A Vetorial Tech não se responsabiliza por perdas decorrentes do uso nem por conteúdo de sites.",
        "7. Mudanças" to "Estes termos podem mudar. Quando mudarem, o Velox pede que você aceite de novo antes de continuar."
    )

    val NOVIDADES = listOf(
        Novidade(1, "2026-11-14", "novo", "Apresentação e novidades", "Na primeira abertura o Velox mostra as funções, explica as permissões e pede o aceite dos termos. Depois de cada atualização, esta tela conta o que mudou."),
        Novidade(2, "2026-11-14", "correcao", "Janelinha de vídeo", "Ao sair do YouTube com o vídeo rodando, a janelinha agora mostra só o vídeo, sem as abas, o endereço e a barra de jogo do Velox. E, se o modo segundo plano estiver desligado, o Velox pergunta se você quer ativar para a janelinha funcionar."),
        Novidade(3, "2026-11-14", "melhoria", "Caixas no visual do Velox", "Os avisos e perguntas do Velox agora seguem o visual do app: cantos recortados, as cores e o brilho do seu tema, em vez do cinza padrão do Android."),
        Novidade(4, "2026-11-14", "melhoria", "Engrenagem nos Ajustes", "A engrenagem de Ajustes agora é uma engrenagem de verdade, não mais parecida com um sol."),
        Novidade(5, "2026-11-15", "melhoria", "Seletores no visual do Velox", "As caixas de marcar dos Ajustes agora usam as cores e o formato do tema, no lugar do azul padrão do sistema."),
        Novidade(6, "2026-11-15", "novo", "Luz do logo com a cor que você quiser", "No painel, em Visual, você escolhe a luz que corre em volta das letras do Velox: a padrão, RGB (muda de cor), branca ou uma cor de sua escolha. Vale no celular e no computador."),
        Novidade(7, "2026-11-15", "novo", "Renomear favoritos e ícone do site", "Toque no lápis (✎) de um favorito no painel, ou segure o favorito na faixa, para trocar o nome e deixar curtinho. Cada favorito também mostra o ícone do site na frente do nome."),
        Novidade(8, "2026-11-15", "melhoria", "Atualização com barra de download", "Ao achar uma versão nova, o Velox avisa e pergunta se você quer atualizar. Depois mostra a barra do download e, no fim, abre a tela do Android para você confirmar a instalação. É o mesmo jeito da Vênus."),
        Novidade(9, "2026-11-16", "correcao", "Janela de salvar senha sem botão cortado", "No aviso “Salvar senha?”, o botão Salvar aparecia cortado. Agora os botões ficam bem encaixados e dá para ler e tocar em todos."),
        Novidade(10, "2026-11-16", "correcao", "Rolar para cima não recarrega mais a página", "Em sites que rolam por dentro (como a Vênus na web), rolar a lista para cima recarregava a página sem querer. Agora só recarrega se você puxar a partir do topo, e é preciso puxar um pouco mais."),
        Novidade(11, "2026-11-17", "melhoria", "Barra de baixo transparente e engrenagem nos ajustes", "A barra do endereço e dos botões agora fica transparente, como no Windows: forma uma barra só e a foto de fundo que você escolheu aparece por trás. E o botão de ajustes agora é uma engrenagem, no lugar do solzinho."),
        Novidade(12, "2026-11-17", "correcao", "Rolar para o lado não recarrega mais a página", "Ao arrastar para o lado as opções da tela inicial (e outras listas que rolam para o lado), aparecia a bolinha de atualizar como se você quisesse recarregar. Agora arrastar para o lado só rola a lista; recarregar só acontece quando você puxa de cima para baixo."),
        Novidade(13, "2026-11-17", "novo", "Vetorial ID obrigatório", "O Velox continua 100% grátis, mas agora pede o seu Vetorial ID ao abrir. Se ainda não tem, crie na hora, na própria janelinha. A conta é uma só para todos os apps da Vetorial Tech."),
        Novidade(14, "2026-11-17", "novo", "Recuperar a senha sem e-mail", "Ao criar o Vetorial ID você define uma senha de recuperação de 6 números e uma pergunta secreta. Se esquecer a senha, toque em Esqueci a senha na tela de entrada, escolha um dos dois jeitos e crie a senha nova."),
        Novidade(15, "2026-11-20", "novo", "Quem abriu o link", "Quando outro app abre uma página no Velox sozinho, o Velox agora avisa qual app mandou o link. Assim você descobre de onde veio e pode tirar a permissão ou desinstalar esse app."),
        Novidade(16, "2026-11-20", "novo", "Página como aplicativo na tela inicial", "Em Ajustes (ou no painel de favoritos), toque em “Colocar na tela inicial”. Você escolhe o nome e o Velox cria um ícone com a logo do site na tela inicial do celular. Ao tocar, a página abre direto no Velox."),
        Novidade(17, "2026-11-21", "novo", "Feedback e relato de erros", "Em Ajustes › Sobre o Velox há agora Enviar feedback e Reportar um erro: conte o que achou, avise quando algo não funcionou e responda uma pesquisa rápida de 1 minuto. O Velox está em fase de testes (beta) e cada relato ajuda a melhorar."),
        Novidade(18, "2026-11-24", "novo", "Velox Gaming e Velox Pro", "O Velox agora tem duas edições no mesmo app. Gaming é o visual de jogo, com neon, luz no logo e fundo animado. Pro é limpo e direto, sem brilho nem animações, ideal para o trabalho. Quem instala escolhe na primeira abertura, e todos podem trocar quando quiser em Painel › Visual › Edição do Velox. A troca aplica as cores e o estilo da edição, e você ainda pode ajustar o que quiser depois."),
        Novidade(19, "2026-11-25", "melhoria", "Bloqueio de anúncios mais forte", "A lista de anunciantes e rastreadores ficou bem maior, e o Velox agora também esconde banners de anúncio e avisos de cookies que escapavam. Continua em Ajustes › Bloquear anúncios, e dá para desligar."),
        Novidade(20, "2026-11-25", "novo", "Abas dormindo", "A aba que ficou parada em segundo plano por 15 minutos é esvaziada para poupar memória e volta sozinha, no mesmo endereço, quando você abre. Nunca dorme a aba que está na tela, a anônima, a que carrega ou quando há som tocando. Dá para desligar no painel."),
        Novidade(21, "2026-11-25", "novo", "Enviar aba para o outro aparelho", "No painel, em Abas ou em Nesta página, toque em “Enviar para o outro aparelho”. A página aparece no Velox do computador (ou do celular) em instantes, em “Enviadas para você”. Precisa estar com o Vetorial ID entrado nos dois."),
        Novidade(22, "2026-11-25", "novo", "Abrir PDF no Vindex", "Na lista de downloads, todo PDF baixado ganha o botão “Abrir no Vindex” (se o Vindex estiver instalado) para ler, anotar e preencher formulários."),
        Novidade(23, "2026-11-25", "novo", "Traduzir página", "No painel, em Nesta página, toque em “Traduzir página” para ver o site em português. Toque de novo, agora em “Ver original”, para voltar. O endereço da página é enviado ao Google Tradutor só quando você pede.")
        Novidade(24, "2026-11-26", "correcao", "Ícones de volta", "Depois da última atualização, os ícones do painel podiam não aparecer. Já voltaram ao normal."),
    )

    fun rotulo(tipo: String): String = when (tipo) { "novo" -> "Novo"; "melhoria" -> "Melhorou"; "correcao" -> "Corrigido"; else -> tipo }
    fun ultimoId(lista: List<Novidade> = NOVIDADES): Int = lista.maxOfOrNull { it.id } ?: 0
    /** O que a pessoa ainda não viu, da mais nova para a mais antiga. */
    fun novasDesde(visto: Int, lista: List<Novidade> = NOVIDADES): List<Novidade> = lista.filter { it.id > visto }.sortedByDescending { it.id }
    /** Precisa mostrar a apresentação (tour + permissões + termos)? */
    fun precisaApresentar(termosAceitos: Int, tourVisto: Boolean): Boolean = termosAceitos != VERSAO_DOS_TERMOS || !tourVisto
}

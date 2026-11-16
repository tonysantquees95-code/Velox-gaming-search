// Primeira abertura (tour → permissões explicadas → termos) e "O que há de novo".
// REGRA: toda mudança que altere o que o app faz ou como se usa ganha uma linha em NOVIDADES, com id = o maior id + 1.
// A tela mostra só o que a pessoa ainda não viu, então não depende do número da versão.
const VERSAO_DOS_TERMOS = 1;

const TOUR = [
  { icone: "🚀", titulo: "Bem-vindo ao Velox", texto: "O navegador gamer da Vetorial Tech: rápido, com bloqueio de anúncios e feito para quem joga.\n\nPasse pelas telas para conhecer as funções. Você pode rever tudo em Ajustes › Sobre o Velox." },
  { icone: "🛡️", titulo: "Bloqueio de anúncios", texto: "O Velox barra anúncios e rastreadores sozinho e mostra quantos bloqueou. Dá para desligar em Ajustes." },
  { icone: "🗂️", titulo: "Abas, anônima e favoritos", texto: "A setinha ao lado da primeira aba liga as abas verticais e organiza as guias; o menu dos três pontinhos tem tela dividida e captura de tela. Abra várias abas (Ctrl+T), use a aba anônima quando quiser e guarde seus sites favoritos na barra para voltar rápido." },
  { icone: "🔐", titulo: "Cofre de senhas", texto: "O Velox oferece guardar logins e preenche para você. O cofre fica protegido neste computador (senha mestra) e só sincroniza se você ligar." },
  { icone: "🎮", titulo: "Modo gamer", texto: "O modo gamer deixa uma barra de atalhos discreta enquanto você joga no navegador, com medidor de desempenho." },
  { icone: "☁️", titulo: "Sincronização", texto: "Entrando com o Vetorial ID você leva favoritos, tema e (se quiser) senhas para os seus outros aparelhos." },
  { icone: "🔄", titulo: "Sempre atualizado", texto: "O Velox procura versão nova sozinho, pergunta antes de instalar e, depois de atualizar, mostra o que mudou." },
];

const PERMISSOES = "O Velox pede o mínimo, e só na hora em que você usa a função:\n\n" +
  "🎤 Microfone e 📷 câmera — só quando um site que você abriu pedir (videochamada, por exemplo). Antes, o Velox pergunta se você permite aquele site.\n\n" +
  "📍 Localização e 🔔 notificações — só se um site pedir, e sempre com a sua autorização.\n\n" +
  "⬇️ Downloads — os arquivos baixados vão para a pasta Downloads, e a instalação de uma versão nova do Velox sempre pergunta antes.\n\n" +
  "O Windows não exige permissões extras para o Velox. Você pode mudar o que cada site pode usar nos Ajustes.";

const TERMOS = [
  ["1. O que é o Velox", "O Velox é um navegador da Vetorial Tech. Nesta fase ele está em testes (beta): pode ter erros e mudar sem aviso."],
  ["2. Navegação e sites", "Os sites que você abre são de terceiros, com regras próprias. A Vetorial Tech não controla o conteúdo deles. O bloqueio de anúncios reduz, mas não elimina, rastreadores e riscos."],
  ["3. Privacidade", "O histórico, os favoritos e o cofre ficam no seu computador. Só são enviados à sua conta Vetorial ID se você ligar a sincronização, e apenas o que você escolher sincronizar. A aba anônima não grava histórico."],
  ["4. Senhas", "O cofre guarda logins protegidos neste computador. Se você esquecer a senha mestra, a Vetorial Tech não consegue recuperá-los."],
  ["5. Permissões dos sites", "Câmera, microfone e localização só são liberados para um site quando você permite. Você pode revogar quando quiser."],
  ["6. Garantias", "O Velox é oferecido como está, sem garantia de funcionamento contínuo. A Vetorial Tech não se responsabiliza por perdas decorrentes do uso nem por conteúdo de sites."],
  ["7. Mudanças", "Estes termos podem mudar. Quando mudarem, o Velox pede que você aceite de novo antes de continuar."],
];

const NOVIDADES = [
  { id: 1, data: "2026-11-14", tipo: "novo", titulo: "Apresentação e novidades", texto: "Na primeira abertura o Velox mostra as funções, explica as permissões e pede o aceite dos termos. Depois de cada atualização, esta tela conta o que mudou." },
  { id: 2, data: "2026-11-14", tipo: "melhoria", titulo: "Caixas no visual do Velox", texto: "Os avisos e perguntas do Velox (apresentação, atualização, escolha de login) agora abrem na janelinha com o visual do app, e não mais na caixa cinza do Windows." },
  { id: 3, data: "2026-11-14", tipo: "melhoria", titulo: "Engrenagem nos Ajustes", texto: "A engrenagem de Ajustes agora é uma engrenagem de verdade, não mais parecida com um sol." },
  { id: 4, data: "2026-11-15", tipo: "melhoria", titulo: "Seletores no visual do Velox", texto: "As caixas de marcar, as barras deslizantes e as listas de escolha dos Ajustes agora usam as cores e o formato do tema, no lugar do azul e branco padrão do sistema." },
  { id: 5, data: "2026-11-15", tipo: "novo", titulo: "Luz do logo com a cor que você quiser", texto: "Em Ajustes › Tema você escolhe a luz que corre em volta das letras do Velox: a padrão, RGB (muda de cor), branca ou uma cor de sua escolha. Vale no celular e no computador." },
  { id: 6, data: "2026-11-15", tipo: "melhoria", titulo: "Tema igual ao do celular", texto: "Nos Ajustes do Windows agora dá para escolher o estilo dos atalhos da página inicial e o quanto escurecer a foto de fundo, como já era no celular." },
  { id: 7, data: "2026-11-15", tipo: "novo", titulo: "Renomear favoritos e ícone do site", texto: "Clique com o botão direito em um favorito para trocar o nome e deixar curtinho. Agora cada favorito também mostra o ícone do site na frente do nome, para achar mais rápido." },
  { id: 8, data: "2026-11-15", tipo: "novo", titulo: "Downloads em várias partes", texto: "Quando o site aceita, o Velox baixa o arquivo em várias partes ao mesmo tempo, o que ajuda muito em sites que limitam a velocidade de cada conexão. Se o site não aceitar, o download continua normal, sem você precisar fazer nada." },
  { id: 9, data: "2026-11-15", tipo: "correcao", titulo: "Zoom com Ctrl e a roda do mouse", texto: "Segurar o Ctrl e girar a roda do mouse (ou os dois dedos no touchpad) agora aproxima e afasta a página, como nos outros navegadores. O tradutor automático do Chromium também foi desligado." },
  { id: 10, data: "2026-11-15", tipo: "melhoria", titulo: "Abrir arquivo e abrir pasta nos downloads", texto: "Na lista de downloads recentes, cada arquivo pronto agora tem os botões Abrir e Abrir pasta, para você escolher entre abrir na hora ou só ver onde ele ficou. A caixinha de renomear favorito também foi arrumada." },
  { id: 11, data: "2026-11-15", tipo: "correcao", titulo: "Sem tela branca ao abrir um site", texto: "Ao abrir um favorito ou fazer uma pesquisa, a tela de antes continua até o site novo estar pronto, sem piscar branco. Enquanto carrega, uma barrinha aparece embaixo da aba, nas cores do tema." },
  { id: 12, data: "2026-11-15", tipo: "novo", titulo: "Abas verticais e ações de abas", texto: "A setinha ao lado da primeira aba abre um menu para ligar as abas verticais (a lista de abas fica na lateral), organizar as guias por site, pesquisar entre as abas abertas e reabrir as fechadas. Atalho: Ctrl+Shift+A." },
  { id: 13, data: "2026-11-15", tipo: "novo", titulo: "Tela dividida e captura de tela", texto: "No menu dos três pontinhos agora tem Tela dividida (duas páginas lado a lado) e Captura de tela (também com Ctrl+Shift+S): a imagem é copiada e guardada na pasta Imagens\\Velox." },
  { id: 14, data: "2026-11-15", tipo: "melhoria", titulo: "Ajustes em página com menu lateral", texto: "Os ajustes agora têm um menu à esquerda com as seções (Privacidade, Tema, Senhas e outras) e um campo para pesquisar, como nas configurações de um navegador moderno." },
  { id: 15, data: "2026-11-15", tipo: "melhoria", titulo: "Atualização com barra de download", texto: "Ao achar uma versão nova, o Velox avisa, você escolhe baixar e acompanha a barra de download em Ajustes › Atualizações. Quando terminar, é você quem escolhe a hora de instalar. É o mesmo jeito da Vênus." },
  { id: 16, data: "2026-11-15", tipo: "correcao", titulo: "Barra de carregamento embaixo do nome da aba", texto: "A barrinha de carregamento das abas agora corre por baixo do nome, sem ficar na mesma linha e sem mexer no visual do nome." },
  { id: 17, data: "2026-11-16", tipo: "correcao", titulo: "Janela de salvar senha sem botão cortado", texto: "No aviso “Salvar senha?”, o botão Salvar aparecia cortado. Agora os botões ficam bem encaixados e dá para ler e tocar em todos." },
];

const ROTULO = { novo: "Novo", melhoria: "Melhorou", correcao: "Corrigido" };
const ultimoId = (lista = NOVIDADES) => lista.reduce((m, n) => Math.max(m, n.id), 0);
/** O que a pessoa ainda não viu, da mais nova para a mais antiga. */
const novasDesde = (visto, lista = NOVIDADES) => lista.filter((n) => n.id > visto).sort((a, b) => b.id - a.id);
/** Precisa mostrar a apresentação (tour + permissões + termos)? */
const precisaApresentar = (a) => (a && a.termosAceitos) !== VERSAO_DOS_TERMOS || !(a && a.tourVisto);
const textoDasNovidades = (itens) => itens.map((n) => `[${ROTULO[n.tipo] || n.tipo}] ${n.titulo}\n${n.texto}`).join("\n\n");
const textoDosTermos = () => TERMOS.map(([t, c]) => `${t}\n${c}`).join("\n\n");

module.exports = { VERSAO_DOS_TERMOS, TOUR, PERMISSOES, TERMOS, NOVIDADES, ROTULO, ultimoId, novasDesde, precisaApresentar, textoDasNovidades, textoDosTermos };

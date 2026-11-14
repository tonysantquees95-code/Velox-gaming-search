// Primeira abertura (tour → permissões explicadas → termos) e "O que há de novo".
// REGRA: toda mudança que altere o que o app faz ou como se usa ganha uma linha em NOVIDADES, com id = o maior id + 1.
// A tela mostra só o que a pessoa ainda não viu, então não depende do número da versão.
const VERSAO_DOS_TERMOS = 1;

const TOUR = [
  { icone: "🚀", titulo: "Bem-vindo ao Velox", texto: "O navegador gamer da Vetorial Tech: rápido, com bloqueio de anúncios e feito para quem joga.\n\nPasse pelas telas para conhecer as funções. Você pode rever tudo em Ajustes › Sobre o Velox." },
  { icone: "🛡️", titulo: "Bloqueio de anúncios", texto: "O Velox barra anúncios e rastreadores sozinho e mostra quantos bloqueou. Dá para desligar em Ajustes." },
  { icone: "🗂️", titulo: "Abas, anônima e favoritos", texto: "Abra várias abas (Ctrl+T), use a aba anônima quando quiser e guarde seus sites favoritos na barra para voltar rápido." },
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

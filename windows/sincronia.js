// Favoritos sincronizados com o Vetorial ID (celular e computador). Mescla por endereço, com marca de apagado.
const SERVIDOR = "https://venus-servidor.tonysantquees95.workers.dev";

/** local/remoto: { favoritos: [{ n, u, t }], apagados: { [url]: t } }. Vence quem mexeu por último em cada endereço. */
function mesclar(local, remoto) {
  const lf = Array.isArray(local && local.favoritos) ? local.favoritos : [];
  const rf = Array.isArray(remoto && remoto.favoritos) ? remoto.favoritos : [];
  const apagados = { ...((remoto && remoto.apagados) || {}), ...((local && local.apagados) || {}) };
  for (const k of Object.keys((remoto && remoto.apagados) || {})) apagados[k] = Math.max(apagados[k] || 0, remoto.apagados[k]);
  for (const k of Object.keys((local && local.apagados) || {})) apagados[k] = Math.max(apagados[k] || 0, local.apagados[k]);
  const porUrl = new Map();
  for (const f of [...rf, ...lf]) {
    if (!f || typeof f.u !== "string" || typeof f.n !== "string") continue;
    const t = Number(f.t) || 0;
    const atual = porUrl.get(f.u);
    if (!atual || t >= (Number(atual.t) || 0)) porUrl.set(f.u, { n: f.n, u: f.u, t });
  }
  const favoritos = [...porUrl.values()].filter((f) => (apagados[f.u] || 0) < (f.t || 0) || !(f.u in apagados)).sort((a, b) => (a.t || 0) - (b.t || 0));
  // apagados antigos (mais de 60 dias) já foram propagados
  const corte = Date.now() - 60 * 86_400_000;
  for (const k of Object.keys(apagados)) if (apagados[k] < corte) delete apagados[k];
  return { favoritos, apagados };
}

const iguais = (a, b) => JSON.stringify(a) === JSON.stringify(b);

async function chamar(token, metodo, caminho, corpo) {
  const r = await fetch(SERVIDOR + caminho, {
    method: metodo,
    headers: { "content-type": "application/json", ...(token ? { authorization: `Bearer ${token}` } : {}) },
    body: corpo ? JSON.stringify(corpo) : undefined,
  });
  const d = await r.json().catch(() => ({}));
  if (!r.ok) { const e = new Error(d.error || `Erro ${r.status}`); e.status = r.status; e.dados = d; throw e; }
  return d;
}

async function entrar(email, senha, aparelho) {
  const d = await chamar("", "POST", "/id/entrar", { email, senha, dispositivo: aparelho, app: "velox" });
  return { token: d.token, email: (d.usuario && d.usuario.email) || email };
}

/** Cria o Vetorial ID na hora (grátis) e já entra. */
async function criarConta(nome, email, senha, aparelho, recuperacao) {
  const d = await chamar("", "POST", "/id/cadastrar", { nome, email, senha, dispositivo: aparelho, app: "velox", recuperacao });
  return { token: d.token, email: (d.usuario && d.usuario.email) || email };
}

/** Esqueci a senha, sem e-mail: devolve a pergunta secreta da conta (erro se não existe ou não tem recuperação). */
async function perguntaSecreta(email) {
  const d = await chamar("", "POST", "/id/recuperar/pergunta", { email });
  if (!d.existe) throw new Error("Não encontramos uma conta com esse e-mail.");
  if (!d.configurada) throw new Error("Essa conta foi criada antes da recuperação por senha de 6 números. Entre com a senha e cadastre a recuperação no site do Vetorial ID.");
  return d.pergunta;
}

/** metodo: "pin" (6 números) ou "pergunta" (resposta secreta). Troca a senha se acertar. */
async function recuperarPorSegredo(email, metodo, valor, novaSenha) {
  await chamar("", "POST", "/id/recuperar/segredo", { email, metodo, valor, novaSenha });
}

/** Baixa o que está no servidor, mescla com o local e devolve { doc, mudouLocal }. Sobe o resultado se for diferente do remoto. */
async function sincronizar(token, local) {
  for (let tentativa = 0; tentativa < 2; tentativa++) {
    const lista = await chamar(token, "GET", "/id/sync");
    const atual = (lista.docs || []).find((d) => d.chave === "velox");
    const remoto = atual ? atual.valor : null;
    const doc = mesclar(local, remoto);
    if (remoto && iguais(doc, { favoritos: remoto.favoritos || [], apagados: remoto.apagados || {} })) return { doc, mudouLocal: !iguais(doc, local) };
    try {
      await chamar(token, "PUT", "/id/sync/velox", { valor: doc, versao: atual ? atual.versao : 0 });
      return { doc, mudouLocal: !iguais(doc, local) };
    } catch (e) {
      if (e.status !== 409) throw e;
    }
  }
  throw new Error("Outro aparelho está mudando os favoritos agora. Tente de novo.");
}

// ---------------------------------------------------------------- tema (um documento por assunto, para o celular e o computador)
/** Vence a mudança mais recente. t = quando a pessoa mexeu no tema pela última vez (milissegundos). */
async function doc(token, chave) {
  const lista = await chamar(token, "GET", "/id/sync");
  return (lista.docs || []).find((d) => d.chave === chave) || null;
}

async function sincronizarTema(token, local) {
  for (let tentativa = 0; tentativa < 2; tentativa++) {
    const atual = await doc(token, "velox-tema");
    const remoto = atual && atual.valor && typeof atual.valor.tema === "string" ? { t: Number(atual.valor.t) || 0, tema: atual.valor.tema } : null;
    if (remoto && remoto.t > (local.t || 0)) return { mudouLocal: true, doc: remoto, acao: "recebido" };
    if (remoto && remoto.t === (local.t || 0)) return { mudouLocal: false, doc: remoto, acao: "igual" };
    if (!(local.t > 0) || !local.tema) return { mudouLocal: false, doc: remoto, acao: "nada" };
    try {
      await chamar(token, "PUT", "/id/sync/velox-tema", { valor: { t: local.t, tema: local.tema }, versao: atual ? atual.versao : 0 });
      return { mudouLocal: false, doc: { t: local.t, tema: local.tema }, acao: "enviado" };
    } catch (e) { if (e.status !== 409) throw e; }
  }
  throw new Error("Outro aparelho está mudando o tema agora. Tente de novo.");
}

// ---------------------------------------------------------------- senhas (cifradas aqui; o servidor só guarda texto embaralhado)
const crypto = require("crypto");
const VOLTAS = 210_000;
const chaveDe = (senha, sal) => crypto.pbkdf2Sync(String(senha), sal, VOLTAS, 32, "sha256");

function cifrarSenhas(senha, doc, sal = crypto.randomBytes(16)) {
  const iv = crypto.randomBytes(12);
  const c = crypto.createCipheriv("aes-256-gcm", chaveDe(senha, sal), iv);
  const dados = Buffer.concat([c.update(JSON.stringify(doc), "utf8"), c.final()]);
  return { v: 1, sal: sal.toString("base64"), dados: Buffer.concat([iv, dados, c.getAuthTag()]).toString("base64") };
}

function decifrarSenhas(senha, pacote) {
  const sal = Buffer.from(pacote.sal, "base64");
  const bruto = Buffer.from(pacote.dados, "base64");
  const d = crypto.createDecipheriv("aes-256-gcm", chaveDe(senha, sal), bruto.subarray(0, 12));
  d.setAuthTag(bruto.subarray(bruto.length - 16));
  const claro = Buffer.concat([d.update(bruto.subarray(12, bruto.length - 16)), d.final()]);
  return JSON.parse(claro.toString("utf8"));
}

const chaveItem = (i) => i.origem + "\n" + i.usuario;

/** local/remoto: { itens: [{ origem, usuario, senha, atualizado }], apagados: { "origem\nusuario": t } }. */
function mesclarSenhas(local, remoto, agora = Date.now()) {
  const lista = (x) => (Array.isArray(x && x.itens) ? x.itens : []).filter((i) => i && typeof i.origem === "string" && typeof i.senha === "string");
  const apagados = {};
  for (const m of [remoto && remoto.apagados, local && local.apagados]) for (const k of Object.keys(m || {})) apagados[k] = Math.max(apagados[k] || 0, Number(m[k]) || 0);
  const por = new Map();
  for (const i of [...lista(remoto), ...lista(local)]) {
    const novo = { origem: i.origem, usuario: i.usuario || "", senha: i.senha, atualizado: Number(i.atualizado) || 0 };
    const k = chaveItem(novo), a = por.get(k);
    if (!a || novo.atualizado >= a.atualizado) por.set(k, novo);
  }
  const itens = [...por.values()].filter((i) => !(chaveItem(i) in apagados) || apagados[chaveItem(i)] < i.atualizado).sort((a, b) => chaveItem(a).localeCompare(chaveItem(b)));
  const corte = agora - 60 * 86_400_000;
  for (const k of Object.keys(apagados)) if (apagados[k] < corte) delete apagados[k];
  return { itens, apagados };
}

const comparavel = (d) => JSON.stringify({ i: (d.itens || []).map((x) => [x.origem, x.usuario || "", x.senha, x.atualizado]).sort(), a: Object.entries(d.apagados || {}).sort() });

/** Devolve { doc, mudouLocal }. Se a senha mestra daqui for diferente da do outro aparelho, lança erro com .codigo = "senha_diferente". */
async function sincronizarSenhas(token, local, senhaMestra) {
  for (let tentativa = 0; tentativa < 2; tentativa++) {
    const atual = await doc(token, "velox-senhas");
    let remoto = null, sal;
    if (atual && atual.valor && atual.valor.dados) {
      try { remoto = decifrarSenhas(senhaMestra, atual.valor); sal = Buffer.from(atual.valor.sal, "base64"); }
      catch { const e = new Error("A senha mestra deste computador é diferente da do outro aparelho. Use a mesma nos dois para sincronizar as senhas."); e.codigo = "senha_diferente"; throw e; }
    }
    const doc2 = mesclarSenhas(local, remoto);
    const mudouLocal = comparavel(doc2) !== comparavel(local);
    if (remoto && comparavel(doc2) === comparavel(remoto)) return { doc: doc2, mudouLocal };
    try {
      await chamar(token, "PUT", "/id/sync/velox-senhas", { valor: cifrarSenhas(senhaMestra, doc2, sal), versao: atual ? atual.versao : 0 });
      return { doc: doc2, mudouLocal };
    } catch (e) { if (e.status !== 409) throw e; }
  }
  throw new Error("Outro aparelho está mudando as senhas agora. Tente de novo.");
}

/** Lista de abas enviadas entre aparelhos (chave velox-abas). Se outro aparelho gravou antes (409), junta e tenta de novo. */
async function lerAbas(token) {
  const lista = await chamar(token, "GET", "/id/sync");
  const d = (lista.docs || []).find((x) => x.chave === "velox-abas");
  return d ? d.valor : { itens: [] };
}
async function mudarAbas(token, mudar, tentativas = 3) {
  for (let i = 0; i < tentativas; i++) {
    const lista = await chamar(token, "GET", "/id/sync");
    const atual = (lista.docs || []).find((x) => x.chave === "velox-abas");
    const novo = mudar(atual ? atual.valor : { itens: [] });
    try { await chamar(token, "PUT", "/id/sync/velox-abas", { valor: novo, versao: atual ? atual.versao : 0 }); return novo; }
    catch (e) { if (e.status !== 409) throw e; }
  }
  throw new Error("Outro aparelho está mexendo nisso agora. Tente de novo.");
}

module.exports = { lerAbas, mudarAbas, mesclar, entrar, criarConta, perguntaSecreta, recuperarPorSegredo, sincronizar, sincronizarTema, sincronizarSenhas, mesclarSenhas, cifrarSenhas, decifrarSenhas, SERVIDOR };

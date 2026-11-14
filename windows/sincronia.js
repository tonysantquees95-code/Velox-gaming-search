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
  const d = await chamar("", "POST", "/id/entrar", { email, senha, dispositivo: aparelho });
  return { token: d.token, email: (d.usuario && d.usuario.email) || email };
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

module.exports = { mesclar, entrar, sincronizar, SERVIDOR };

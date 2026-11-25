// "Enviar aba para o outro aparelho": uma lista pequena guardada no Vetorial ID (chave velox-abas).
// Cada item é visto por aparelho; o aparelho que enviou nunca recebe de volta. Lógica pura e testada.
const SETE_DIAS = 7 * 86_400_000;
const MAXIMO = 20;

/** Só http(s), sem usuário:senha, até 2000 caracteres. Devolve o endereço limpo ou null. */
function urlValida(u) {
  try {
    const x = new URL(String(u || "").trim());
    if (!/^https?:$/.test(x.protocol) || x.username || x.password || !x.hostname.includes(".")) return null;
    const s = x.toString();
    return s.length <= 2000 ? s : null;
  } catch { return null; }
}

function limpar(doc, agora = Date.now()) {
  const itens = (doc && Array.isArray(doc.itens) ? doc.itens : [])
    .filter((i) => i && typeof i.id === "string" && urlValida(i.url) && agora - (Number(i.t) || 0) < SETE_DIAS)
    .map((i) => ({ id: i.id, url: i.url, titulo: String(i.titulo || "").slice(0, 120), de: String(i.de || ""), deNome: String(i.deNome || "").slice(0, 30), t: Number(i.t) || 0, vistoPor: Array.isArray(i.vistoPor) ? i.vistoPor.map(String).slice(0, 10) : [] }));
  return { itens: itens.sort((a, b) => b.t - a.t).slice(0, MAXIMO) };
}

/** Acrescenta uma aba enviada por este aparelho. */
function enviar(doc, { url, titulo, de, deNome }, agora = Date.now()) {
  const u = urlValida(url);
  if (!u || !de) return limpar(doc, agora);
  const base = limpar(doc, agora).itens.filter((i) => !(i.url === u && i.de === de)); // reenviar a mesma página não duplica
  base.unshift({ id: `${agora.toString(36)}-${Math.random().toString(36).slice(2, 7)}`, url: u, titulo: String(titulo || "").slice(0, 120), de, deNome: String(deNome || "").slice(0, 30), t: agora, vistoPor: [de] });
  return limpar({ itens: base }, agora);
}

/** O que este aparelho ainda não viu e que veio de outro. */
function pendentes(doc, meuId, agora = Date.now()) {
  return limpar(doc, agora).itens.filter((i) => i.de !== meuId && !i.vistoPor.includes(meuId));
}

/** Marca como visto neste aparelho. */
function marcarVisto(doc, id, meuId, agora = Date.now()) {
  const d = limpar(doc, agora);
  for (const i of d.itens) if (i.id === id && !i.vistoPor.includes(meuId)) i.vistoPor.push(meuId);
  return d;
}

/** Quando dois aparelhos mexem ao mesmo tempo: junta os itens e os "vistos". */
function mesclar(a, b, agora = Date.now()) {
  const mapa = new Map();
  for (const i of [...limpar(a, agora).itens, ...limpar(b, agora).itens]) {
    const x = mapa.get(i.id);
    mapa.set(i.id, x ? { ...x, vistoPor: [...new Set([...x.vistoPor, ...i.vistoPor])] } : i);
  }
  return limpar({ itens: [...mapa.values()] }, agora);
}

module.exports = { urlValida, limpar, enviar, pendentes, marcarVisto, mesclar, MAXIMO };

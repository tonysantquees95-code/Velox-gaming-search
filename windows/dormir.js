// Abas dormindo: a aba que ficou parada em segundo plano é esvaziada para liberar memória e volta ao endereço de antes quando a pessoa abre.
// Aqui fica só a regra (pura e testada); o main.js é quem esvazia e acorda.
const MINUTOS_PADRAO = 15;

/** Minutos válidos (de 5 a 240). */
function minutosValidos(m) {
  const n = Math.round(Number(m));
  return Number.isFinite(n) ? Math.max(5, Math.min(240, n)) : MINUTOS_PADRAO;
}

/**
 * Quais abas podem dormir agora. Cada aba: { id, interna, anonima, carregando, audivel, dormindo, url, ultimoUso }.
 * Nunca dorme: a que está na tela, a de página interna, a anônima, a que carrega ou toca som, a que já dorme e a que não é http(s).
 */
function escolherParaDormir(abas, agora, { minutos = MINUTOS_PADRAO, naTela = [] } = {}) {
  const limite = minutosValidos(minutos) * 60_000;
  return abas
    .filter((a) => !naTela.includes(a.id) && !a.interna && !a.anonima && !a.carregando && !a.audivel && !a.dormindo
      && /^https?:\/\//i.test(a.url || "") && agora - (Number(a.ultimoUso) || 0) >= limite)
    .map((a) => a.id);
}

/** Total das abas dormindo: quantas são e quantos MB cada uma tinha em uso quando dormiu (medido de verdade, não estimado). */
function resumo(abas) {
  let n = 0, mb = 0;
  for (const a of abas) if (a.dormindo) { n++; mb += Number(a.mbLiberados) || 0; }
  return { dormindo: n, mb: Math.round(mb) };
}

/** "3 abas dormindo · cerca de 420 MB livres" (ou vazio se nenhuma). */
function frase(r) {
  if (!r || !r.dormindo) return "";
  const abas = r.dormindo === 1 ? "1 aba dormindo" : `${r.dormindo} abas dormindo`;
  return r.mb > 0 ? `${abas} · cerca de ${r.mb} MB livres` : abas;
}

module.exports = { MINUTOS_PADRAO, minutosValidos, escolherParaDormir, resumo, frase };

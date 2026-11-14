// Game bar do modo gamer: posições e tamanho da barrinha por cima do jogo (parte pura, testada à parte).
const POSICOES = ["dir", "esq", "cima", "baixo"];
const proxima = (p) => POSICOES[(POSICOES.indexOf(p) + 1) % POSICOES.length] || "dir";
const valida = (p) => (POSICOES.includes(p) ? p : "dir");

/** Retângulo da barra dentro da janela (largura x altura da área de conteúdo). Recolhida vira só uma alça na borda. */
function limitesBarra(pos, recolhida, largura, altura) {
  const p = valida(pos);
  const lado = p === "esq" || p === "dir";
  const M = 12;
  let w = recolhida ? (lado ? 26 : 64) : (lado ? 64 : 470);
  let h = recolhida ? (lado ? 64 : 26) : (lado ? 470 : 64);
  w = Math.max(0, Math.min(w, largura));
  h = Math.max(0, Math.min(h, altura));
  const folga = recolhida ? 0 : M;
  if (p === "dir") return { x: largura - w - folga, y: Math.round((altura - h) / 2), width: w, height: h };
  if (p === "esq") return { x: folga, y: Math.round((altura - h) / 2), width: w, height: h };
  if (p === "cima") return { x: Math.round((largura - w) / 2), y: folga, width: w, height: h };
  return { x: Math.round((largura - w) / 2), y: altura - h - folga, width: w, height: h };
}

module.exports = { POSICOES, proxima, valida, limitesBarra };

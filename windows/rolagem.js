// Barra de rolagem do Velox: fina, no tema do navegador e (por padrão) invisível até o mouse passar por cima.
const MODOS = ["hover", "sempre", "padrao"];
const CORES = { tema: "34, 211, 238", roxo: "167, 139, 250", branco: "255, 255, 255", vermelho: "255, 77, 99" };

function normalizar(r) {
  const x = r && typeof r === "object" ? r : {};
  const largura = Math.max(6, Math.min(16, Math.round(Number(x.largura)) || 8));
  return { modo: MODOS.includes(x.modo) ? x.modo : "hover", largura, cor: CORES[x.cor] ? x.cor : "tema" };
}

function css(r) {
  const { modo, largura, cor } = normalizar(r);
  if (modo === "padrao") return "";
  const c = CORES[cor];
  const base = modo === "hover" ? `rgba(${c}, 0)` : `rgba(${c}, .5)`;
  let t = `::-webkit-scrollbar{width:${largura}px;height:${largura}px;background:transparent}` +
    `::-webkit-scrollbar-track,::-webkit-scrollbar-corner{background:transparent}` +
    `::-webkit-scrollbar-thumb{background:${base};border-radius:999px;border:2px solid transparent;background-clip:padding-box;min-height:36px;min-width:36px}`;
  if (modo === "hover") t += `*:hover::-webkit-scrollbar-thumb{background:rgba(${c}, .5);background-clip:padding-box}`;
  t += `::-webkit-scrollbar-thumb:hover{background:rgba(${c}, .9);background-clip:padding-box}`;
  return t;
}

module.exports = { MODOS, CORES, normalizar, css };

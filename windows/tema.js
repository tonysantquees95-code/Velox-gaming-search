// Tema do Velox: um texto "c1=22d3ee&c2=a78bfa&..." igual ao do celular. Valores estranhos são descartados.
const HEX = /^[0-9a-fA-F]{6}$/;
const CHAVES = ["c1", "c2", "bg", "sup", "campo", "su", "tx", "br", "fu", "fo", "an", "base", "ti", "pk", "dim", "tr", "lz", "lc", "ed"];
const PADRAO = { c1: "22d3ee", c2: "a78bfa", bg: "070b14", sup: "0d1530", campo: "141e40", su: "8fa0cf", tx: "ffffff", br: "70", fu: "aurora", fo: "futurista", an: "1", base: "noite", ti: "marca", pk: "linha", dim: "35", tr: "30", lz: "padrao", lc: "22d3ee", ed: "gaming" };
const em = (lista) => (v) => lista.includes(v);
const VALIDOS = {
  c1: (v) => HEX.test(v), c2: (v) => HEX.test(v), bg: (v) => HEX.test(v), sup: (v) => HEX.test(v), campo: (v) => HEX.test(v), su: (v) => HEX.test(v), tx: (v) => HEX.test(v),
  br: (v) => /^\d{1,3}$/.test(v), dim: (v) => /^\d{1,3}$/.test(v), tr: (v) => /^\d{1,3}$/.test(v),
  ed: em(["gaming", "pro"]), lc: (v) => HEX.test(v), lz: em(["padrao", "rgb", "branca", "cor"]),
  fu: em(["aurora", "grade", "circuito", "pontos", "liso"]), fo: em(["futurista", "redondo"]), an: em(["0", "1"]),
  base: em(["noite", "carvao", "vinho", "floresta", "grafite"]), ti: em(["marca", "neon", "mono"]), pk: em(["linha", "fino", "grossa", "duo", "aurora", "cor", "neon", "reto", "sombra"]),
};

/** Lê o texto e devolve o tema completo (o que faltar ou vier errado fica no padrão). */
function normalizar(texto) {
  const m = { ...PADRAO };
  for (const par of String(texto || "").split("&")) {
    const i = par.indexOf("=");
    if (i <= 0) continue;
    const k = par.slice(0, i), v = par.slice(i + 1);
    if (CHAVES.includes(k) && VALIDOS[k](v)) m[k] = v;
  }
  return CHAVES.map((k) => `${k}=${m[k]}`).join("&");
}

/** Cores e estilo de cada edição. Escolher uma edição aplica tudo isto (depois a pessoa ajusta o que quiser). */
const EDICOES = {
  gaming: { c1: "22d3ee", c2: "a78bfa", bg: "070b14", sup: "0d1530", campo: "141e40", su: "8fa0cf", tx: "ffffff", br: "70", fu: "aurora", fo: "futurista", an: "1", base: "noite", ti: "marca", pk: "linha", tr: "30", lz: "padrao", lc: "22d3ee", ed: "gaming" },
  pro: { c1: "5b9dff", c2: "4a7fd6", bg: "0f1115", sup: "171a21", campo: "20242d", su: "9aa3b2", tx: "ffffff", br: "0", fu: "liso", fo: "redondo", an: "0", base: "grafite", ti: "mono", pk: "fino", tr: "20", lz: "branca", lc: "5b9dff", ed: "pro" },
};

/** Troca de edição: aplica o visual dela por cima do tema atual (a foto de fundo e o brilho de escurecer ficam como estão). */
function aplicarEdicao(texto, ed) {
  const base = doTexto(texto);
  const preset = EDICOES[ed] || EDICOES.gaming;
  return normalizar(Object.entries({ ...base, ...preset }).map(([k, v]) => `${k}=${v}`).join("&"));
}

const doTexto = (texto) => Object.fromEntries(normalizar(texto).split("&").map((p) => p.split("=")));

module.exports = { normalizar, doTexto, aplicarEdicao, EDICOES, PADRAO, CHAVES };

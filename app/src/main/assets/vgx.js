/* Tema e marca VGX do Velox (compartilhado pelas páginas). Lê o tema do endereço (?c1=..&c2=..) e aplica nas variáveis de CSS. */
(function () {
  var P = new URLSearchParams(location.search), R = document.documentElement, S = R.style;
  var hex = function (v) { return /^[0-9a-fA-F]{6}$/.test(v || ""); };
  var rgb = function (h) { var n = parseInt(h, 16); return [(n >> 16) & 255, (n >> 8) & 255, n & 255].join(", "); };
  var cor = { bg: "--bg", sup: "--sup", campo: "--campo", c1: "--neon", c2: "--neon2", tx: "--tx", su: "--su" };
  Object.keys(cor).forEach(function (k) { if (hex(P.get(k))) S.setProperty(cor[k], "#" + P.get(k)); });
  if (hex(P.get("c1"))) { S.setProperty("--neon-rgb", rgb(P.get("c1"))); var n = parseInt(P.get("c1"), 16); S.setProperty("--sobre", (0.299 * (n >> 16) + 0.587 * ((n >> 8) & 255) + 0.114 * (n & 255)) > 150 ? "#06090f" : "#ffffff"); }
  if (hex(P.get("c2"))) S.setProperty("--neon2-rgb", rgb(P.get("c2")));
  var br = parseInt(P.get("br"), 10); if (br >= 0 && br <= 100) S.setProperty("--brilho", String(br / 100));
  var fu = P.get("fu"); if (/^(aurora|grade|circuito|pontos|liso)$/.test(fu || "")) R.dataset.fundo = fu; else R.dataset.fundo = "aurora";
  R.dataset.formato = P.get("fo") === "redondo" ? "redondo" : "futurista";
  R.dataset.anim = P.get("an") === "0" ? "0" : "1";

  /* Monograma VGX com relevo: V, G e X em ângulos, empilhados para dar profundidade. */
  var LETRAS = [
    "M0 0L56 0L100 112L144 0L200 0L128 170L72 170Z",
    "M232 0L392 0L392 42L250 42L250 128L334 128L334 104L296 104L296 70L392 70L392 170L232 170L192 130L192 40Z",
    "M424 0L482 0L524 50.3L566 0L624 0L553 85L624 170L566 170L524 119.7L482 170L424 170L495 85Z"
  ];
  var contaVGX = 0;
  window.desenharVGX = function (alvo, opcoes) {
    var k = ++contaVGX, o = opcoes || {}, camadas = o.camadas == null ? 11 : o.camadas, d = LETRAS.join(" ");
    var uns = "";
    for (var i = camadas; i >= 1; i--) uns += '<use href="#vgx-forma'+k+'" x="' + i + '" y="' + i + '" style="fill:color-mix(in srgb, var(--neon2) 38%, #04060d)"/>';
    alvo.innerHTML = '<svg class="vgx" viewBox="-14 -14 668 210" role="img" aria-label="VGX">' +
      '<defs><path id="vgx-forma'+k+'" d="' + d + '"/>' +
      '<linearGradient id="vgx-g'+k+'" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="640" y2="170"><stop offset="0" style="stop-color:var(--neon)"/><stop offset="1" style="stop-color:var(--neon2)"/></linearGradient>' +
      '<filter id="vgx-b'+k+'" x="-30%" y="-40%" width="160%" height="180%"><feGaussianBlur stdDeviation="10"/></filter></defs>' +
      (o.brilho === false ? "" : '<use href="#vgx-forma'+k+'" style="fill:var(--neon)" filter="url(#vgx-b'+k+')" opacity=".55"/>') + uns +
      '<use href="#vgx-forma'+k+'" fill="url(#vgx-g'+k+')" stroke="rgba(255,255,255,.55)" stroke-width="1.5"/></svg>';
  };
  document.querySelectorAll("[data-vgx]").forEach(function (el) { window.desenharVGX(el, { camadas: +(el.dataset.camadas || 11), brilho: el.dataset.brilho !== "0" }); });
  window.TEMA_PARAMS = P;
})();

/* Cores prontas e bases escuras (a página "painel" monta o tema completo e o app só guarda e aplica). */
window.VGX = {
  CORES: [
    ["neon", "Neon", "22d3ee", "a78bfa"], ["vermelho", "Vermelho", "ff2a3d", "b0001a"], ["laranja", "Laranja", "ff8a1f", "d9300f"], ["dourado", "Dourado", "ffc21a", "ff7a00"],
    ["verde", "Verde", "2dff8a", "06a85a"], ["azul", "Azul", "3d8bff", "1b3dbd"], ["roxo", "Roxo", "a855ff", "5b21b6"], ["rosa", "Rosa", "ff3da6", "9b5cff"]
  ],
  BASES: [
    ["noite", "Noite", "070b14", "0d1530", "141e40", "8fa0cf"], ["carvao", "Carvão", "0a0a0d", "15151a", "1f1f27", "9a9aa8"],
    ["vinho", "Vinho", "12060b", "230c15", "33111f", "c79aa8"], ["floresta", "Floresta", "05110d", "0b2119", "113026", "8fc7b0"]
  ],
  FUNDOS: [["aurora", "Aurora"], ["grade", "Grade"], ["circuito", "Circuito"], ["pontos", "Pontos"], ["liso", "Liso"]],
  mistura: function (hex) { var n = parseInt(hex, 16), r = n >> 16, g = (n >> 8) & 255, b = n & 255, h = function (v) { v = Math.max(0, Math.min(255, Math.round(v))); return (v < 16 ? "0" : "") + v.toString(16); }; return h(r * 0.55 + 10) + h(g * 0.5) + h(b * 0.7 + 20); },
  params: function (t) {
    var b = (this.BASES.filter(function (x) { return x[0] === t.base; })[0]) || this.BASES[0];
    return new URLSearchParams({ c1: t.c1, c2: t.c2, bg: b[2], sup: b[3], campo: b[4], su: b[5], tx: "ffffff", br: String(t.br), fu: t.fu, fo: t.fo, an: t.an ? "1" : "0", base: t.base }).toString();
  },
  doEndereco: function (P) {
    var g = function (k, d) { var v = P.get(k); return v == null ? d : v; };
    return { c1: g("c1", "22d3ee"), c2: g("c2", "a78bfa"), base: g("base", "noite"), br: +g("br", "70"), fu: g("fu", "aurora"), fo: g("fo", "futurista"), an: g("an", "1") !== "0" };
  }
};

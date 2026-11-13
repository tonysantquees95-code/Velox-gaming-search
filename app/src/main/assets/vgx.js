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

  /* Marca VGX: V e G ligados e um X grande, em tubos de neon inclinados. */
  var FORMAS = {
    g: "M262.8 64.4 C251.2 47.9 232.2 38 212 38 C177.8 38 150 65.8 150 100 C150 134.2 177.8 162 212 162 C246.2 162 274 134.2 274 100 L226 100",
    v: "M20 38 L84 160 L156 36", x1: "M312 6 L412 196", x2: "M412 6 L312 196"
  };
  var contaVGX = 0;
  window.desenharVGX = function (alvo, opcoes) {
    var k = ++contaVGX, o = opcoes || {}, gid = "vgx-g" + k;
    var tubo = function (d, w, halo) {
      return (halo ? '<path d="' + d + '" stroke="rgba(3,5,12,.9)" stroke-width="' + (w + halo) + '"/>' : '') +
        '<path d="' + d + '" stroke="url(#' + gid + ')" stroke-width="' + w + '"/><path d="' + d + '" stroke="#fff" stroke-opacity=".34" stroke-width="4"/>';
    };
    var todas = [FORMAS.g, FORMAS.v, FORMAS.x1, FORMAS.x2].map(function (d) { return '<path d="' + d + '"/>'; }).join("");
    alvo.innerHTML = '<svg class="vgx" viewBox="-8 -22 480 246" role="img" aria-label="VGX">' +
      '<defs><linearGradient id="' + gid + '" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="440" y2="200"><stop offset="0" style="stop-color:var(--neon)"/><stop offset="1" style="stop-color:var(--neon2)"/></linearGradient>' +
      '<filter id="vgx-b' + k + '" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="9"/></filter></defs>' +
      '<g transform="translate(14 0) skewX(-11)" fill="none" stroke-linecap="round" stroke-linejoin="round">' +
      (o.brilho === false ? "" : '<g filter="url(#vgx-b' + k + ')" opacity=".6" style="stroke:var(--neon)" stroke-width="28">' + todas + '</g>') +
      tubo(FORMAS.g, 28) + tubo(FORMAS.v, 28, 7) + tubo(FORMAS.x1, 34) + tubo(FORMAS.x2, 34, 7) + '</g></svg>';
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
  ESTILOS: [["marca", "Cores da marca"], ["neon", "Neon do tema"], ["mono", "Branco"]],
  PACOTES: [["linha", "Linha"], ["grossa", "Grossa"], ["duo", "Duplo"]],
  FUNDOS: [["aurora", "Aurora"], ["grade", "Grade"], ["circuito", "Circuito"], ["pontos", "Pontos"], ["liso", "Liso"]],
  mistura: function (hex) { var n = parseInt(hex, 16), r = n >> 16, g = (n >> 8) & 255, b = n & 255, h = function (v) { v = Math.max(0, Math.min(255, Math.round(v))); return (v < 16 ? "0" : "") + v.toString(16); }; return h(r * 0.55 + 10) + h(g * 0.5) + h(b * 0.7 + 20); },
  params: function (t) {
    var b = (this.BASES.filter(function (x) { return x[0] === t.base; })[0]) || this.BASES[0];
    return new URLSearchParams({ c1: t.c1, c2: t.c2, bg: b[2], sup: b[3], campo: b[4], su: b[5], tx: "ffffff", br: String(t.br), fu: t.fu, fo: t.fo, an: t.an ? "1" : "0", base: t.base, ti: t.ti, pk: t.pk, dim: String(t.dim) }).toString();
  },
  doEndereco: function (P) {
    var g = function (k, d) { var v = P.get(k); return v == null ? d : v; };
    return { c1: g("c1", "22d3ee"), c2: g("c2", "a78bfa"), base: g("base", "noite"), br: +g("br", "70"), fu: g("fu", "aurora"), fo: g("fo", "futurista"), an: g("an", "1") !== "0", ti: g("ti", "marca"), pk: g("pk", "linha"), dim: +g("dim", "35") };
  }
};

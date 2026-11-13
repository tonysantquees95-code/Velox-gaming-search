/* Tema e marca VX do Velox (compartilhado pelas páginas). Lê o tema do endereço (?c1=..&c2=..) e aplica nas variáveis de CSS. */
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

  /* Marca VX: V e X em letras chanfradas (corpo escuro, borda interna acesa e uma luz na segunda cor do tema). */
  var LETRAS = {
    v: "M0 0 L64 0 L110 124 L156 0 L220 0 L142 190 L78 190 Z",
    x1: "M190 0 L254 0 L400 190 L336 190 Z", x2: "M336 0 L400 0 L254 190 L190 190 Z"
  };
  var contaVGX = 0;
  window.desenharVGX = function (alvo, opcoes) {
    var k = ++contaVGX, o = opcoes || {}, id = "vx" + k;
    var emblema = o.emblema === true, texto = o.texto === true;
    var poly = function (att) { return [LETRAS.x1, LETRAS.x2, LETRAS.v].map(function (d) { return '<path d="' + d + '" ' + att + '/>'; }).join(""); };
    var fundo = "";
    if (emblema) {
      var ticks = ""; for (var i = 0; i < 36; i++) { var ang = i * 10 * Math.PI / 180, r1 = 214, r2 = i % 3 ? 220 : 228; ticks += '<line x1="' + (200 + r1 * Math.cos(ang)).toFixed(1) + '" y1="' + (95 + r1 * Math.sin(ang)).toFixed(1) + '" x2="' + (200 + r2 * Math.cos(ang)).toFixed(1) + '" y2="' + (95 + r2 * Math.sin(ang)).toFixed(1) + '"/>'; }
      var trilhas = ["M-130 20 H-60 L-36 44 H40", "M-130 70 H-80 L-60 90 H20", "M-130 130 H-90 L-70 150 H0", "M540 -10 H470 L446 14 H380", "M540 40 H490 L470 60 H400", "M540 110 H500 L480 130 H410"];
      var base = trilhas.map(function (d) { return '<path d="' + d + '"/>'; }).join("");
      var pulsos = trilhas.map(function (d, j) { return '<path class="pulso" pathLength="100" d="' + d + '" style="animation-duration:' + (2.2 + (j % 3) * 0.7).toFixed(1) + 's;animation-delay:-' + (j * 0.6).toFixed(1) + 's"/>'; }).join("");
      fundo = '<g fill="none" style="stroke:var(--neon)" stroke-opacity=".3" stroke-width="1.6">' +
        '<g class="gira"><circle cx="200" cy="95" r="168" stroke-opacity=".3"/></g><g class="gira-r"><circle cx="200" cy="95" r="196" stroke-dasharray="120 26 40 26" stroke-opacity=".28"/>' + ticks + '</g>' + base + '</g>' +
        '<g fill="none" style="stroke:var(--neon)" stroke-opacity=".16" stroke-width="1"><path d="M-110 -40 V230 M-80 -40 V230 M-50 -40 V230 M-110 -20 H-20 M-110 10 H-20 M-110 40 H-20 M-110 170 H-20 M-110 200 H-20 M450 -40 V230 M480 -40 V230 M510 -40 V230 M410 -20 H520 M410 10 H520 M410 170 H520 M410 200 H520"/></g>' +
        '<g fill="none" stroke-linecap="round" stroke-width="3.2" style="stroke:var(--neon)" class="pulsos">' + pulsos + '</g>' +
        '<g style="fill:var(--neon)" fill-opacity=".5"><circle cx="-36" cy="44" r="3.4"/><circle cx="20" cy="90" r="3.4"/><circle cx="446" cy="14" r="3.4"/><circle cx="470" cy="60" r="3.4"/></g>';
    }
    var vb = emblema ? "-140 -100 680 " + (texto ? 400 : 380) : "-14 -14 428 218";
    alvo.innerHTML = '<svg class="vgx" viewBox="' + vb + '" role="img" aria-label="VX">' +
      '<defs><linearGradient id="' + id + 'c" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#2a3f63"/><stop offset=".55" stop-color="#101a30"/><stop offset="1" stop-color="#070c18"/></linearGradient>' +
      '<clipPath id="' + id + 'k">' + poly("") + '</clipPath>' +
      '<filter id="' + id + 'g" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="2.2" result="m"/><feMerge><feMergeNode in="m"/><feMergeNode in="SourceGraphic"/></feMerge></filter>' +
      '<filter id="' + id + 'b" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="12"/></filter></defs>' +
      fundo +
      (o.brilho === false ? "" : '<g filter="url(#' + id + 'b)" opacity=".62" style="fill:var(--neon)">' + poly("") + '</g>') +
      '<g transform="translate(-8 8)" style="fill:var(--neon2);stroke:var(--neon2)" stroke-width="4" stroke-linejoin="miter" opacity=".95">' + poly("") + '</g>' +
      '<g fill="#03050c" stroke="#03050c" stroke-width="16" stroke-linejoin="miter">' + poly("") + '</g>' +
      '<g fill="url(#' + id + 'c)">' + poly("") + '</g>' +
      '<g clip-path="url(#' + id + 'k)" fill="none" stroke-linejoin="miter"><g style="stroke:var(--neon)" stroke-width="9">' + poly("") + '</g><g stroke="#fff" stroke-opacity=".4" stroke-width="3">' + poly("") + '</g></g>' +
      '<g class="corredores" fill="none" stroke-linejoin="round" stroke-linecap="round" filter="url(#' + id + 'g)">' +
        [LETRAS.x1, LETRAS.x2, LETRAS.v].map(function (d, j) { return '<path class="corre a" pathLength="100" d="' + d + '" style="animation-delay:-' + (j * 0.9).toFixed(1) + 's"/><path class="corre b" pathLength="100" d="' + d + '" style="animation-delay:-' + (1.6 + j * 0.9).toFixed(1) + 's"/>'; }).join("") + '</g>' +
      '<path d="M16 14 L56 14 L100 130 M236 14 L260 14 L392 178" fill="none" stroke="#fff" stroke-opacity=".12" stroke-width="6"/>' +
      (texto ? '<text x="200" y="262" text-anchor="middle" font-family="Segoe UI,system-ui,sans-serif" font-weight="800" font-size="50" letter-spacing="6" fill="#9fb0cc">VELOX GAMING</text><text x="200" y="300" text-anchor="middle" font-family="Segoe UI,system-ui,sans-serif" font-weight="700" font-size="26" letter-spacing="14" fill="#8fa0cf">SEARCH</text>' : '') +
      '</svg>';
  };
  document.querySelectorAll("[data-vgx]").forEach(function (el) { window.desenharVGX(el, { brilho: el.dataset.brilho !== "0", emblema: el.dataset.emblema === "1", texto: el.dataset.texto === "1" }); });
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

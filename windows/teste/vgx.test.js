const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("fs"), path = require("path"), vm = require("vm");

// O vgx.js roda no navegador; aqui ele roda com um "documento" mínimo só para conferir as funções de desenho.
function carregar() {
  const el = { dataset: {}, style: { setProperty() {} } };
  const janela = { location: { search: "" }, document: { documentElement: el, querySelectorAll: () => [] }, addEventListener() {} };
  janela.window = janela;
  const codigo = fs.readFileSync(path.join(__dirname, "..", "..", "app", "src", "main", "assets", "vgx.js"), "utf8");
  vm.runInNewContext(codigo, { ...janela, URLSearchParams, document: janela.document, location: janela.location, window: janela });
  return janela.VGX;
}

test("os ícones da barra são desenhados em todos os pacotes (a barra some se isto falhar)", () => {
  const V = carregar();
  assert.ok(V && typeof V.pacoteSvg === "function");
  for (const [id] of V.PACOTES) {
    const svg = V.pacoteSvg(id, '<path d="M15 5L8 12L15 19"/>', "22d3ee", "a78bfa", 1);
    assert.match(svg, /^<svg viewBox="0 0 24 24"/, id);
  }
});

test("cor que não é hexa de 6 dígitos nunca entra no desenho", () => {
  const svg = carregar().pacoteSvg("linha", '<path d="M0 0"/>', '"><script>', "zz", 2);
  assert.ok(!svg.includes("<script>"));
});

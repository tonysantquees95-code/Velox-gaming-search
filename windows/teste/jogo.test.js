const test = require("node:test");
const assert = require("node:assert/strict");
const { limitesBarra, proxima, valida } = require("../jogo");

test("posições andam em volta e valores estranhos viram direita", () => {
  assert.equal(proxima("dir"), "esq");
  assert.equal(proxima("baixo"), "dir");
  assert.equal(valida("xyz"), "dir");
});

test("a barra fica dentro da janela em cada posição", () => {
  for (const pos of ["dir", "esq", "cima", "baixo"]) for (const rec of [false, true]) {
    const r = limitesBarra(pos, rec, 1280, 720);
    assert.ok(r.x >= 0 && r.y >= 0 && r.x + r.width <= 1280 && r.y + r.height <= 720, `${pos} ${rec}`);
  }
  const d = limitesBarra("dir", false, 1280, 720);
  assert.equal(d.x + d.width + 12, 1280);
  const c = limitesBarra("cima", false, 1280, 720);
  assert.equal(c.y, 12);
  assert.equal(c.x * 2 + c.width, 1280);
});

test("janela minúscula não estoura", () => {
  const r = limitesBarra("baixo", false, 300, 200);
  assert.ok(r.width <= 300 && r.height <= 200);
});

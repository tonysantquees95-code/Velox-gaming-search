const test = require("node:test");
const assert = require("node:assert/strict");
const { normalizar, css } = require("../rolagem");

test("valores estranhos viram o padrão", () => {
  assert.deepEqual(normalizar(null), { modo: "hover", largura: 8, cor: "tema" });
  assert.deepEqual(normalizar({ modo: "x", largura: 99, cor: "y" }), { modo: "hover", largura: 16, cor: "tema" });
  assert.equal(normalizar({ largura: 1 }).largura, 6);
});

test("modo hover esconde a barra até o mouse passar", () => {
  const t = css({ modo: "hover" });
  assert.match(t, /thumb\{background:rgba\(34, 211, 238, 0\)/);
  assert.match(t, /\*:hover::-webkit-scrollbar-thumb/);
});

test("modo sempre fica visível e padrao não mexe em nada", () => {
  assert.doesNotMatch(css({ modo: "sempre" }), /\*:hover/);
  assert.match(css({ modo: "sempre", cor: "roxo" }), /167, 139, 250, \.5/);
  assert.equal(css({ modo: "padrao" }), "");
});

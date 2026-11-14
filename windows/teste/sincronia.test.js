const test = require("node:test");
const assert = require("node:assert/strict");
const { mesclar } = require("../sincronia.js");

const f = (u, t, n = u) => ({ n, u, t });

test("une favoritos dos dois lados sem repetir", () => {
  const r = mesclar({ favoritos: [f("a", 1), f("b", 2)], apagados: {} }, { favoritos: [f("b", 2), f("c", 3)], apagados: {} });
  assert.deepEqual(r.favoritos.map((x) => x.u), ["a", "b", "c"]);
});

test("apagado depois de criado some nos dois lados", () => {
  const agora = Date.now();
  const r = mesclar({ favoritos: [], apagados: { b: agora } }, { favoritos: [f("a", 1), f("b", agora - 1000)], apagados: {} });
  assert.deepEqual(r.favoritos.map((x) => x.u), ["a"]);
});

test("favoritado de novo depois de apagado volta", () => {
  const agora = Date.now();
  const r = mesclar({ favoritos: [f("b", agora)], apagados: {} }, { favoritos: [], apagados: { b: agora - 5000 } });
  assert.deepEqual(r.favoritos.map((x) => x.u), ["b"]);
});

test("entradas inválidas são ignoradas", () => {
  const r = mesclar({ favoritos: [null, { u: 1 }, f("a", 1)] }, null);
  assert.deepEqual(r.favoritos.map((x) => x.u), ["a"]);
});

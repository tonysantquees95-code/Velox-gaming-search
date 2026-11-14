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

const { mesclarSenhas, cifrarSenhas, decifrarSenhas } = require("../sincronia.js");
const s = (o, u, p, t) => ({ origem: o, usuario: u, senha: p, atualizado: t });

test("senhas: vence a mais nova e apagado depois de salvo some", () => {
  const agora = Date.now();
  const r = mesclarSenhas(
    { itens: [s("a", "x", "velha", agora - 2000), s("b", "y", "b1", agora - 2000)], apagados: {} },
    { itens: [s("a", "x", "nova", agora - 1000)], apagados: { "b\ny": agora - 500 } });
  assert.deepEqual(r.itens.map((i) => [i.origem, i.senha]), [["a", "nova"]]);
});

test("senhas: salvar de novo depois de apagar traz de volta", () => {
  const agora = Date.now();
  const r = mesclarSenhas({ itens: [s("a", "x", "p", agora)], apagados: {} }, { itens: [], apagados: { "a\nx": agora - 5000 } });
  assert.equal(r.itens.length, 1);
});

test("senhas: cifra e decifra com a mesma senha mestra e falha com outra", () => {
  const doc = { itens: [s("a", "x", "segredo", 1)], apagados: {} };
  const p = cifrarSenhas("mestra-123", doc);
  assert.doesNotMatch(JSON.stringify(p), /segredo/);
  assert.deepEqual(decifrarSenhas("mestra-123", p), doc);
  assert.throws(() => decifrarSenhas("outra-senha", p));
});

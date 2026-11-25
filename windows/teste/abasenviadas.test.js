const test = require("node:test");
const assert = require("node:assert/strict");
const { enviar, pendentes, marcarVisto, mesclar, urlValida, limpar, MAXIMO } = require("../abasenviadas");

const T = 1_700_000_000_000;

test("só aceita site http(s) sem usuário e senha", () => {
  assert.ok(urlValida("https://exemplo.com/a?b=1"));
  assert.equal(urlValida("javascript:alert(1)"), null);
  assert.equal(urlValida("file:///c:/x"), null);
  assert.equal(urlValida("https://user:pass@exemplo.com"), null);
  assert.equal(urlValida("https://localhost/"), null);
  assert.equal(urlValida("https://exemplo.com/" + "a".repeat(2100)), null);
});

test("quem enviou não recebe de volta; o outro aparelho recebe uma vez", () => {
  const d = enviar({ itens: [] }, { url: "https://exemplo.com/", titulo: "Exemplo", de: "cel", deNome: "Celular" }, T);
  assert.equal(pendentes(d, "cel", T).length, 0);
  assert.equal(pendentes(d, "pc", T).length, 1);
  const visto = marcarVisto(d, d.itens[0].id, "pc", T);
  assert.equal(pendentes(visto, "pc", T).length, 0);
});

test("reenviar a mesma página não duplica e itens velhos somem em 7 dias", () => {
  let d = enviar({ itens: [] }, { url: "https://exemplo.com/", de: "cel" }, T);
  d = enviar(d, { url: "https://exemplo.com/", de: "cel" }, T + 1000);
  assert.equal(d.itens.length, 1);
  assert.equal(limpar(d, T + 8 * 86_400_000).itens.length, 0);
});

test("a lista tem limite e endereço ruim é ignorado", () => {
  let d = { itens: [] };
  for (let i = 0; i < MAXIMO + 5; i++) d = enviar(d, { url: `https://exemplo.com/${i}`, de: "cel" }, T + i);
  assert.equal(d.itens.length, MAXIMO);
  assert.equal(enviar(d, { url: "javascript:1", de: "cel" }, T + 99).itens.length, MAXIMO);
});

test("conflito entre dois aparelhos junta os itens e os vistos", () => {
  const base = enviar({ itens: [] }, { url: "https://exemplo.com/", de: "cel" }, T);
  const a = marcarVisto(base, base.itens[0].id, "pc", T);
  const b = marcarVisto(base, base.itens[0].id, "note", T);
  const m = mesclar(a, b, T);
  assert.equal(m.itens.length, 1);
  assert.deepEqual([...m.itens[0].vistoPor].sort(), ["cel", "note", "pc"]);
});

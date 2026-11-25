const test = require("node:test");
const assert = require("node:assert/strict");
const { escolherParaDormir, resumo, frase, minutosValidos } = require("../dormir");

const MIN = 60_000;
const aba = (o) => ({ id: 1, interna: null, anonima: false, carregando: false, audivel: false, dormindo: false, url: "https://exemplo.com/a", ultimoUso: 0, ...o });

test("só dorme quem ficou parado além do limite", () => {
  const agora = 100 * MIN;
  const abas = [aba({ id: 1, ultimoUso: agora - 20 * MIN }), aba({ id: 2, ultimoUso: agora - 5 * MIN })];
  assert.deepEqual(escolherParaDormir(abas, agora, { minutos: 15 }), [1]);
});

test("nunca dorme a aba na tela, interna, anônima, carregando, com som, já dormindo ou que não é site", () => {
  const agora = 500 * MIN;
  const velha = { ultimoUso: 0 };
  const abas = [
    aba({ id: 1, ...velha }), aba({ id: 2, ...velha, interna: "inicio" }), aba({ id: 3, ...velha, anonima: true }),
    aba({ id: 4, ...velha, carregando: true }), aba({ id: 5, ...velha, audivel: true }), aba({ id: 6, ...velha, dormindo: true }),
    aba({ id: 7, ...velha, url: "file:///c:/x.html" }), aba({ id: 8, ...velha }),
  ];
  assert.deepEqual(escolherParaDormir(abas, agora, { minutos: 15, naTela: [8] }), [1]);
});

test("minutos fora da faixa viram valores seguros", () => {
  assert.equal(minutosValidos(1), 5);
  assert.equal(minutosValidos(9999), 240);
  assert.equal(minutosValidos("abc"), 15);
});

test("resumo soma só as dormindo e a frase fica em português", () => {
  const r = resumo([aba({ dormindo: true, mbLiberados: 120.4 }), aba({ dormindo: true, mbLiberados: 80 }), aba({ mbLiberados: 999 })]);
  assert.deepEqual(r, { dormindo: 2, mb: 200 });
  assert.equal(frase(r), "2 abas dormindo · cerca de 200 MB livres");
  assert.equal(frase({ dormindo: 1, mb: 0 }), "1 aba dormindo");
  assert.equal(frase({ dormindo: 0, mb: 0 }), "");
});

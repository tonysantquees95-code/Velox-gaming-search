const test = require("node:test");
const assert = require("node:assert/strict");
const A = require("../apresentacao");

test("novidades: ids únicos e crescentes, tipos válidos e textos preenchidos", () => {
  const ids = A.NOVIDADES.map((n) => n.id);
  assert.equal(new Set(ids).size, ids.length, "id repetido");
  assert.deepEqual(ids, [...ids].sort((a, b) => a - b), "ids fora de ordem: a mais nova vai no fim, com id = maior + 1");
  for (const n of A.NOVIDADES) {
    assert.ok(["novo", "melhoria", "correcao"].includes(n.tipo), `tipo inválido na novidade ${n.id}`);
    assert.match(n.data, /^\d{4}-\d{2}-\d{2}$/);
    assert.ok(n.titulo.trim().length >= 4 && n.texto.trim().length >= 10);
  }
});

test("mostra só o que a pessoa ainda não viu", () => {
  const l = [1, 2, 3, 4].map((id) => ({ id, data: "2026-11-14", tipo: "novo", titulo: "t" + id, texto: "texto" }));
  assert.deepEqual(A.novasDesde(2, l).map((n) => n.id), [4, 3]);
  assert.deepEqual(A.novasDesde(4, l), []);
  assert.equal(A.ultimoId(l), 4);
  assert.equal(A.ultimoId([]), 0);
});

test("apresentação: completa e só some depois de aceitar os termos e ver o tour", () => {
  assert.ok(A.TOUR.length >= 4 && A.TERMOS.length >= 5 && A.PERMISSOES.length > 50);
  assert.equal(A.precisaApresentar({}), true);
  assert.equal(A.precisaApresentar({ termosAceitos: A.VERSAO_DOS_TERMOS }), true);
  assert.equal(A.precisaApresentar({ tourVisto: true, termosAceitos: A.VERSAO_DOS_TERMOS - 1 }), true);
  assert.equal(A.precisaApresentar({ tourVisto: true, termosAceitos: A.VERSAO_DOS_TERMOS }), false);
});

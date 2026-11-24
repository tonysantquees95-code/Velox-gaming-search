const test = require("node:test");
const assert = require("node:assert/strict");
const { normalizar, doTexto, PADRAO } = require("../tema");

test("texto vazio ou estranho vira o tema padrão", () => {
  assert.deepEqual(doTexto(""), PADRAO);
  assert.deepEqual(doTexto("c1=zzz&pk=xx&fu=grade&lixo=1"), { ...PADRAO, fu: "grade" });
});

test("tema válido passa e fica completo e igual nos dois aparelhos", () => {
  const t = normalizar("c1=ff2a3d&c2=b0001a&base=vinho&pk=neon&an=0");
  assert.equal(doTexto(t).c1, "ff2a3d");
  assert.equal(doTexto(t).an, "0");
  assert.equal(normalizar(t), t);
  assert.equal(t.split("&").length, 19);
});

test("luz (LED) do logo: modos e cor válidos passam, o resto volta ao padrão", () => {
  assert.equal(doTexto("lz=rgb").lz, "rgb");
  assert.equal(doTexto("lz=cor&lc=ff00aa").lc, "ff00aa");
  assert.equal(doTexto("lz=laser&lc=zzz").lz, "padrao");
  assert.equal(doTexto("lz=laser&lc=zzz").lc, "22d3ee");
});

test("edição: padrão é gaming, valor estranho volta a gaming e a base grafite é aceita", () => {
  assert.equal(doTexto("").ed, "gaming");
  assert.equal(doTexto("ed=outra").ed, "gaming");
  assert.equal(doTexto("ed=pro&base=grafite").base, "grafite");
});

test("trocar de edição aplica o visual dela e trocar de volta restaura o gaming", () => {
  const { aplicarEdicao } = require("../tema");
  const pro = doTexto(aplicarEdicao("c1=ff2a3d&fu=grade&dim=60", "pro"));
  assert.equal(pro.ed, "pro");
  assert.equal(pro.fu, "liso");
  assert.equal(pro.fo, "redondo");
  assert.equal(pro.an, "0");
  assert.equal(pro.dim, "60", "o escurecer da foto não muda");
  assert.equal(doTexto(aplicarEdicao(normalizar("ed=pro"), "gaming")).c1, PADRAO.c1);
  assert.equal(aplicarEdicao(aplicarEdicao("", "pro"), "gaming"), normalizar(""));
});

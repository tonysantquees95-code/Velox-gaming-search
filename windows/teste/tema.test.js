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
  assert.equal(t.split("&").length, 18);
});

test("luz (LED) do logo: modos e cor válidos passam, o resto volta ao padrão", () => {
  assert.equal(doTexto("lz=rgb").lz, "rgb");
  assert.equal(doTexto("lz=cor&lc=ff00aa").lc, "ff00aa");
  assert.equal(doTexto("lz=laser&lc=zzz").lz, "padrao");
  assert.equal(doTexto("lz=laser&lc=zzz").lc, "22d3ee");
});

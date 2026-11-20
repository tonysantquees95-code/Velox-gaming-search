const test = require("node:test");
const assert = require("node:assert/strict");
const { nomeDeArquivo, enderecoValido, nomeSugerido, icoDePng, argumentosDe } = require("../atalho");

test("nome de arquivo tira o que o Windows não aceita", () => {
  assert.equal(nomeDeArquivo('YouTube: "vídeos" / música?'), "YouTube vídeos música");
  assert.equal(nomeDeArquivo("  ...  ", "Página"), "Página");
  assert.equal(nomeDeArquivo("CON", "Página"), "Página");
  assert.equal(nomeDeArquivo("Loja.  "), "Loja");
  assert.ok(nomeDeArquivo("a".repeat(200)).length <= 60);
});

test("só http e https viram atalho", () => {
  assert.equal(enderecoValido("https://exemplo.com/a?b=1"), true);
  assert.equal(enderecoValido("http://x.org"), true);
  assert.equal(enderecoValido("file:///c:/a.txt"), false);
  assert.equal(enderecoValido("javascript:alert(1)"), false);
  assert.equal(enderecoValido(""), false);
});

test("nome sugerido usa o título ou o site", () => {
  assert.equal(nomeSugerido("Minha loja", "https://a.com"), "Minha loja");
  assert.equal(nomeSugerido("", "https://www.youtube.com/watch?v=1"), "youtube.com");
  assert.equal(nomeSugerido("Velox", "https://www.bing.com"), "bing.com");
});

test(".ico leva o PNG inteiro e o cabeçalho certo", () => {
  const png = Buffer.from([0x89, 0x50, 0x4e, 0x47, 1, 2, 3]);
  const ico = icoDePng(png, 64);
  assert.equal(ico.readUInt16LE(2), 1);
  assert.equal(ico.readUInt16LE(4), 1);
  assert.equal(ico.readUInt8(6), 64);
  assert.equal(ico.readUInt32LE(14), png.length);
  assert.equal(ico.readUInt32LE(18), 22);
  assert.deepEqual(ico.subarray(22), png);
  assert.equal(icoDePng(png, 256).readUInt8(6), 0);
});

test("argumentos protegem aspas do endereço", () => {
  assert.equal(argumentosDe("https://a.com/x"), '"https://a.com/x"');
  assert.equal(argumentosDe('https://a.com/"x'), '"https://a.com/%22x"');
});

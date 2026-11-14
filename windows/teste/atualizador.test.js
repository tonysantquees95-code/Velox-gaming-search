const test = require("node:test");
const assert = require("node:assert/strict");
const { numeroDaVersao, urlPermitida, decidir, sha256DoArquivo } = require("../atualizador");

const SHA = "a".repeat(64);
const U = "https://github.com/tonysantquees95-code/velox-gaming-search/releases/download/windows-1.9/Velox-Instalador-1.9.0.exe";

test("o número da versão vem do segundo pedaço", () => {
  assert.equal(numeroDaVersao("1.23.0"), 23);
  assert.equal(numeroDaVersao("1.5"), 5);
  assert.equal(numeroDaVersao("1.0.0"), 0);
  assert.equal(numeroDaVersao("lixo"), 0);
  assert.equal(numeroDaVersao(undefined), 0);
});

test("só link https do GitHub, sem caracteres perigosos", () => {
  assert.ok(urlPermitida(U));
  assert.ok(urlPermitida("https://objects.githubusercontent.com/x/y"));
  assert.ok(!urlPermitida("http://github.com/x/y.exe"));
  assert.ok(!urlPermitida("https://evil.com/github.com/x.exe"));
  assert.ok(!urlPermitida("https://github.com.evil.com/x.exe"));
  assert.ok(!urlPermitida(U + " & calc"));
  assert.ok(!urlPermitida(U + '"'));
  assert.ok(!urlPermitida(null));
});

test("decide entre nada, instalar sozinho e só avisar", () => {
  assert.equal(decidir("1.9.0", { numero: 9, url: U, sha256: SHA }).acao, "nada");
  assert.equal(decidir("1.9.0", { numero: 8, url: U, sha256: SHA }).acao, "nada");
  assert.equal(decidir("1.0.0", { numero: 9, url: U, sha256: SHA }).acao, "nada", "versão de desenvolvimento não se atualiza");
  assert.equal(decidir("1.9.0", null).acao, "nada");
  const r = decidir("1.9.0", { numero: 10, url: U, sha256: SHA.toUpperCase(), tamanho: 5 });
  assert.equal(r.acao, "instalar");
  assert.equal(r.sha256, SHA);
  assert.equal(r.numero, 10);
  assert.equal(decidir("1.9.0", { numero: 10, url: U, sha256: "" }).acao, "avisar", "sem SHA-256 não instala sozinho");
  assert.equal(decidir("1.9.0", { numero: 10, url: "https://evil.com/x.exe", sha256: SHA }).acao, "avisar");
});

test("calcula o SHA-256 de um arquivo", () => {
  assert.equal(sha256DoArquivo(Buffer.from("abc")), "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
});

const test = require("node:test");
const assert = require("node:assert/strict");
const { urlTraduzida, ehTraduzida, urlOriginal } = require("../traduzir");
const { ehPdf, candidatos, achar } = require("../vindex");

test("monta o endereço da tradução e recupera o original", () => {
  const t = urlTraduzida("https://exemplo.com/a b?x=1");
  assert.ok(t.startsWith("https://translate.google.com/translate?"));
  assert.equal(urlOriginal(t), "https://exemplo.com/a%20b?x=1");
  assert.ok(ehTraduzida(t));
  assert.ok(ehTraduzida("https://www-exemplo-com.translate.goog/a?_x_tr_sl=auto"));
});

test("não traduz o que não é site nem o que já é tradução", () => {
  assert.equal(urlTraduzida("file:///c:/a.html"), null);
  assert.equal(urlTraduzida("velox://inicio"), null);
  assert.equal(urlTraduzida(""), null);
  assert.equal(urlTraduzida(urlTraduzida("https://exemplo.com/")), null);
  assert.equal(urlOriginal("https://exemplo.com/"), null);
});

test("PDF e lugares do Vindex", () => {
  assert.ok(ehPdf("Contrato.PDF"));
  assert.ok(!ehPdf("foto.png"));
  const env = { LOCALAPPDATA: "C:\\Users\\a\\AppData\\Local", ProgramFiles: "C:\\Program Files" };
  assert.equal(candidatos(env).length, 2);
  assert.equal(achar((c) => c.includes("Program Files"), env), candidatos(env)[1]);
  assert.equal(achar(() => false, env), null);
});

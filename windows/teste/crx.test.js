const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("fs");
const os = require("os");
const path = require("path");
const { zipDoCrx, extrairZip, idDaLoja } = require("../crx.js");

const CRX = Buffer.from("Q3IyNAMAAAAFAAAAAAAAAABQSwMEFAAAAAgAZ6ltXSiyktU0AAAAOgAAAA0AAABtYW5pZmVzdC5qc29uq1bKTczLTEstLokvSy0qzszPU7JSMNZRUMpLzE1VslJQCkktLklV0lFQQkgrGeoZKNUCAFBLAwQUAAAACABnqW1dm2SSXR0AAACEAwAACwAAAGpzL2Z1bmRvLmpzS87PK87PSdXLyU/XUM/PVNe0Th4VGRUZFaGjCABQSwECFAMUAAAACABnqW1dKLKS1TQAAAA6AAAADQAAAAAAAAAAAAAAgAEAAAAAbWFuaWZlc3QuanNvblBLAQIUAxQAAAAIAGepbV2bZJJdHQAAAIQDAAALAAAAAAAAAAAAAACAAV8AAABqcy9mdW5kby5qc1BLBQYAAAAAAgACAHQAAAClAAAAAAA=", "base64");
const ZIP = Buffer.from("UEsDBBQAAAAIAGepbV0ospLVNAAAADoAAAANAAAAbWFuaWZlc3QuanNvbqtWyk3My0xLLS6JL0stKs7Mz1OyUjDWUVDKS8xNVbJSUApJLS5JVdJRUEJIKxnqGSjVAgBQSwMEFAAAAAgAZ6ltXZtkkl0dAAAAhAMAAAsAAABqcy9mdW5kby5qc0vOzyvOz0nVy8lP11DPz1TXtE4eFRkVGRWhowgAUEsBAhQDFAAAAAgAZ6ltXSiyktU0AAAAOgAAAA0AAAAAAAAAAAAAAIABAAAAAG1hbmlmZXN0Lmpzb25QSwECFAMUAAAACABnqW1dm2SSXR0AAACEAwAACwAAAAAAAAAAAAAAgAFfAAAAanMvZnVuZG8uanNQSwUGAAAAAAIAAgB0AAAApQAAAAAA", "base64");

test("extrai o zip de dentro do CRX3", () => {
  const pasta = fs.mkdtempSync(path.join(os.tmpdir(), "crx-"));
  const n = extrairZip(zipDoCrx(CRX), pasta);
  assert.equal(n, 2);
  assert.equal(JSON.parse(fs.readFileSync(path.join(pasta, "manifest.json"), "utf8")).name, "Teste");
  assert.ok(fs.readFileSync(path.join(pasta, "js", "fundo.js"), "utf8").startsWith("console.log"));
});

test("aceita zip puro e recusa lixo", () => {
  assert.equal(zipDoCrx(ZIP), ZIP);
  assert.throws(() => zipDoCrx(Buffer.from("isto não é uma extensão de verdade")), /não é uma extensão/);
});

test("reconhece o ID da loja", () => {
  const id = "cjpalhdlnbpafiamejdnhcphjbkeiagm";
  assert.equal(idDaLoja(id), id);
  assert.equal(idDaLoja("https://chromewebstore.google.com/detail/ublock-origin/" + id + "?hl=pt"), id);
  assert.equal(idDaLoja("qualquer coisa"), null);
});

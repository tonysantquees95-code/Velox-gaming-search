const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("fs");
const path = require("path");

test("todo arquivo local que o main.js carrega entra no instalador", () => {
  const pacote = JSON.parse(fs.readFileSync(path.join(__dirname, "..", "package.json"), "utf8"));
  const lista = pacote.build.files;
  const codigo = fs.readFileSync(path.join(__dirname, "..", "main.js"), "utf8");
  const locais = [...codigo.matchAll(/require\("\.\/([\w-]+)"\)/g)].map((m) => m[1] + ".js");
  assert.ok(locais.length > 5);
  for (const arq of locais) assert.ok(lista.includes(arq), `${arq} falta em build.files`);
});

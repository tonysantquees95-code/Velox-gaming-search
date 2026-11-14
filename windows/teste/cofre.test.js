const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("fs");
const os = require("os");
const path = require("path");
const { Cofre } = require("../cofre.js");

const novo = () => new Cofre(path.join(fs.mkdtempSync(path.join(os.tmpdir(), "velox-")), "cofre.json"));

test("cria, guarda, trava e reabre com a senha mestra", () => {
  const c = novo();
  assert.equal(c.existe(), false);
  c.criar("senha-mestra");
  assert.equal(c.salvar("https://a.com", "toni", "segredo1"), "novo");
  assert.equal(c.salvar("https://a.com", "toni", "segredo1"), "igual");
  assert.equal(c.salvar("https://a.com", "toni", "segredo2"), "atualizado");
  c.travar();
  assert.throws(() => c.doSite("https://a.com"), /trancado/);
  assert.throws(() => c.abrir("errada"), /incorreta/);
  c.abrir("senha-mestra");
  assert.deepEqual(c.doSite("https://a.com").map((x) => [x.usuario, x.senha]), [["toni", "segredo2"]]);
  assert.equal(c.lista()[0].senha, undefined);
});

test("o arquivo não deixa a senha à vista", () => {
  const c = novo();
  c.criar("senha-mestra");
  c.salvar("https://a.com", "toni", "segredo-bem-unico");
  const bruto = fs.readFileSync(c.arquivo, "utf8");
  assert.ok(!bruto.includes("segredo-bem-unico"));
  assert.ok(!bruto.includes("toni"));
});

test("apagar, nunca oferecer e trocar a senha mestra", () => {
  const c = novo();
  c.criar("senha-mestra");
  c.salvar("https://a.com", "u", "p");
  c.nuncaOferecer("https://b.com");
  c.trocarSenha("senha-mestra", "outra-senha");
  c.travar();
  assert.throws(() => c.abrir("senha-mestra"), /incorreta/);
  c.abrir("outra-senha");
  assert.equal(c.jaPerguntou("https://b.com"), true);
  const id = c.lista()[0].id;
  assert.equal(c.revelar(id), "p");
  c.apagar(id);
  assert.equal(c.lista().length, 0);
  assert.throws(() => c.criar("123"), /pelo menos|Já existe/);
});

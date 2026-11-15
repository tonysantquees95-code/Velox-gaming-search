// Confere o download em partes contra um servidor local que aceita (ou não) pedidos por faixa.
const test = require("node:test"), assert = require("assert"), http = require("http"), fs = require("fs"), os = require("os"), path = require("path"), crypto = require("crypto");
const { sondar, baixarEmPartes } = require("../baixador");

function servidor(dados, { faixas = true, quebrarUma = false } = {}) {
  let quebrou = false;
  const s = http.createServer((req, res) => {
    const f = /^bytes=(\d+)-(\d+)$/.exec(req.headers.range || "");
    if (!faixas || !f) { res.writeHead(200, { "content-length": dados.length }); return res.end(dados); }
    const ini = Number(f[1]), fim = Math.min(Number(f[2]), dados.length - 1);
    if (quebrarUma && ini > 0 && !quebrou) { quebrou = true; res.writeHead(206, { "content-range": `bytes ${ini}-${fim}/${dados.length}`, "content-length": fim - ini + 1 }); res.write(dados.subarray(ini, ini + 10)); return res.destroy(); }
    res.writeHead(206, { "content-range": `bytes ${ini}-${fim}/${dados.length}`, "content-length": fim - ini + 1 });
    res.end(dados.subarray(ini, fim + 1));
  });
  return new Promise((ok) => s.listen(0, "127.0.0.1", () => ok({ s, url: `http://127.0.0.1:${s.address().port}/a.bin` })));
}
const dados = crypto.randomBytes(1_000_003);
const pasta = fs.mkdtempSync(path.join(os.tmpdir(), "velox-"));

test("sonda: servidor com faixas", async () => {
  const { s, url } = await servidor(dados);
  const r = await sondar(fetch, url);
  s.close();
  assert.deepStrictEqual({ ok: r.ok, total: r.total }, { ok: true, total: dados.length });
});
test("sonda: servidor sem faixas manda baixar do jeito normal", async () => {
  const { s, url } = await servidor(dados, { faixas: false });
  const r = await sondar(fetch, url);
  s.close();
  assert.strictEqual(r.ok, false);
});
test("baixa em partes e o arquivo sai idêntico", async () => {
  const { s, url } = await servidor(dados);
  const destino = path.join(pasta, "ok.bin"); let ultimo = 0;
  await baixarEmPartes({ buscar: fetch, url, destino, total: dados.length, conexoes: 5, pedaco: 64 * 1024, aoProgresso: (b) => { ultimo = b; } });
  s.close();
  assert.ok(fs.readFileSync(destino).equals(dados));
  assert.strictEqual(ultimo, dados.length);
  assert.ok(!fs.existsSync(destino + ".parte"));
});
test("refaz o pedaço que quebrou no meio", async () => {
  const { s, url } = await servidor(dados, { quebrarUma: true });
  const destino = path.join(pasta, "retoma.bin");
  await baixarEmPartes({ buscar: fetch, url, destino, total: dados.length, conexoes: 3, pedaco: 100 * 1024 });
  s.close();
  assert.ok(fs.readFileSync(destino).equals(dados));
});
test("cancelar apaga o que estava pela metade", async () => {
  const { s, url } = await servidor(dados);
  const destino = path.join(pasta, "cancela.bin"); const ac = new AbortController();
  const p = baixarEmPartes({ buscar: fetch, url, destino, total: dados.length, conexoes: 2, pedaco: 32 * 1024, sinal: ac.signal, aoProgresso: () => ac.abort() });
  await assert.rejects(p);
  s.close();
  assert.ok(!fs.existsSync(destino) && !fs.existsSync(destino + ".parte"));
});

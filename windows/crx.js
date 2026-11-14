// Extensões do Chrome: baixa o .crx da loja pelo ID, tira o zip de dentro e abre os arquivos numa pasta.
const zlib = require("zlib");
const fs = require("fs");
const path = require("path");

/** Devolve o zip que vem depois do cabeçalho do CRX (versões 2 e 3). */
function zipDoCrx(buf) {
  if (buf.length > 4 && buf.readUInt32LE(0) === 0x04034b50) return buf; // já é um zip
  if (buf.length < 16 || buf.toString("latin1", 0, 4) !== "Cr24") throw new Error("Esse arquivo não é uma extensão do Chrome.");
  const versao = buf.readUInt32LE(4);
  if (versao === 3) return buf.subarray(12 + buf.readUInt32LE(8));
  if (versao === 2) return buf.subarray(16 + buf.readUInt32LE(8) + buf.readUInt32LE(12));
  throw new Error("Versão de extensão não suportada.");
}

/** Lê o zip pela lista central e extrai tudo em `pasta`, recusando caminhos fora dela. */
function extrairZip(zip, pasta) {
  let fim = -1;
  for (let i = zip.length - 22; i >= Math.max(0, zip.length - 65_557); i--) {
    if (zip.readUInt32LE(i) === 0x06054b50) { fim = i; break; }
  }
  if (fim < 0) throw new Error("O arquivo da extensão está corrompido.");
  const total = zip.readUInt16LE(fim + 10);
  let p = zip.readUInt32LE(fim + 16);
  const raiz = path.resolve(pasta);
  fs.mkdirSync(raiz, { recursive: true });
  let arquivos = 0, bytes = 0;
  for (let n = 0; n < total; n++) {
    if (zip.readUInt32LE(p) !== 0x02014b50) throw new Error("O arquivo da extensão está corrompido.");
    const metodo = zip.readUInt16LE(p + 10);
    const tamComp = zip.readUInt32LE(p + 20);
    const tamNormal = zip.readUInt32LE(p + 24);
    const nomeLen = zip.readUInt16LE(p + 28), extraLen = zip.readUInt16LE(p + 30), comLen = zip.readUInt16LE(p + 32);
    const local = zip.readUInt32LE(p + 42);
    const nome = zip.toString("utf8", p + 46, p + 46 + nomeLen);
    p += 46 + nomeLen + extraLen + comLen;
    if (nome.endsWith("/")) continue;
    const destino = path.resolve(raiz, nome);
    if (destino !== raiz && !destino.startsWith(raiz + path.sep)) throw new Error("Extensão com caminho suspeito: recusada.");
    bytes += tamNormal;
    if (++arquivos > 5000 || bytes > 200 * 1024 * 1024) throw new Error("Extensão grande demais.");
    const inicio = local + 30 + zip.readUInt16LE(local + 26) + zip.readUInt16LE(local + 28);
    const dados = zip.subarray(inicio, inicio + tamComp);
    const conteudo = metodo === 0 ? dados : metodo === 8 ? zlib.inflateRawSync(dados) : null;
    if (!conteudo) throw new Error("Extensão com compressão não suportada.");
    fs.mkdirSync(path.dirname(destino), { recursive: true });
    fs.writeFileSync(destino, conteudo);
  }
  if (!fs.existsSync(path.join(raiz, "manifest.json"))) throw new Error("A extensão não tem manifest.json.");
  return arquivos;
}

const ID_VALIDO = /^[a-p]{32}$/;
/** Aceita o ID ou o endereço da página da extensão na Chrome Web Store. */
function idDaLoja(texto) {
  const t = String(texto || "").trim();
  if (ID_VALIDO.test(t)) return t;
  const m = /chrome(?:web)?store\.google\.com\/.*\/([a-p]{32})(?:[/?#]|$)/i.exec(t) || /chromewebstore\.google\.com\/detail\/[^/]+\/([a-p]{32})/i.exec(t);
  return m ? m[1] : null;
}

async function baixarDaLoja(id, pasta, versaoChrome = "130.0.0.0") {
  const url = `https://clients2.google.com/service/update2/crx?response=redirect&prodversion=${versaoChrome}&acceptformat=crx2,crx3&x=${encodeURIComponent(`id=${id}&uc`)}`;
  const r = await fetch(url);
  if (!r.ok) throw new Error(`A loja não entregou a extensão (${r.status}).`);
  const buf = Buffer.from(await r.arrayBuffer());
  fs.rmSync(pasta, { recursive: true, force: true });
  extrairZip(zipDoCrx(buf), pasta);
}

module.exports = { zipDoCrx, extrairZip, idDaLoja, baixarDaLoja };

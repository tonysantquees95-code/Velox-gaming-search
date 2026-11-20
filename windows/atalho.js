// Atalho de uma página para a área de trabalho / Menu Iniciar. Lógica pura (testada em teste/atalho.test.js).

/** Nome de arquivo seguro no Windows, a partir do nome que a pessoa escolheu. */
function nomeDeArquivo(nome, padrao) {
  const base = String(nome || "").replace(/[\\/:*?"<>|\u0000-\u001f]/g, " ").replace(/\s+/g, " ").trim().replace(/[. ]+$/, "").slice(0, 60);
  const proibidos = /^(con|prn|aux|nul|com\d|lpt\d)$/i;
  const r = base && !proibidos.test(base) ? base : String(padrao || "Velox");
  return r;
}

/** Só endereços http(s) viram atalho. */
function enderecoValido(url) { return /^https?:\/\/[^\s]+$/i.test(String(url || "")); }

/** Nome sugerido: o título da página ou, sem ele, o endereço do site. */
function nomeSugerido(titulo, url) {
  const t = String(titulo || "").trim();
  if (t && t.toLowerCase() !== "velox") return t.slice(0, 40);
  try { return new URL(url).hostname.replace(/^www\./, ""); } catch (_) { return "Página"; }
}

/** Empacota uma imagem PNG em um arquivo .ico (o Windows aceita PNG dentro do .ico). `lado` = 16..256. */
function icoDePng(png, lado) {
  const l = Math.max(16, Math.min(256, lado | 0));
  const cab = Buffer.alloc(22);
  cab.writeUInt16LE(0, 0); cab.writeUInt16LE(1, 2); cab.writeUInt16LE(1, 4);
  cab.writeUInt8(l === 256 ? 0 : l, 6); cab.writeUInt8(l === 256 ? 0 : l, 7);
  cab.writeUInt8(0, 8); cab.writeUInt8(0, 9);
  cab.writeUInt16LE(1, 10); cab.writeUInt16LE(32, 12);
  cab.writeUInt32LE(png.length, 14); cab.writeUInt32LE(22, 18);
  return Buffer.concat([cab, png]);
}

/** Argumentos do atalho: só o endereço, entre aspas (o Velox já abre o endereço recebido). */
function argumentosDe(url) { return `"${String(url).replace(/"/g, "%22")}"`; }

module.exports = { nomeDeArquivo, enderecoValido, nomeSugerido, icoDePng, argumentosDe };

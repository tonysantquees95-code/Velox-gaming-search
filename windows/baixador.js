// Baixa um arquivo em várias partes ao mesmo tempo, quando o site aceita pedidos por faixa (Range).
// Muitos sites limitam a velocidade de cada conexão; com várias juntas a soma rende mais.
// `buscar` é uma função parecida com fetch (no app vem da sessão do navegador, com cookies).
const fs = require("fs");

const MINIMO = 8 * 1024 * 1024; // abaixo disso não compensa

/** Pergunta ao servidor se aceita faixas. Devolve { ok, total, etag } — ok falso = baixar do jeito normal. */
async function sondar(buscar, url, cabecalhos = {}, sinal) {
  try {
    const r = await buscar(url, { headers: { ...cabecalhos, Range: "bytes=0-0" }, signal: sinal });
    if (r.body && r.body.cancel) r.body.cancel().catch(() => {});
    if (r.status !== 206) return { ok: false };
    const enc = (r.headers.get("content-encoding") || "identity").toLowerCase();
    if (enc !== "identity") return { ok: false };
    const m = /^bytes\s+0-0\/(\d+)$/i.exec(r.headers.get("content-range") || "");
    if (!m) return { ok: false };
    const total = Number(m[1]);
    if (!(total > 0)) return { ok: false };
    const etag = r.headers.get("etag") || "";
    return { ok: true, total, etag: etag && !/^W\//.test(etag) ? etag : "" };
  } catch (_) { return { ok: false }; }
}

const espera = (ms) => new Promise((r) => setTimeout(r, ms));

/**
 * Baixa `total` bytes de `url` para `destino` em pedaços, com `conexoes` ao mesmo tempo.
 * Grava em destino + ".parte" e só renomeia no fim. Lança erro se algum pedaço falhar de vez.
 */
async function baixarEmPartes({ buscar, url, destino, total, etag = "", cabecalhos = {}, conexoes = 6, pedaco = 4 * 1024 * 1024, aoProgresso = () => {}, sinal }) {
  const temp = destino + ".parte";
  const ac = new AbortController();
  if (sinal) { if (sinal.aborted) ac.abort(); else sinal.addEventListener("abort", () => ac.abort(), { once: true }); }
  const fh = await fs.promises.open(temp, "w");
  let recebido = 0, proximo = 0;
  const n = Math.ceil(total / pedaco);
  const limpar = async () => { try { await fh.close(); } catch (_) {} try { await fs.promises.unlink(temp); } catch (_) {} };
  try {
    await fh.truncate(total);
    const baixarPedaco = async (i) => {
      const ini = i * pedaco, fim = Math.min(total, ini + pedaco) - 1;
      let ultimoErro = null;
      for (let tentativa = 0; tentativa < 4; tentativa++) {
        if (ac.signal.aborted) throw new Error("cancelado");
        let pos = ini;
        try {
          const r = await buscar(url, { headers: { ...cabecalhos, Range: `bytes=${ini}-${fim}`, ...(etag ? { "If-Range": etag } : {}) }, signal: ac.signal });
          if (r.status !== 206) throw new Error("o site não aceitou continuar o pedaço (" + r.status + ")");
          const m = /^bytes\s+(\d+)-(\d+)\/(\d+|\*)$/i.exec(r.headers.get("content-range") || "");
          if (!m || Number(m[1]) !== ini) throw new Error("faixa errada");
          const leitor = r.body.getReader();
          for (;;) {
            const { done, value } = await leitor.read();
            if (done) break;
            if (pos + value.length > fim + 1) throw new Error("veio mais do que o pedido");
            await fh.write(value, 0, value.length, pos);
            pos += value.length;
            recebido += value.length;
            aoProgresso(recebido);
          }
          if (pos !== fim + 1) throw new Error("pedaço veio incompleto");
          return;
        } catch (e) {
          recebido -= pos - ini; // desconta o que esse pedaço já tinha contado: vai refazer
          aoProgresso(recebido);
          ultimoErro = e;
          if (ac.signal.aborted) throw new Error("cancelado");
          await espera(400 * Math.pow(2, tentativa));
        }
      }
      throw ultimoErro || new Error("falhou");
    };
    const trabalhador = async () => {
      for (;;) {
        if (ac.signal.aborted) return;
        const i = proximo++;
        if (i >= n) return;
        await baixarPedaco(i);
      }
    };
    const lote = Array.from({ length: Math.max(1, Math.min(conexoes, n)) }, () => trabalhador().catch((e) => { ac.abort(); throw e; }));
    const res = await Promise.allSettled(lote);
    const falha = res.find((x) => x.status === "rejected");
    if (falha || ac.signal.aborted) throw falha ? falha.reason : new Error("cancelado");
    await fh.close();
    await fs.promises.rename(temp, destino);
  } catch (e) {
    await limpar();
    throw e;
  }
}

module.exports = { sondar, baixarEmPartes, MINIMO };

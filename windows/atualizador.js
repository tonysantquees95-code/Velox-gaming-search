// Atualização do Velox no Windows: consulta a versão mais nova, baixa o instalador, confere o SHA-256 e instala em silêncio.
// A parte de decisão fica aqui (sem Electron) para poder testar.
const crypto = require("crypto");

const SERVIDOR = "https://venus-servidor.tonysantquees95.workers.dev";
const ENDERECO = SERVIDOR + "/atualizacao?app=velox-windows";
const DOWNLOAD = ENDERECO + "&baixar=1";

/** "1.23.0" → 23 (o número da publicação é o segundo pedaço da versão). */
function numeroDaVersao(versao) {
  const m = /^1\.(\d+)(?:\.\d+)?$/.exec(String(versao || "").trim());
  return m ? Number(m[1]) : 0;
}

/** Só aceita link https do GitHub (onde ficam as publicações), sem espaços nem aspas. */
function urlPermitida(url) {
  if (typeof url !== "string" || /[\s"'`$<>^&|]/.test(url)) return false;
  const m = /^https:\/\/([^/]+)\//.exec(url);
  if (!m) return false;
  const h = m[1].toLowerCase();
  return h === "github.com" || h.endsWith(".github.com") || h.endsWith(".githubusercontent.com");
}

const shaValido = (s) => typeof s === "string" && /^[0-9a-f]{64}$/i.test(s);

/**
 * Decide o que fazer com a resposta do servidor.
 * - "nada": está em dia (ou a resposta não serve)
 * - "instalar": dá para baixar e instalar sozinho (tem link seguro e SHA-256)
 * - "avisar": há versão nova, mas sem como conferir o arquivo; só avisa e leva para a página de download
 */
function decidir(versaoAtual, resposta) {
  if (!resposta || typeof resposta !== "object") return { acao: "nada" };
  const atual = numeroDaVersao(versaoAtual);
  const nova = Number(resposta.numero) || 0;
  if (!atual || nova <= atual) return { acao: "nada" };
  if (urlPermitida(resposta.url) && shaValido(resposta.sha256)) return { acao: "instalar", numero: nova, url: resposta.url, sha256: resposta.sha256.toLowerCase(), tamanho: Number(resposta.tamanho) || 0 };
  return { acao: "avisar", numero: nova };
}

const sha256DoArquivo = (buf) => crypto.createHash("sha256").update(buf).digest("hex");

/** Roteiro do PowerShell que roda depois que o Velox fecha: instala em silêncio e abre o Velox de novo. Os valores vão por variáveis de ambiente. */
const ROTEIRO = "$ErrorActionPreference='SilentlyContinue'; " +
  "try { Wait-Process -Id ([int]$env:V_PID) -Timeout 30 } catch {} ; " +
  "Start-Process -FilePath $env:V_SETUP -ArgumentList '/S' -Wait; " +
  "Remove-Item $env:V_SETUP -Force; " +
  "Start-Process -FilePath $env:V_APP";

module.exports = { SERVIDOR, ENDERECO, DOWNLOAD, numeroDaVersao, urlPermitida, shaValido, decidir, sha256DoArquivo, ROTEIRO };

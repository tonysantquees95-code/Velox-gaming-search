// Abrir um PDF baixado no Vindex (o leitor da suíte). O Vindex do Windows instala só para o usuário, em %LOCALAPPDATA%\Programs\Vindex.
const path = require("path");

const ehPdf = (nome) => /\.pdf$/i.test(String(nome || ""));

/** Lugares onde o Vindex costuma estar, do mais provável ao menos. */
function candidatos(env = process.env) {
  const l = [];
  if (env.LOCALAPPDATA) l.push(path.join(env.LOCALAPPDATA, "Programs", "Vindex", "Vindex.exe"));
  if (env.ProgramFiles) l.push(path.join(env.ProgramFiles, "Vindex", "Vindex.exe"));
  if (env["ProgramFiles(x86)"]) l.push(path.join(env["ProgramFiles(x86)"], "Vindex", "Vindex.exe"));
  return l;
}

/** Primeiro caminho que existe (existe = função de teste, normalmente fs.existsSync). */
function achar(existe, env = process.env) { return candidatos(env).find((c) => existe(c)) || null; }

module.exports = { ehPdf, candidatos, achar };

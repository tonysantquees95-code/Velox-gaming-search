// Cofre de senhas do Velox: arquivo local cifrado com AES-256-GCM; a chave vem da senha mestra (scrypt).
// A senha mestra nunca é guardada. Sem ela, o arquivo não abre.
const crypto = require("crypto");
const fs = require("fs");

const N = 1 << 15;
const FORMATO = 1;

function derivar(senha, sal) {
  return crypto.scryptSync(String(senha), sal, 32, { N, r: 8, p: 1, maxmem: 128 * N * 8 * 2 });
}

function cifrar(chave, objeto) {
  const iv = crypto.randomBytes(12);
  const c = crypto.createCipheriv("aes-256-gcm", chave, iv);
  const dados = Buffer.concat([c.update(JSON.stringify(objeto), "utf8"), c.final()]);
  return { iv: iv.toString("base64"), tag: c.getAuthTag().toString("base64"), dados: dados.toString("base64") };
}

function decifrar(chave, pacote) {
  const d = crypto.createDecipheriv("aes-256-gcm", chave, Buffer.from(pacote.iv, "base64"));
  d.setAuthTag(Buffer.from(pacote.tag, "base64"));
  const claro = Buffer.concat([d.update(Buffer.from(pacote.dados, "base64")), d.final()]);
  return JSON.parse(claro.toString("utf8"));
}

class Cofre {
  constructor(arquivo, minutosAteTravar = 15) {
    this.arquivo = arquivo;
    this.chave = null;
    this.sal = null;
    this.itens = [];
    this.nunca = []; // sites onde a pessoa pediu para nunca oferecer salvar
    this.ms = minutosAteTravar * 60_000;
    this.timer = null;
    this.aoTravar = null;
  }

  existe() { return fs.existsSync(this.arquivo); }
  aberto() { return !!this.chave; }

  criar(senha) {
    if (String(senha).length < 6) throw new Error("A senha mestra precisa ter pelo menos 6 caracteres.");
    if (this.existe()) throw new Error("Já existe um cofre neste computador.");
    this.sal = crypto.randomBytes(16);
    this.chave = derivar(senha, this.sal);
    this.itens = [];
    this.nunca = [];
    this.gravar();
    this.reiniciarRelogio();
  }

  abrir(senha) {
    const arq = JSON.parse(fs.readFileSync(this.arquivo, "utf8"));
    const sal = Buffer.from(arq.sal, "base64");
    const chave = derivar(senha, sal);
    let dados;
    try { dados = decifrar(chave, arq); } catch { throw new Error("Senha mestra incorreta."); }
    this.sal = sal;
    this.chave = chave;
    this.itens = Array.isArray(dados.itens) ? dados.itens : [];
    this.nunca = Array.isArray(dados.nunca) ? dados.nunca : [];
    this.reiniciarRelogio();
  }

  travar() {
    this.chave = null;
    this.itens = [];
    this.nunca = [];
    if (this.timer) clearTimeout(this.timer);
    this.timer = null;
    if (this.aoTravar) this.aoTravar();
  }

  reiniciarRelogio() {
    if (this.timer) clearTimeout(this.timer);
    this.timer = setTimeout(() => this.travar(), this.ms);
    if (this.timer.unref) this.timer.unref();
  }

  exigir() {
    if (!this.chave) throw new Error("O cofre está trancado.");
    this.reiniciarRelogio();
  }

  gravar() {
    const pacote = { v: FORMATO, sal: this.sal.toString("base64"), ...cifrar(this.chave, { itens: this.itens, nunca: this.nunca }) };
    const tmp = this.arquivo + ".tmp";
    fs.writeFileSync(tmp, JSON.stringify(pacote));
    fs.renameSync(tmp, this.arquivo);
  }

  /** Logins guardados para um site (origem como https://exemplo.com). */
  doSite(origem) {
    this.exigir();
    return this.itens.filter((i) => i.origem === origem).map(({ id, usuario, senha }) => ({ id, usuario, senha }));
  }

  /** Lista sem as senhas, para a tela de ajustes. */
  lista() {
    this.exigir();
    return this.itens.map(({ id, origem, usuario, atualizado }) => ({ id, origem, usuario, atualizado }));
  }

  revelar(id) {
    this.exigir();
    const i = this.itens.find((x) => x.id === id);
    return i ? i.senha : null;
  }

  /** Guarda ou atualiza (mesmo site + mesmo usuário). Devolve "novo", "atualizado" ou "igual". */
  salvar(origem, usuario, senha, agora = Date.now()) {
    this.exigir();
    const i = this.itens.find((x) => x.origem === origem && x.usuario === usuario);
    if (i) {
      if (i.senha === senha) return "igual";
      i.senha = senha;
      i.atualizado = agora;
      this.gravar();
      return "atualizado";
    }
    this.itens.push({ id: crypto.randomBytes(8).toString("hex"), origem, usuario, senha, atualizado: agora });
    this.gravar();
    return "novo";
  }

  apagar(id) {
    this.exigir();
    this.itens = this.itens.filter((x) => x.id !== id);
    this.gravar();
  }

  jaPerguntou(origem) { return this.nunca.includes(origem); }
  nuncaOferecer(origem) {
    this.exigir();
    if (!this.nunca.includes(origem)) { this.nunca.push(origem); this.gravar(); }
  }

  trocarSenha(atual, nova) {
    this.exigir();
    if (String(nova).length < 6) throw new Error("A senha mestra precisa ter pelo menos 6 caracteres.");
    const arq = JSON.parse(fs.readFileSync(this.arquivo, "utf8"));
    try { decifrar(derivar(atual, Buffer.from(arq.sal, "base64")), arq); } catch { throw new Error("Senha mestra atual incorreta."); }
    this.sal = crypto.randomBytes(16);
    this.chave = derivar(nova, this.sal);
    this.gravar();
  }
}

module.exports = { Cofre };

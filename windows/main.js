// Velox Search para Windows: janela com barra de abas própria e uma página (WebContentsView) por aba.
const { app, BrowserWindow, WebContentsView, ipcMain, session, shell, dialog, Menu, safeStorage, clipboard, net } = require("electron");
const path = require("path");
const fs = require("fs");
const crypto = require("crypto");
const { pathToFileURL } = require("url");
const { Cofre } = require("./cofre");
const { Bloqueio } = require("./bloqueio");
const { idDaLoja, baixarDaLoja } = require("./crx");
const sincronia = require("./sincronia");
const atualizador = require("./atualizador");

const ALTURA_BARRA = 84;
const ALTURA_FAVORITOS = 32;
const BUSCADORES = {
  google: (q) => "https://www.google.com/search?q=" + encodeURIComponent(q),
  duckduckgo: (q) => "https://duckduckgo.com/?q=" + encodeURIComponent(q),
  bing: (q) => "https://www.bing.com/search?q=" + encodeURIComponent(q),
};
const NOMES_BUSCADOR = { google: "Google", duckduckgo: "DuckDuckGo", bing: "Bing" };
const TEMA = "c1=22d3ee&c2=a78bfa&bg=070b14&sup=0d1530&campo=141e40&su=8fa0cf&tx=ffffff&br=70&fu=aurora&fo=futurista&an=1&base=noite&ti=marca&pk=linha&dim=35&tr=30";
const INTERNAS = { downloads: "downloads.html", ajustes: "ajustes.html", historico: "historico.html" };
const PERMISSOES_COM_PERGUNTA = { media: "usar a câmera ou o microfone", geolocation: "saber a sua localização", notifications: "enviar notificações", midi: "usar dispositivos MIDI", "clipboard-read": "ler a área de transferência", "display-capture": "gravar a sua tela" };
const PERMISSOES_LIBERADAS = new Set(["fullscreen", "pointerLock", "clipboard-sanitized-write", "keyboardLock"]);

let janela;
let abas = []; // { id, view, interna, titulo, url, carregando, anonima }
let ativa = null;
let proximoId = 1;
let abriuUmaVez = false;
const fechadas = [];
const downloads = [];
let cofre, bloqueio;
let tokenSync = "";
let sincronizando = false;

// ------------------------------------------------------------------ ajustes (um arquivo JSON)
const arquivoAjustes = () => path.join(app.getPath("userData"), "velox.json");
let ajustes = {
  favoritos: [], apagados: {}, fundo: "", transparencia: 30, buscador: "google", mostrarFavoritos: true, bloqueio: true, continuar: true,
  abas: [], historico: [], permissoes: {}, extensoes: [], sync: { email: "", token: "", ultimo: 0 },
};
function lerAjustes() {
  try { ajustes = { ...ajustes, ...JSON.parse(fs.readFileSync(arquivoAjustes(), "utf8")) }; } catch { /* primeira vez */ }
}
let gravando = null;
function gravarAjustes() {
  if (gravando) return;
  gravando = setTimeout(() => {
    gravando = null;
    try { fs.writeFileSync(arquivoAjustes(), JSON.stringify(ajustes)); } catch { /* sem espaço: segue */ }
  }, 400);
}
function gravarAgora() {
  if (gravando) { clearTimeout(gravando); gravando = null; }
  try { fs.writeFileSync(arquivoAjustes(), JSON.stringify(ajustes)); } catch { /* ok */ }
}

// o token da sincronização fica cifrado com o Windows (DPAPI) quando possível
const guardarToken = (t) => { try { return safeStorage.isEncryptionAvailable() ? "dp:" + safeStorage.encryptString(t).toString("base64") : "tx:" + t; } catch { return "tx:" + t; } };
const lerToken = (s) => { try { return String(s).startsWith("dp:") ? safeStorage.decryptString(Buffer.from(s.slice(3), "base64")) : String(s).slice(3); } catch { return ""; } };

// ------------------------------------------------------------------ endereços
function resolverEntrada(texto) {
  const t = String(texto || "").trim();
  if (!t) return null;
  if (/^(https?|file):\/\//i.test(t)) return t;
  if (/^[\w-]+(\.[\w-]+)+(:\d+)?(\/\S*)?$/.test(t) && !/\s/.test(t)) return "https://" + t;
  if (/^localhost(:\d+)?(\/\S*)?$/i.test(t)) return "http://" + t;
  return (BUSCADORES[ajustes.buscador] || BUSCADORES.google)(t);
}

function enderecoInicio() {
  const u = pathToFileURL(path.join(__dirname, "inicio", "inicio.html"));
  u.search = TEMA + `&n1=${bloqueio ? bloqueio.bloqueados : 0}&n2=${bloqueio ? bloqueio.bloqueados : 0}&ft=${ajustes.fundo ? 1 : 0}&ad=${ajustes.bloqueio ? 1 : 0}&bus=${encodeURIComponent(NOMES_BUSCADOR[ajustes.buscador] || "Google")}${abriuUmaVez ? "" : "&ab=1"}`;
  abriuUmaVez = true;
  return u.toString();
}
const enderecoInterno = (nome) => pathToFileURL(path.join(__dirname, "ui", INTERNAS[nome])).toString();
const origemDe = (url) => { try { const u = new URL(url); return /^https?:$/.test(u.protocol) ? u.origin : ""; } catch { return ""; } };

// ------------------------------------------------------------------ abas
const alturaBarra = () => ALTURA_BARRA + (ajustes.mostrarFavoritos && ajustes.favoritos.length ? ALTURA_FAVORITOS : 0);
function limites() {
  const [l, a] = janela.getContentSize();
  const h = alturaBarra();
  return { x: 0, y: h, width: l, height: Math.max(0, a - h) };
}
function reposicionar() { if (ativa && !janela.isFullScreen()) ativa.view.setBounds(limites()); }

let busca = { aberta: false, resultado: null };
function estadoDasAbas() {
  const at = ativa;
  return {
    ativa: at ? at.id : null,
    abas: abas.map((a) => ({ id: a.id, titulo: a.titulo || (a.interna === "inicio" ? "Início" : a.interna === "downloads" ? "Downloads" : a.interna === "ajustes" ? "Ajustes" : a.interna === "historico" ? "Histórico" : "Nova aba"), url: a.url || "", carregando: !!a.carregando, anonima: !!a.anonima })),
    voltar: !!(at && at.view.webContents.navigationHistory.canGoBack()),
    avancar: !!(at && at.view.webContents.navigationHistory.canGoForward()),
    favorita: !!(at && at.url && ajustes.favoritos.some((f) => f.u === at.url)),
    interna: at ? at.interna : null,
    fundo: ajustes.fundo ? pathToFileURL(ajustes.fundo).toString() : "",
    transparencia: ajustes.transparencia,
    buscador: ajustes.buscador,
    favoritos: ajustes.mostrarFavoritos ? ajustes.favoritos.map((f) => ({ n: f.n, u: f.u })) : [],
    bloqueio: ajustes.bloqueio,
    bloqueados: bloqueio ? bloqueio.bloqueados : 0,
    cofreAberto: !!(cofre && cofre.aberto()),
  };
}
function avisarBarra() {
  if (janela && !janela.isDestroyed()) janela.webContents.send("estado", estadoDasAbas());
  salvarAbasAbertas();
}

function salvarAbasAbertas() {
  ajustes.abas = abas.filter((a) => !a.interna && !a.anonima && /^https?:/i.test(a.url || "")).map((a) => a.url).slice(0, 40);
  gravarAjustes();
}

function criarAba(destino, opcoes = {}) {
  const interna = opcoes.interna || null;
  const especial = interna && interna !== "inicio";
  const view = new WebContentsView({
    webPreferences: {
      partition: opcoes.anonima ? "velox-anonima" : "persist:velox",
      preload: especial ? path.join(__dirname, "preload.js") : interna ? undefined : path.join(__dirname, "preload-paginas.js"),
      contextIsolation: true,
      sandbox: !especial,
      nodeIntegration: false,
    },
  });
  const aba = { id: proximoId++, view, interna, titulo: "", url: "", carregando: false, anonima: !!opcoes.anonima };
  const wc = view.webContents;
  view.setBackgroundColor(interna === "inicio" ? "#00000000" : "#ffffffff");

  wc.on("page-title-updated", (_e, t) => { aba.titulo = t; historicoTitulo(aba); avisarBarra(); });
  wc.on("did-start-loading", () => { aba.carregando = true; avisarBarra(); });
  wc.on("did-stop-loading", () => { aba.carregando = false; avisarBarra(); });
  wc.on("did-navigate", (_e, url) => { aba.url = aba.interna ? "" : url; historicoVisita(aba, url); avisarBarra(); });
  wc.on("did-navigate-in-page", (_e, url) => { if (!aba.interna) aba.url = url; avisarBarra(); });
  wc.on("will-navigate", (e, url) => {
    if (url.startsWith("velox://")) { e.preventDefault(); acaoVelox(aba, new URL(url)); }
  });
  wc.setWindowOpenHandler(({ url }) => {
    if (/^https?:/i.test(url)) criarAba(url, { anonima: aba.anonima });
    return { action: "deny" };
  });
  wc.on("found-in-page", (_e, r) => { if (aba === ativa && janela && !janela.isDestroyed()) janela.webContents.send("busca-resultado", { ordinal: r.activeMatchOrdinal, total: r.matches }); });
  wc.on("enter-html-full-screen", () => { if (aba === ativa) { janela.setFullScreen(true); view.setBounds({ x: 0, y: 0, width: janela.getContentSize()[0], height: janela.getContentSize()[1] }); } });
  wc.on("leave-html-full-screen", () => { if (aba === ativa) { janela.setFullScreen(false); setTimeout(reposicionar, 50); } });
  wc.on("context-menu", (_e, p) => {
    const itens = [];
    if (p.linkURL) itens.push({ label: "Abrir link em nova aba", click: () => criarAba(p.linkURL, { anonima: aba.anonima }) }, { label: "Copiar endereço do link", click: () => clipboard.writeText(p.linkURL) });
    if (p.srcURL && p.mediaType === "image") itens.push({ label: "Usar imagem como fundo do Velox", click: () => baixarComoFundo(p.srcURL) }, { label: "Salvar imagem", click: () => wc.downloadURL(p.srcURL) }, { label: "Copiar imagem", click: () => wc.copyImageAt(p.x, p.y) });
    if (p.selectionText) itens.push({ role: "copy", label: "Copiar" }, { label: "Pesquisar “" + p.selectionText.slice(0, 24) + "”", click: () => criarAba((BUSCADORES[ajustes.buscador] || BUSCADORES.google)(p.selectionText), { anonima: aba.anonima }) });
    if (p.isEditable) itens.push({ role: "cut", label: "Recortar" }, { role: "copy", label: "Copiar" }, { role: "paste", label: "Colar" });
    if (!itens.length) itens.push({ label: "Voltar", enabled: wc.navigationHistory.canGoBack(), click: () => wc.navigationHistory.goBack() }, { label: "Recarregar", click: () => wc.reload() });
    itens.push({ type: "separator" }, { label: "Inspecionar", click: () => wc.inspectElement(p.x, p.y) });
    Menu.buildFromTemplate(itens).popup();
  });
  wc.on("before-input-event", (e, input) => atalhos(e, input));

  abas.push(aba);
  if (especial) wc.loadURL(enderecoInterno(interna));
  else if (!destino || interna === "inicio") { aba.interna = "inicio"; view.setBackgroundColor("#00000000"); wc.loadURL(enderecoInicio()); }
  else wc.loadURL(destino);
  mostrar(aba);
  return aba;
}

function mostrar(aba) {
  if (busca.aberta) fecharBusca();
  if (ativa && ativa !== aba) janela.contentView.removeChildView(ativa.view);
  ativa = aba;
  janela.contentView.addChildView(aba.view);
  aba.view.setBounds(janela.isFullScreen() ? { x: 0, y: 0, width: janela.getContentSize()[0], height: janela.getContentSize()[1] } : limites());
  aba.view.webContents.focus();
  avisarBarra();
}

function fecharAba(id) {
  const i = abas.findIndex((a) => a.id === id);
  if (i < 0) return;
  const aba = abas[i];
  if (!aba.interna && !aba.anonima && /^https?:/i.test(aba.url)) { fechadas.push(aba.url); if (fechadas.length > 20) fechadas.shift(); }
  abas.splice(i, 1);
  if (ativa === aba) { janela.contentView.removeChildView(aba.view); ativa = null; }
  aba.view.webContents.close();
  if (!abas.length) criarAba(null, { interna: "inicio" });
  else if (!ativa) mostrar(abas[Math.min(i, abas.length - 1)]);
  else avisarBarra();
}

function acaoVelox(aba, u) {
  if (u.hostname === "buscar") {
    const destino = resolverEntrada(u.searchParams.get("q") || "");
    if (destino) { aba.interna = null; aba.view.setBackgroundColor("#ffffffff"); aba.view.webContents.loadURL(destino); }
  }
}

function irPara(texto) {
  if (!ativa) return;
  const destino = resolverEntrada(texto);
  if (!destino) return;
  if (ativa.interna && ativa.interna !== "inicio") { criarAba(destino); return; } // telas internas têm acesso especial: nunca navegam para fora
  ativa.interna = null;
  ativa.view.setBackgroundColor("#ffffffff");
  ativa.view.webContents.loadURL(destino);
}

function abrirInterna(nome) {
  const ja = abas.find((a) => a.interna === nome);
  if (ja) mostrar(ja); else criarAba(null, { interna: nome });
}

function zoom(passo) {
  if (!ativa) return;
  const wc = ativa.view.webContents;
  wc.setZoomLevel(passo === 0 ? 0 : Math.max(-3, Math.min(5, wc.getZoomLevel() + passo)));
}

function abrirBusca() { busca.aberta = true; if (janela) janela.webContents.send("abrir-busca"); janela.webContents.focus(); }
function fecharBusca() {
  busca.aberta = false;
  if (ativa) ativa.view.webContents.stopFindInPage("clearSelection");
  if (janela && !janela.isDestroyed()) janela.webContents.send("fechar-busca");
}

function atalhos(e, input) {
  if (input.type !== "keyDown") return;
  const ctrl = input.control || input.meta;
  const k = input.key.toLowerCase();
  if (ctrl && input.shift && k === "t") { e.preventDefault(); const u = fechadas.pop(); if (u) criarAba(u); }
  else if (ctrl && k === "t") { e.preventDefault(); criarAba(null, { interna: "inicio" }); }
  else if (ctrl && k === "w") { e.preventDefault(); if (ativa) fecharAba(ativa.id); }
  else if (ctrl && k === "l") { e.preventDefault(); janela.webContents.focus(); janela.webContents.send("focar-endereco"); }
  else if (ctrl && k === "f") { e.preventDefault(); abrirBusca(); }
  else if (ctrl && k === "j") { e.preventDefault(); abrirInterna("downloads"); }
  else if (ctrl && k === "h") { e.preventDefault(); abrirInterna("historico"); }
  else if (ctrl && k === "d") { e.preventDefault(); alternarFavorito(); }
  else if (ctrl && k === "p") { e.preventDefault(); if (ativa) ativa.view.webContents.print(); }
  else if (ctrl && (k === "=" || k === "+")) { e.preventDefault(); zoom(0.5); }
  else if (ctrl && k === "-") { e.preventDefault(); zoom(-0.5); }
  else if (ctrl && k === "0") { e.preventDefault(); zoom(0); }
  else if (input.key === "F5" || (ctrl && k === "r")) { e.preventDefault(); if (ativa) ativa.view.webContents.reload(); }
  else if (input.key === "F12") { e.preventDefault(); if (ativa) ativa.view.webContents.toggleDevTools(); }
  else if (input.key === "Escape" && busca.aberta) { fecharBusca(); }
  else if (input.alt && input.key === "ArrowLeft") { if (ativa) ativa.view.webContents.navigationHistory.goBack(); }
  else if (input.alt && input.key === "ArrowRight") { if (ativa) ativa.view.webContents.navigationHistory.goForward(); }
  else if (ctrl && input.key === "Tab") {
    e.preventDefault();
    const i = abas.indexOf(ativa);
    mostrar(abas[(i + (input.shift ? abas.length - 1 : 1)) % abas.length]);
  }
}

// ------------------------------------------------------------------ favoritos
function alternarFavorito() {
  if (!ativa || !ativa.url || !/^https?:/i.test(ativa.url)) return;
  const i = ajustes.favoritos.findIndex((f) => f.u === ativa.url);
  const agora = Date.now();
  if (i >= 0) { ajustes.apagados[ativa.url] = agora; ajustes.favoritos.splice(i, 1); }
  else { delete ajustes.apagados[ativa.url]; ajustes.favoritos.push({ n: (ativa.titulo || ativa.url).slice(0, 40), u: ativa.url, t: agora }); }
  gravarAjustes();
  reposicionar();
  avisarBarra();
  sincronizarDepois();
}

// ------------------------------------------------------------------ sincronização com o Vetorial ID
let relogioSync = null;
function sincronizarDepois(ms = 3000) {
  if (!tokenSync) return;
  clearTimeout(relogioSync);
  relogioSync = setTimeout(() => { sincronizarAgora().catch(() => undefined); }, ms);
}
async function sincronizarAgora() {
  if (!tokenSync || sincronizando) return { ok: false, erro: "Entre com o Vetorial ID para sincronizar." };
  sincronizando = true;
  try {
    const r = await sincronia.sincronizar(tokenSync, { favoritos: ajustes.favoritos, apagados: ajustes.apagados });
    if (r.mudouLocal) { ajustes.favoritos = r.doc.favoritos; ajustes.apagados = r.doc.apagados; reposicionar(); }
    ajustes.sync.ultimo = Date.now();
    gravarAjustes();
    avisarBarra();
    return { ok: true, total: ajustes.favoritos.length };
  } catch (e) {
    if (e.status === 401) { tokenSync = ""; ajustes.sync.token = ""; gravarAjustes(); return { ok: false, erro: "A sessão do Vetorial ID acabou. Entre de novo." }; }
    return { ok: false, erro: e.message || "Não consegui sincronizar agora." };
  } finally { sincronizando = false; }
}

// ------------------------------------------------------------------ histórico
function historicoVisita(aba, url) {
  if (aba.anonima || aba.interna || !/^https?:/i.test(url)) return;
  const h = ajustes.historico;
  if (h.length && h[0].u === url) { h[0].t = Date.now(); return; }
  h.unshift({ u: url, n: aba.titulo || url, t: Date.now() });
  if (h.length > 3000) h.length = 3000;
  gravarAjustes();
}
function historicoTitulo(aba) {
  if (aba.anonima || aba.interna || !ajustes.historico.length) return;
  const e = ajustes.historico.find((x) => x.u === aba.url);
  if (e && aba.titulo) { e.n = aba.titulo; gravarAjustes(); }
}

// ------------------------------------------------------------------ fundo (wallpaper)
function guardarFundo(origem) {
  try {
    const destino = path.join(app.getPath("userData"), "fundo" + (path.extname(origem) || ".jpg"));
    if (origem !== destino) fs.copyFileSync(origem, destino);
    ajustes.fundo = destino;
    gravarAjustes();
    recarregarInicios();
    avisarBarra();
  } catch { /* arquivo não abriu */ }
}
async function baixarComoFundo(url) {
  try {
    const r = await fetch(url);
    if (!r.ok) throw new Error("http " + r.status);
    const tipo = (r.headers.get("content-type") || "").split(";")[0];
    const ext = tipo === "image/png" ? ".png" : tipo === "image/webp" ? ".webp" : ".jpg";
    const tmp = path.join(app.getPath("temp"), "velox-fundo" + ext);
    fs.writeFileSync(tmp, Buffer.from(await r.arrayBuffer()));
    guardarFundo(tmp);
  } catch { dialog.showErrorBox("Velox", "Essa imagem não abriu. Tente outra."); }
}
function recarregarInicios() {
  for (const a of abas) if (a.interna === "inicio") a.view.webContents.loadURL(enderecoInicio());
}

// ------------------------------------------------------------------ downloads
function ligarDownloads(ses) {
  ses.on("will-download", (_e, item) => {
    const caminho = path.join(app.getPath("downloads"), item.getFilename());
    item.setSavePath(caminho);
    const d = { id: Date.now() + Math.random(), nome: item.getFilename(), caminho, total: item.getTotalBytes(), recebido: 0, estado: "baixando", velocidade: 0, item };
    let ultimo = { t: Date.now(), b: 0 };
    downloads.unshift(d);
    item.on("updated", (_ev, estado) => {
      d.recebido = item.getReceivedBytes();
      d.total = item.getTotalBytes() || d.total;
      const agora = Date.now();
      if (agora - ultimo.t >= 700) { d.velocidade = ((d.recebido - ultimo.b) * 1000) / (agora - ultimo.t); ultimo = { t: agora, b: d.recebido }; }
      d.estado = estado === "interrupted" ? "interrompido" : item.isPaused() ? "pausado" : "baixando";
      avisarDownloads();
    });
    item.once("done", (_ev, estado) => {
      d.estado = estado === "completed" ? "pronto" : estado === "cancelled" ? "cancelado" : "falhou";
      d.velocidade = 0;
      avisarDownloads();
    });
    avisarDownloads();
    abrirInterna("downloads");
  });
}
const resumoDownloads = () => downloads.map(({ item, ...d }) => d);
function avisarDownloads() {
  for (const a of abas) if (a.interna === "downloads" && !a.view.webContents.isDestroyed()) a.view.webContents.send("downloads", resumoDownloads());
}

// ------------------------------------------------------------------ caixas de diálogo próprias
let proximoDialogo = 1;
/** Abre uma janelinha modal e devolve { botao, valores }. */
function perguntar(def) {
  return new Promise((resolve) => {
    const id = proximoDialogo++;
    const j = new BrowserWindow({
      parent: janela, modal: true, width: 440, height: def.altura || 280, resizable: false, minimizable: false, maximizable: false, show: false,
      backgroundColor: "#0d1530", title: def.titulo || "Velox", autoHideMenuBar: true,
      webPreferences: { preload: path.join(__dirname, "preload.js"), contextIsolation: true, nodeIntegration: false },
    });
    j.setMenuBarVisibility(false);
    let respondido = false;
    const canal = `dialogo:${id}`;
    const fim = (r) => { if (respondido) return; respondido = true; ipcMain.removeAllListeners(canal); if (!j.isDestroyed()) j.destroy(); resolve(r); };
    ipcMain.once(canal, (_e, r) => fim(r));
    j.on("closed", () => fim({ botao: "fechar", valores: {} }));
    j.once("ready-to-show", () => j.show());
    j.loadFile(path.join(__dirname, "ui", "dialogo.html"), { hash: encodeURIComponent(JSON.stringify({ ...def, id })) });
  });
}

// ------------------------------------------------------------------ senhas
async function desbloquear(motivo) {
  if (cofre.aberto()) return true;
  for (let t = 0; t < 3; t++) {
    const r = await perguntar({ titulo: "Cofre de senhas", texto: motivo || "Digite a senha mestra para abrir o cofre de senhas do Velox.", altura: 260, campos: [{ id: "senha", tipo: "password", rotulo: "Senha mestra" }], botoes: [{ id: "ok", rotulo: "Abrir", primario: true }, { id: "cancelar", rotulo: "Cancelar" }] });
    if (r.botao !== "ok") return false;
    try { cofre.abrir(r.valores.senha || ""); avisarBarra(); return true; } catch (e) { motivo = e.message + " Tente de novo."; }
  }
  return false;
}
async function criarCofre() {
  const r = await perguntar({ titulo: "Criar o cofre de senhas", texto: "Crie uma senha mestra. Ela protege todas as suas senhas neste computador e não pode ser recuperada se você esquecer.", altura: 340, campos: [{ id: "senha", tipo: "password", rotulo: "Senha mestra (mínimo 6 caracteres)" }, { id: "conf", tipo: "password", rotulo: "Repita a senha mestra" }], botoes: [{ id: "ok", rotulo: "Criar cofre", primario: true }, { id: "cancelar", rotulo: "Agora não" }] });
  if (r.botao !== "ok") return false;
  if (r.valores.senha !== r.valores.conf) { dialog.showErrorBox("Velox", "As senhas mestras não são iguais."); return false; }
  try { cofre.criar(r.valores.senha); avisarBarra(); return true; } catch (e) { dialog.showErrorBox("Velox", e.message); return false; }
}

async function aoCapturarSenha(wc, d) {
  const aba = abas.find((a) => a.view.webContents === wc);
  if (!aba || aba.anonima) return;
  const origem = origemDe(wc.getURL());
  if (!origem || origem !== d.origem || !d.senha) return;
  const host = new URL(origem).host;
  if (!cofre.existe()) {
    if (!(await criarCofre())) return;
  } else if (!cofre.aberto()) {
    if (!(await desbloquear(`Digite a senha mestra para salvar o login de ${host}.`))) return;
  }
  if (cofre.jaPerguntou(origem)) return;
  const existente = cofre.doSite(origem).find((x) => x.usuario === d.usuario);
  if (existente && existente.senha === d.senha) return;
  const r = await perguntar({
    titulo: "Salvar senha?", altura: 250,
    texto: existente ? `Atualizar a senha de “${d.usuario || "(sem usuário)"}” em ${host}?` : `Salvar o login de “${d.usuario || "(sem usuário)"}” em ${host}?`,
    botoes: [{ id: "salvar", rotulo: existente ? "Atualizar" : "Salvar", primario: true }, { id: "agora", rotulo: "Agora não" }, { id: "nunca", rotulo: "Nunca neste site" }],
  });
  if (r.botao === "salvar") { try { cofre.salvar(origem, d.usuario, d.senha); } catch { /* cofre trancou no meio */ } }
  else if (r.botao === "nunca") { try { cofre.nuncaOferecer(origem); } catch { /* ok */ } }
}

async function preencherAtual() {
  if (!ativa || ativa.interna) return;
  const origem = origemDe(ativa.view.webContents.getURL());
  if (!origem) return;
  if (!cofre.existe()) { if (!(await criarCofre())) return; }
  else if (!cofre.aberto() && !(await desbloquear())) return;
  const logins = cofre.doSite(origem);
  if (!logins.length) { dialog.showMessageBox(janela, { message: "Não há senha salva para este site.", buttons: ["OK"] }); return; }
  let escolhido = logins[0];
  if (logins.length > 1) {
    const r = await dialog.showMessageBox(janela, { message: "Qual login usar?", buttons: [...logins.map((l) => l.usuario || "(sem usuário)"), "Cancelar"], cancelId: logins.length });
    if (r.response >= logins.length) return;
    escolhido = logins[r.response];
  }
  ativa.view.webContents.send("senha:preencher", { usuario: escolhido.usuario, senha: escolhido.senha });
}

// ------------------------------------------------------------------ permissões por site
function ligarPermissoes(ses) {
  ses.setPermissionRequestHandler((wc, perm, resposta, detalhes) => {
    if (PERMISSOES_LIBERADAS.has(perm)) return resposta(true);
    const rotulo = PERMISSOES_COM_PERGUNTA[perm];
    const origem = origemDe(detalhes.requestingUrl || (wc && wc.getURL()) || "");
    if (!rotulo || !origem) return resposta(false);
    const guardado = (ajustes.permissoes[origem] || {})[perm];
    if (guardado) return resposta(guardado === "permitir");
    perguntar({ titulo: "Permissão do site", altura: 270, texto: `${new URL(origem).host} quer ${rotulo}.`, botoes: [{ id: "permitir", rotulo: "Permitir", primario: true }, { id: "bloquear", rotulo: "Bloquear" }] }).then((r) => {
      const ok = r.botao === "permitir";
      if (r.botao === "permitir" || r.botao === "bloquear") { (ajustes.permissoes[origem] = ajustes.permissoes[origem] || {})[perm] = r.botao; gravarAjustes(); }
      resposta(ok);
    });
  });
  ses.setPermissionCheckHandler((_wc, perm, origemDoPedido) => {
    if (PERMISSOES_LIBERADAS.has(perm)) return true;
    const origem = origemDe(origemDoPedido || "");
    return !!origem && (ajustes.permissoes[origem] || {})[perm] === "permitir";
  });
}

// ------------------------------------------------------------------ extensões
const sessaoPrincipal = () => session.fromPartition("persist:velox");
async function carregarExtensao(caminho) {
  const ses = sessaoPrincipal();
  const abrir = ses.extensions && ses.extensions.loadExtension ? (p) => ses.extensions.loadExtension(p, { allowFileAccess: true }) : (p) => ses.loadExtension(p, { allowFileAccess: true });
  return abrir(caminho);
}
async function descarregarExtensao(caminho) {
  const ses = sessaoPrincipal();
  const todas = ses.extensions && ses.extensions.getAllExtensions ? ses.extensions.getAllExtensions() : ses.getAllExtensions();
  const e = todas.find((x) => path.resolve(x.path) === path.resolve(caminho));
  if (!e) return;
  if (ses.extensions && ses.extensions.removeExtension) ses.extensions.removeExtension(e.id); else ses.removeExtension(e.id);
}
async function ligarExtensoes() {
  for (const e of ajustes.extensoes) {
    if (!e.ligada) continue;
    try { await carregarExtensao(e.caminho); } catch (erro) { e.erro = String(erro && erro.message ? erro.message : erro); }
  }
}
const pastaDasExtensoes = () => path.join(app.getPath("userData"), "extensoes");

// ------------------------------------------------------------------ comandos vindos das telas
const resumoAjustes = () => ({
  bloqueio: ajustes.bloqueio, bloqueados: bloqueio ? bloqueio.bloqueados : 0, modoBloqueio: bloqueio ? bloqueio.modo : "desligado",
  continuar: ajustes.continuar, mostrarFavoritos: ajustes.mostrarFavoritos, buscador: ajustes.buscador,
  cofre: { existe: cofre.existe(), aberto: cofre.aberto(), itens: cofre.aberto() ? cofre.lista() : [] },
  extensoes: ajustes.extensoes.map(({ id, nome, ligada, erro }) => ({ id, nome, ligada, erro: erro || "" })),
  sites: Object.entries(ajustes.permissoes).map(([origem, p]) => ({ origem, permissoes: p })),
  sync: { email: ajustes.sync.email, entrou: !!tokenSync, ultimo: ajustes.sync.ultimo, favoritos: ajustes.favoritos.length },
  versao: app.getVersion(),
});
const tente = async (f) => { try { return { ok: true, ...(await f()) }; } catch (e) { return { ok: false, erro: e && e.message ? e.message : String(e) }; } };
const dasInternas = (e) => { const a = abas.find((x) => x.view.webContents === e.sender); return !!(a && a.interna && a.interna !== "inicio"); };
const so = (fn) => (e, ...a) => (dasInternas(e) ? fn(e, ...a) : undefined);

function ligarComandos() {
  // barra (janela principal)
  const daBarra = (fn) => (e, ...a) => (janela && e.sender === janela.webContents ? fn(e, ...a) : undefined);
  // script que corta os anúncios do YouTube: só vai para páginas do youtube e só com o bloqueio ligado
  let scriptYoutube = null;
  ipcMain.on("yt:script", (e) => {
    e.returnValue = "";
    try {
      const h = new URL(e.senderFrame ? e.senderFrame.url : e.sender.getURL()).hostname;
      if (!ajustes.bloqueio || !(h === "youtube.com" || h.endsWith(".youtube.com"))) return;
      if (scriptYoutube === null) scriptYoutube = fs.readFileSync(path.join(__dirname, "inicio", "youtube.js"), "utf8");
      e.returnValue = scriptYoutube;
    } catch (err) { /* sem script */ }
  });
  ipcMain.on("nova-aba", daBarra(() => criarAba(null, { interna: "inicio" })));
  ipcMain.on("nova-anonima", daBarra(() => criarAba(null, { interna: "inicio", anonima: true })));
  ipcMain.on("fechar-aba", daBarra((_e, id) => fecharAba(id)));
  ipcMain.on("mostrar-aba", daBarra((_e, id) => { const a = abas.find((x) => x.id === id); if (a) mostrar(a); }));
  ipcMain.on("ir", daBarra((_e, texto) => irPara(texto)));
  ipcMain.on("voltar", daBarra(() => ativa && ativa.view.webContents.navigationHistory.goBack()));
  ipcMain.on("avancar", daBarra(() => ativa && ativa.view.webContents.navigationHistory.goForward()));
  ipcMain.on("recarregar", daBarra(() => ativa && (ativa.carregando ? ativa.view.webContents.stop() : ativa.view.webContents.reload())));
  ipcMain.on("inicio", daBarra(() => { if (ativa) { ativa.interna = "inicio"; ativa.view.setBackgroundColor("#00000000"); ativa.view.webContents.loadURL(enderecoInicio()); } }));
  ipcMain.on("favorito", daBarra(alternarFavorito));
  ipcMain.on("abrir-interna", daBarra((_e, nome) => { if (INTERNAS[nome]) abrirInterna(nome); }));
  ipcMain.on("escolher-fundo", daBarra(async () => {
    const r = await dialog.showOpenDialog(janela, { title: "Escolher fundo do Velox", properties: ["openFile"], filters: [{ name: "Imagens", extensions: ["jpg", "jpeg", "png", "webp", "bmp"] }] });
    if (!r.canceled && r.filePaths[0]) guardarFundo(r.filePaths[0]);
  }));
  ipcMain.on("remover-fundo", daBarra(() => { ajustes.fundo = ""; gravarAjustes(); recarregarInicios(); avisarBarra(); }));
  ipcMain.on("transparencia", daBarra((_e, v) => { ajustes.transparencia = Math.max(0, Math.min(100, Number(v) || 0)); gravarAjustes(); avisarBarra(); }));
  ipcMain.on("buscador", daBarra((_e, id) => { if (BUSCADORES[id]) { ajustes.buscador = id; gravarAjustes(); recarregarInicios(); avisarBarra(); } }));
  ipcMain.on("barra-pronta", daBarra(avisarBarra));
  ipcMain.on("chave", daBarra(() => { preencherAtual().catch(() => undefined); }));
  ipcMain.on("busca", daBarra((_e, { texto, proximo }) => {
    if (!ativa || !texto) return;
    ativa.view.webContents.findInPage(texto, { forward: proximo !== false, findNext: true });
  }));
  ipcMain.on("busca-fechar", daBarra(() => fecharBusca()));
  ipcMain.on("abrir-favorito", daBarra((_e, u) => { if (/^https?:/i.test(u)) irPara(u); }));

  // páginas das web: formulários de login
  ipcMain.on("senha:capturada", (e, d) => { if (d && typeof d.senha === "string") aoCapturarSenha(e.sender, d).catch(() => undefined); });
  ipcMain.on("senha:pagina-pronta", () => undefined);

  // telas internas (downloads, ajustes, histórico)
  ipcMain.handle("downloads-lista", so(() => resumoDownloads()));
  ipcMain.on("download-abrir", so((_e, id) => { const d = downloads.find((x) => x.id === id); if (d) shell.openPath(d.caminho); }));
  ipcMain.on("download-pasta", so((_e, id) => { const d = downloads.find((x) => x.id === id); if (d) shell.showItemInFolder(d.caminho); }));
  ipcMain.on("download-cancelar", so((_e, id) => { const d = downloads.find((x) => x.id === id); if (d && d.item && d.estado === "baixando") d.item.cancel(); }));
  ipcMain.on("download-limpar", so(() => { for (let i = downloads.length - 1; i >= 0; i--) if (downloads[i].estado !== "baixando") downloads.splice(i, 1); avisarDownloads(); }));

  ipcMain.handle("ajustes:estado", so(() => resumoAjustes()));
  ipcMain.handle("ajustes:bloqueio", so((_e, v) => { ajustes.bloqueio = !!v; bloqueio.definir(v); gravarAjustes(); avisarBarra(); return resumoAjustes(); }));
  ipcMain.handle("ajustes:continuar", so((_e, v) => { ajustes.continuar = !!v; gravarAjustes(); return resumoAjustes(); }));
  ipcMain.handle("ajustes:favoritos-barra", so((_e, v) => { ajustes.mostrarFavoritos = !!v; gravarAjustes(); reposicionar(); avisarBarra(); return resumoAjustes(); }));

  ipcMain.handle("cofre:criar", so(() => tente(async () => { const ok = await criarCofre(); if (!ok) throw new Error("O cofre não foi criado."); return {}; })));
  ipcMain.handle("cofre:abrir", so(() => tente(async () => { if (!(await desbloquear())) throw new Error("O cofre continua trancado."); return {}; })));
  ipcMain.handle("cofre:travar", so(() => { cofre.travar(); avisarBarra(); return resumoAjustes(); }));
  ipcMain.handle("cofre:revelar", so((_e, id) => tente(async () => ({ senha: cofre.revelar(id) }))));
  ipcMain.handle("cofre:copiar", so((_e, id) => tente(async () => { const s = cofre.revelar(id); clipboard.writeText(s || ""); setTimeout(() => { if (clipboard.readText() === s) clipboard.clear(); }, 30_000); return {}; })));
  ipcMain.handle("cofre:apagar", so((_e, id) => tente(async () => { cofre.apagar(id); return {}; })));
  ipcMain.handle("cofre:trocar", so(() => tente(async () => {
    const r = await perguntar({ titulo: "Trocar a senha mestra", altura: 360, texto: "Digite a senha mestra atual e a nova.", campos: [{ id: "atual", tipo: "password", rotulo: "Senha mestra atual" }, { id: "nova", tipo: "password", rotulo: "Nova senha mestra" }, { id: "conf", tipo: "password", rotulo: "Repita a nova" }], botoes: [{ id: "ok", rotulo: "Trocar", primario: true }, { id: "cancelar", rotulo: "Cancelar" }] });
    if (r.botao !== "ok") throw new Error("Nada foi alterado.");
    if (r.valores.nova !== r.valores.conf) throw new Error("As senhas novas não são iguais.");
    cofre.trocarSenha(r.valores.atual, r.valores.nova);
    return {};
  })));

  ipcMain.handle("ext:instalar", so((_e, texto) => tente(async () => {
    const id = idDaLoja(texto);
    if (!id) throw new Error("Cole o ID (32 letras) ou o endereço da extensão na Chrome Web Store.");
    const pasta = path.join(pastaDasExtensoes(), id);
    await baixarDaLoja(id, pasta);
    const ext = await carregarExtensao(pasta);
    ajustes.extensoes = ajustes.extensoes.filter((x) => x.caminho !== pasta);
    ajustes.extensoes.push({ id, nome: ext.name || id, caminho: pasta, ligada: true });
    gravarAjustes();
    return { nome: ext.name };
  })));
  ipcMain.handle("ext:pasta", so(() => tente(async () => {
    const r = await dialog.showOpenDialog(janela, { title: "Escolha a pasta da extensão (a que tem o manifest.json)", properties: ["openDirectory"] });
    if (r.canceled || !r.filePaths[0]) throw new Error("Nenhuma pasta escolhida.");
    const pasta = r.filePaths[0];
    const ext = await carregarExtensao(pasta);
    ajustes.extensoes = ajustes.extensoes.filter((x) => x.caminho !== pasta);
    ajustes.extensoes.push({ id: path.basename(pasta), nome: ext.name || path.basename(pasta), caminho: pasta, ligada: true });
    gravarAjustes();
    return { nome: ext.name };
  })));
  ipcMain.handle("ext:alternar", so((_e, id) => tente(async () => {
    const e = ajustes.extensoes.find((x) => x.id === id);
    if (!e) throw new Error("Extensão não encontrada.");
    if (e.ligada) { await descarregarExtensao(e.caminho); e.ligada = false; }
    else { await carregarExtensao(e.caminho); e.ligada = true; e.erro = ""; }
    gravarAjustes();
    return {};
  })));
  ipcMain.handle("ext:remover", so((_e, id) => tente(async () => {
    const e = ajustes.extensoes.find((x) => x.id === id);
    if (!e) return {};
    await descarregarExtensao(e.caminho).catch(() => undefined);
    ajustes.extensoes = ajustes.extensoes.filter((x) => x !== e);
    if (e.caminho.startsWith(pastaDasExtensoes())) fs.rmSync(e.caminho, { recursive: true, force: true });
    gravarAjustes();
    return {};
  })));

  ipcMain.handle("sites:remover", so((_e, origem) => { delete ajustes.permissoes[origem]; gravarAjustes(); return resumoAjustes(); }));

  ipcMain.handle("sync:entrar", so((_e, email, senha) => tente(async () => {
    const r = await sincronia.entrar(String(email || "").trim(), String(senha || ""), { id: ajustes.aparelho, nome: "Velox (Windows)", plataforma: "windows" });
    tokenSync = r.token;
    ajustes.sync = { email: r.email, token: guardarToken(r.token), ultimo: 0 };
    gravarAjustes();
    const s = await sincronizarAgora();
    if (!s.ok) throw new Error(s.erro);
    return {};
  })));
  ipcMain.handle("sync:sair", so(() => { tokenSync = ""; ajustes.sync = { email: "", token: "", ultimo: 0 }; gravarAjustes(); return resumoAjustes(); }));
  ipcMain.handle("sync:agora", so(() => sincronizarAgora()));

  ipcMain.handle("historico:lista", so((_e, texto) => {
    const t = String(texto || "").toLowerCase();
    return ajustes.historico.filter((h) => !t || h.u.toLowerCase().includes(t) || (h.n || "").toLowerCase().includes(t)).slice(0, 300);
  }));
  ipcMain.on("historico:limpar", so(() => { ajustes.historico = []; gravarAjustes(); }));
  ipcMain.on("historico:apagar", so((_e, u) => { ajustes.historico = ajustes.historico.filter((h) => h.u !== u); gravarAjustes(); }));
  ipcMain.on("historico:abrir", so((_e, u) => { if (/^https?:/i.test(u)) criarAba(u); }));
}

// ------------------------------------------------------------------ início
async function iniciar() {
  lerAjustes();
  if (!ajustes.aparelho) { ajustes.aparelho = "velox-win-" + crypto.randomBytes(8).toString("hex"); gravarAgora(); }
  if (ajustes.sync && ajustes.sync.token) tokenSync = lerToken(ajustes.sync.token);
  cofre = new Cofre(path.join(app.getPath("userData"), "cofre.json"));
  cofre.aoTravar = () => avisarBarra();
  bloqueio = new Bloqueio(path.join(app.getPath("userData"), "bloqueio"));
  bloqueio.ligado = ajustes.bloqueio;
  bloqueio.aoMudar = () => { if (!bloqueio.agendado) { bloqueio.agendado = setTimeout(() => { bloqueio.agendado = null; avisarBarra(); }, 1000); } };

  janela = new BrowserWindow({
    icon: path.join(__dirname, "build", "icon.png"),
    width: 1280, height: 800, minWidth: 720, minHeight: 480,
    backgroundColor: "#070b14", title: "Velox", autoHideMenuBar: true,
    webPreferences: { preload: path.join(__dirname, "preload.js"), contextIsolation: true, nodeIntegration: false },
  });
  janela.setMenuBarVisibility(false);
  janela.loadFile(path.join(__dirname, "ui", "index.html"));
  janela.on("resize", reposicionar);
  janela.on("maximize", () => setTimeout(reposicionar, 30));
  janela.on("unmaximize", () => setTimeout(reposicionar, 30));
  janela.on("leave-full-screen", () => setTimeout(reposicionar, 50));
  janela.on("close", gravarAgora);

  const principal = sessaoPrincipal();
  const anonima = session.fromPartition("velox-anonima");
  ligarDownloads(principal); ligarDownloads(anonima);
  ligarPermissoes(principal); ligarPermissoes(anonima);
  ligarComandos();

  janela.webContents.once("did-finish-load", async () => {
    const abrir = process.argv.slice(1).find((a) => /^https?:\/\//i.test(a));
    if (abrir) criarAba(abrir);
    else if (ajustes.continuar && ajustes.abas.length) { for (const u of ajustes.abas) criarAba(u); mostrar(abas[abas.length - 1]); }
    else criarAba(null, { interna: "inicio" });
    // os recursos pesados carregam depois de a janela aparecer
    bloqueio.iniciar([principal, anonima]).then(avisarBarra).catch(() => undefined);
    ligarExtensoes().catch(() => undefined);
    sincronizarDepois(1500);
    setInterval(() => sincronizarDepois(100), 5 * 60_000);
    setTimeout(() => procurarAtualizacao().catch(() => undefined), 20_000);
    setInterval(() => procurarAtualizacao().catch(() => undefined), 6 * 3600_000);
  });
  janela.on("closed", () => { janela = null; });
}


// ---------------------------------------------------------------- atualização automática
let atualizando = false;
let versaoAvisada = 0;

async function procurarAtualizacao() {
  if (atualizando || !app.isPackaged || process.env.VELOX_TESTE === "1") return;
  atualizando = true;
  try {
    const r = await net.fetch(atualizador.ENDERECO, { headers: { accept: "application/json" } });
    if (!r.ok) return;
    const d = atualizador.decidir(app.getVersion(), await r.json());
    if (d.acao === "nada" || d.numero <= versaoAvisada) return;
    const portatil = !!process.env.PORTABLE_EXECUTABLE_FILE;
    if (d.acao === "avisar" || portatil) {
      versaoAvisada = d.numero;
      const o = await dialog.showMessageBox(janela, { type: "info", buttons: ["Baixar agora", "Depois"], defaultId: 0, cancelId: 1, title: "Velox", message: `Há uma versão nova do Velox (1.${d.numero}).`, detail: portatil ? "Você usa a versão portátil: baixe a nova e substitua o arquivo." : "Toque em Baixar agora para pegar o instalador." });
      if (o.response === 0) shell.openExternal(atualizador.DOWNLOAD);
      return;
    }
    const destino = path.join(app.getPath("temp"), "VeloxSetup-atualizacao.exe");
    const b = await net.fetch(d.url);
    if (!b.ok) return;
    const dados = Buffer.from(await b.arrayBuffer());
    if (atualizador.sha256DoArquivo(dados) !== d.sha256) { console.error("Atualização recusada: o SHA-256 não bate."); return; }
    fs.writeFileSync(destino, dados);
    versaoAvisada = d.numero;
    const o = await dialog.showMessageBox(janela, { type: "question", buttons: ["Atualizar agora", "Depois"], defaultId: 0, cancelId: 1, title: "Velox", message: `A versão 1.${d.numero} do Velox está pronta.`, detail: "O Velox fecha, instala a atualização e abre de novo. Suas abas, favoritos e senhas continuam como estão." });
    if (o.response !== 0) { versaoAvisada = 0; return; }
    const { spawn } = require("child_process");
    spawn("powershell", ["-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass", "-Command", atualizador.ROTEIRO], {
      detached: true, stdio: "ignore", windowsHide: true,
      env: { ...process.env, V_PID: String(process.pid), V_SETUP: destino, V_APP: process.execPath },
    }).unref();
    gravarAgora();
    app.quit();
  } catch (e) {
    console.error("Atualização:", e && e.message ? e.message : e);
  } finally {
    atualizando = false;
  }
}

// teste de fumaça no CI: abre o Velox de verdade, espera carregar e sai com código 0 (ou 1 se algo quebrar)
const TESTE = process.env.VELOX_TESTE === "1";
if (TESTE) {
  app.setPath("userData", path.join(require("os").tmpdir(), "velox-teste-" + process.pid));
  process.on("uncaughtException", (e) => { console.error("ERRO NÃO TRATADO:", e && e.stack ? e.stack : e); app.exit(1); });
  app.on("render-process-gone", (_e, _w, d) => { console.error("PROCESSO DA PÁGINA CAIU:", d && d.reason); app.exit(1); });
  setTimeout(() => {
    const ok = abas.length > 0 && janela && !janela.isDestroyed() && ativa && !ativa.view.webContents.isDestroyed();
    console.log(ok ? `TESTE OK: ${abas.length} aba(s), bloqueio=${bloqueio && bloqueio.modo}` : "TESTE FALHOU: sem abas");
    app.exit(ok ? 0 : 1);
  }, 25_000);
}

const unica = app.requestSingleInstanceLock();
if (!unica) app.quit();
else {
  app.on("second-instance", (_e, linha) => {
    if (janela) { if (janela.isMinimized()) janela.restore(); janela.focus(); }
    const abrir = linha.slice(1).find((a) => /^https?:\/\//i.test(a));
    if (abrir) criarAba(abrir);
  });
  app.whenReady().then(iniciar);
  app.on("window-all-closed", () => app.quit());
}

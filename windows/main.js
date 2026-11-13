// Velox Search para Windows: janela com barra de abas própria e uma página (WebContentsView) por aba.
const { app, BrowserWindow, WebContentsView, ipcMain, session, shell, dialog, Menu, nativeImage } = require("electron");
const path = require("path");
const fs = require("fs");
const { pathToFileURL } = require("url");

const ALTURA_BARRA = 84;
const BUSCADORES = {
  google: (q) => "https://www.google.com/search?q=" + encodeURIComponent(q),
  duckduckgo: (q) => "https://duckduckgo.com/?q=" + encodeURIComponent(q),
  bing: (q) => "https://www.bing.com/search?q=" + encodeURIComponent(q),
};
const NOMES_BUSCADOR = { google: "Google", duckduckgo: "DuckDuckGo", bing: "Bing" };
const TEMA = "c1=22d3ee&c2=a78bfa&bg=070b14&sup=0d1530&campo=141e40&su=8fa0cf&tx=ffffff&br=70&fu=aurora&fo=futurista&an=1&base=noite&ti=marca&pk=linha&dim=35&tr=30";

let janela;
let abas = []; // { id, view, interna: "inicio" | "downloads" | null }
let ativa = null;
let proximoId = 1;
let abriuUmaVez = false;
const downloads = []; // { id, nome, caminho, total, recebido, estado, item }

// ------------------------------------------------------------------ ajustes (um arquivo JSON)
const arquivoAjustes = () => path.join(app.getPath("userData"), "velox.json");
let ajustes = { favoritos: [], fundo: "", transparencia: 30, buscador: "google", ultimaPasta: "" };
function lerAjustes() {
  try { ajustes = { ...ajustes, ...JSON.parse(fs.readFileSync(arquivoAjustes(), "utf8")) }; } catch { /* primeira vez */ }
}
function gravarAjustes() {
  try { fs.writeFileSync(arquivoAjustes(), JSON.stringify(ajustes)); } catch { /* sem espaço: segue */ }
}

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
  const bloqueados = 0;
  u.search = TEMA + `&n1=${bloqueados}&n2=${bloqueados}&ft=${ajustes.fundo ? 1 : 0}&ad=0&bus=${encodeURIComponent(NOMES_BUSCADOR[ajustes.buscador] || "Google")}${abriuUmaVez ? "" : "&ab=1"}`;
  abriuUmaVez = true;
  return u.toString();
}
const enderecoDownloads = () => pathToFileURL(path.join(__dirname, "ui", "downloads.html")).toString();

// ------------------------------------------------------------------ abas
function limites() {
  const [l, a] = janela.getContentSize();
  return { x: 0, y: ALTURA_BARRA, width: l, height: Math.max(0, a - ALTURA_BARRA) };
}

function estadoDasAbas() {
  return {
    ativa: ativa ? ativa.id : null,
    abas: abas.map((a) => ({ id: a.id, titulo: a.titulo || (a.interna === "inicio" ? "Início" : a.interna === "downloads" ? "Downloads" : "Nova aba"), url: a.url || "", carregando: !!a.carregando })),
    voltar: !!(ativa && ativa.view.webContents.navigationHistory.canGoBack()),
    avancar: !!(ativa && ativa.view.webContents.navigationHistory.canGoForward()),
    favorita: !!(ativa && ativa.url && ajustes.favoritos.some((f) => f.u === ativa.url)),
    interna: ativa ? ativa.interna : null,
    fundo: ajustes.fundo ? pathToFileURL(ajustes.fundo).toString() : "",
    transparencia: ajustes.transparencia,
    buscador: ajustes.buscador,
  };
}
function avisarBarra() {
  if (janela && !janela.isDestroyed()) janela.webContents.send("estado", estadoDasAbas());
}

function criarAba(destino, opcoes = {}) {
  const interna = opcoes.interna || null;
  const view = new WebContentsView({
    webPreferences: {
      partition: opcoes.anonima ? "velox-anonima" : "persist:velox",
      preload: interna === "downloads" ? path.join(__dirname, "preload.js") : undefined,
      contextIsolation: true,
      sandbox: interna !== "downloads",
      nodeIntegration: false,
    },
  });
  const aba = { id: proximoId++, view, interna, titulo: "", url: "", carregando: false, anonima: !!opcoes.anonima };
  const wc = view.webContents;
  // a página inicial é transparente para mostrar o wallpaper por trás
  view.setBackgroundColor(interna === "inicio" ? "#00000000" : "#ffffffff");

  wc.on("page-title-updated", (_e, t) => { aba.titulo = t; avisarBarra(); });
  wc.on("did-start-loading", () => { aba.carregando = true; avisarBarra(); });
  wc.on("did-stop-loading", () => { aba.carregando = false; avisarBarra(); });
  wc.on("did-navigate", (_e, url) => { aba.url = interna === "inicio" ? "" : url; if (!aba.interna || !url.startsWith("file:")) aba.interna = aba.interna; avisarBarra(); });
  wc.on("did-navigate-in-page", (_e, url) => { aba.url = url; avisarBarra(); });
  wc.on("will-navigate", (e, url) => {
    if (url.startsWith("velox://")) { e.preventDefault(); acaoVelox(aba, new URL(url)); }
  });
  wc.setWindowOpenHandler(({ url }) => {
    if (/^https?:/i.test(url)) criarAba(url, { anonima: aba.anonima });
    return { action: "deny" };
  });
  wc.on("context-menu", (_e, p) => {
    const itens = [];
    if (p.linkURL) itens.push({ label: "Abrir link em nova aba", click: () => criarAba(p.linkURL, { anonima: aba.anonima }) }, { label: "Copiar endereço do link", click: () => require("electron").clipboard.writeText(p.linkURL) });
    if (p.srcURL && p.mediaType === "image") itens.push({ label: "Usar imagem como fundo do Velox", click: () => baixarComoFundo(p.srcURL) }, { label: "Salvar imagem", click: () => wc.downloadURL(p.srcURL) });
    if (p.selectionText) itens.push({ role: "copy", label: "Copiar" });
    if (p.isEditable) itens.push({ role: "paste", label: "Colar" });
    if (itens.length) Menu.buildFromTemplate(itens).popup();
  });
  wc.on("before-input-event", (e, input) => atalhos(e, input));

  abas.push(aba);
  if (interna === "downloads") wc.loadURL(enderecoDownloads());
  else if (!destino || interna === "inicio") { aba.interna = "inicio"; view.setBackgroundColor("#00000000"); wc.loadURL(enderecoInicio()); }
  else wc.loadURL(destino);
  mostrar(aba);
  return aba;
}

function mostrar(aba) {
  if (ativa) janela.contentView.removeChildView(ativa.view);
  ativa = aba;
  janela.contentView.addChildView(aba.view);
  aba.view.setBounds(limites());
  aba.view.webContents.focus();
  avisarBarra();
}

function fecharAba(id) {
  const i = abas.findIndex((a) => a.id === id);
  if (i < 0) return;
  const aba = abas[i];
  abas.splice(i, 1);
  if (ativa === aba) {
    janela.contentView.removeChildView(aba.view);
    ativa = null;
  }
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
  if (ativa.interna === "downloads") { criarAba(destino); return; } // essa tela tem acesso especial: nunca navega para fora
  ativa.interna = null;
  ativa.view.setBackgroundColor("#ffffffff");
  ativa.view.webContents.loadURL(destino);
}

function atalhos(e, input) {
  if (input.type !== "keyDown") return;
  const ctrl = input.control || input.meta;
  if (ctrl && input.key.toLowerCase() === "t") { e.preventDefault(); criarAba(null, { interna: "inicio" }); }
  else if (ctrl && input.key.toLowerCase() === "w") { e.preventDefault(); if (ativa) fecharAba(ativa.id); }
  else if (ctrl && input.key.toLowerCase() === "l") { e.preventDefault(); janela.webContents.focus(); janela.webContents.send("focar-endereco"); }
  else if (ctrl && input.key.toLowerCase() === "j") { e.preventDefault(); abrirDownloads(); }
  else if (ctrl && input.key.toLowerCase() === "d") { e.preventDefault(); alternarFavorito(); }
  else if (input.key === "F5" || (ctrl && input.key.toLowerCase() === "r")) { e.preventDefault(); if (ativa) ativa.view.webContents.reload(); }
  else if (input.alt && input.key === "ArrowLeft") { if (ativa) ativa.view.webContents.navigationHistory.goBack(); }
  else if (input.alt && input.key === "ArrowRight") { if (ativa) ativa.view.webContents.navigationHistory.goForward(); }
  else if (ctrl && input.key === "Tab") {
    e.preventDefault();
    const i = abas.indexOf(ativa);
    mostrar(abas[(i + (input.shift ? abas.length - 1 : 1)) % abas.length]);
  }
}

function abrirDownloads() {
  const ja = abas.find((a) => a.interna === "downloads");
  if (ja) mostrar(ja); else criarAba(null, { interna: "downloads" });
}

function alternarFavorito() {
  if (!ativa || !ativa.url || !/^https?:/i.test(ativa.url)) return;
  const i = ajustes.favoritos.findIndex((f) => f.u === ativa.url);
  if (i >= 0) ajustes.favoritos.splice(i, 1);
  else ajustes.favoritos.push({ n: (ativa.titulo || ativa.url).slice(0, 40), u: ativa.url });
  gravarAjustes();
  avisarBarra();
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
    const pasta = app.getPath("downloads");
    const caminho = path.join(pasta, item.getFilename());
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
    abrirDownloads();
  });
}
const resumoDownloads = () => downloads.map(({ item, ...d }) => d);
function avisarDownloads() {
  for (const a of abas) if (a.interna === "downloads" && !a.view.webContents.isDestroyed()) a.view.webContents.send("downloads", resumoDownloads());
}

// ------------------------------------------------------------------ comandos vindos das telas
function ligarComandos() {
  ipcMain.on("nova-aba", () => criarAba(null, { interna: "inicio" }));
  ipcMain.on("nova-anonima", () => criarAba(null, { interna: "inicio", anonima: true }));
  ipcMain.on("fechar-aba", (_e, id) => fecharAba(id));
  ipcMain.on("mostrar-aba", (_e, id) => { const a = abas.find((x) => x.id === id); if (a) mostrar(a); });
  ipcMain.on("ir", (_e, texto) => irPara(texto));
  ipcMain.on("voltar", () => ativa && ativa.view.webContents.navigationHistory.goBack());
  ipcMain.on("avancar", () => ativa && ativa.view.webContents.navigationHistory.goForward());
  ipcMain.on("recarregar", () => ativa && (ativa.carregando ? ativa.view.webContents.stop() : ativa.view.webContents.reload()));
  ipcMain.on("inicio", () => ativa && (ativa.interna = "inicio", ativa.view.setBackgroundColor("#00000000"), ativa.view.webContents.loadURL(enderecoInicio())));
  ipcMain.on("favorito", alternarFavorito);
  ipcMain.on("downloads", abrirDownloads);
  ipcMain.on("escolher-fundo", async () => {
    const r = await dialog.showOpenDialog(janela, { title: "Escolher fundo do Velox", properties: ["openFile"], filters: [{ name: "Imagens", extensions: ["jpg", "jpeg", "png", "webp", "bmp"] }] });
    if (!r.canceled && r.filePaths[0]) guardarFundo(r.filePaths[0]);
  });
  ipcMain.on("remover-fundo", () => { ajustes.fundo = ""; gravarAjustes(); recarregarInicios(); avisarBarra(); });
  ipcMain.on("transparencia", (_e, v) => { ajustes.transparencia = Math.max(0, Math.min(100, Number(v) || 0)); gravarAjustes(); avisarBarra(); });
  ipcMain.on("buscador", (_e, id) => { if (BUSCADORES[id]) { ajustes.buscador = id; gravarAjustes(); recarregarInicios(); avisarBarra(); } });
  ipcMain.handle("favoritos", () => ajustes.favoritos);
  ipcMain.on("apagar-favorito", (_e, u) => { ajustes.favoritos = ajustes.favoritos.filter((f) => f.u !== u); gravarAjustes(); avisarBarra(); });
  ipcMain.handle("downloads-lista", () => resumoDownloads());
  ipcMain.on("download-abrir", (_e, id) => { const d = downloads.find((x) => x.id === id); if (d) shell.openPath(d.caminho); });
  ipcMain.on("download-pasta", (_e, id) => { const d = downloads.find((x) => x.id === id); if (d) shell.showItemInFolder(d.caminho); });
  ipcMain.on("download-cancelar", (_e, id) => { const d = downloads.find((x) => x.id === id); if (d && d.item && d.estado === "baixando") d.item.cancel(); });
  ipcMain.on("download-limpar", () => { for (let i = downloads.length - 1; i >= 0; i--) if (downloads[i].estado !== "baixando") downloads.splice(i, 1); avisarDownloads(); });
  ipcMain.on("barra-pronta", avisarBarra);
}

// ------------------------------------------------------------------ início
function iniciar() {
  lerAjustes();
  janela = new BrowserWindow({
    width: 1280, height: 800, minWidth: 720, minHeight: 480,
    backgroundColor: "#070b14", title: "Velox", autoHideMenuBar: true,
    webPreferences: { preload: path.join(__dirname, "preload.js"), contextIsolation: true, nodeIntegration: false },
  });
  janela.setMenuBarVisibility(false);
  janela.loadFile(path.join(__dirname, "ui", "index.html"));
  janela.on("resize", () => ativa && ativa.view.setBounds(limites()));
  janela.on("enter-full-screen", () => ativa && ativa.view.setBounds({ x: 0, y: 0, width: janela.getContentSize()[0], height: janela.getContentSize()[1] }));
  janela.on("leave-full-screen", () => ativa && ativa.view.setBounds(limites()));
  ligarDownloads(session.fromPartition("persist:velox"));
  ligarDownloads(session.fromPartition("velox-anonima"));
  ligarComandos();
  janela.webContents.once("did-finish-load", () => {
    const abrir = process.argv.slice(1).find((a) => /^https?:\/\//i.test(a));
    criarAba(abrir || null, abrir ? {} : { interna: "inicio" });
  });
  janela.on("closed", () => { janela = null; });
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

// Roda dentro de cada página (isolado): percebe formulários de login para oferecer salvar a senha e preenche quando a pessoa pede.
const { ipcRenderer, webFrame } = require("electron");

// no YouTube, injeta o bloqueio de anúncios na própria página antes dos scripts dela
try {
  if (/(^|\.)youtube\.com$/.test(location.hostname)) {
    const src = ipcRenderer.sendSync("yt:script");
    if (src) webFrame.executeJavaScript(src);
  }
} catch (e) { /* segue sem bloqueio */ }

const visivel = (el) => !!(el && (el.offsetWidth || el.offsetHeight || el.getClientRects().length));

function campoDeUsuario(senha) {
  const raiz = senha.form || document;
  const campos = Array.from(raiz.querySelectorAll("input")).filter((i) => visivel(i) && /^(text|email|tel|)$/i.test(i.type || ""));
  let escolhido = null;
  for (const c of campos) {
    if (c.compareDocumentPosition(senha) & Node.DOCUMENT_POSITION_FOLLOWING) escolhido = c; // o último campo de texto antes da senha
  }
  return escolhido;
}

function colocar(campo, valor) {
  if (!campo) return;
  const set = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value").set;
  set.call(campo, valor);
  campo.dispatchEvent(new Event("input", { bubbles: true }));
  campo.dispatchEvent(new Event("change", { bubbles: true }));
}

let ultimo = "";
function capturar(senhaEl) {
  if (!senhaEl || !senhaEl.value) return;
  const u = campoDeUsuario(senhaEl);
  const dados = { origem: location.origin, usuario: u ? u.value : "", senha: senhaEl.value };
  const chave = JSON.stringify(dados);
  if (chave === ultimo) return;
  ultimo = chave;
  ipcRenderer.send("senha:capturada", dados);
}

function ligar() {
  for (const s of document.querySelectorAll('input[type="password"]')) {
    if (s.dataset.veloxLigado) continue;
    s.dataset.veloxLigado = "1";
    const f = s.form;
    if (f) f.addEventListener("submit", () => capturar(s), true);
    s.addEventListener("keydown", (e) => { if (e.key === "Enter") capturar(s); }, true);
  }
}

document.addEventListener("click", (e) => {
  const b = e.target && e.target.closest && e.target.closest('button, input[type="submit"], [role="button"]');
  if (!b) return;
  const f = b.form || b.closest("form") || document;
  const s = f.querySelector('input[type="password"]');
  if (s && s.value) capturar(s);
}, true);

new MutationObserver(ligar).observe(document, { childList: true, subtree: true });
document.addEventListener("DOMContentLoaded", () => { ligar(); ipcRenderer.send("senha:pagina-pronta", { origem: location.origin, temSenha: !!document.querySelector('input[type="password"]') }); });

ipcRenderer.on("senha:preencher", (_e, { usuario, senha }) => {
  const s = Array.from(document.querySelectorAll('input[type="password"]')).find(visivel);
  if (!s) return;
  colocar(campoDeUsuario(s), usuario);
  colocar(s, senha);
});

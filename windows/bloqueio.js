// Bloqueio de anúncios e rastreadores. Usa o motor do Ghostery (listas EasyList/uBlock) e, se ele não carregar, uma lista própria de domínios.
const path = require("path");
const fs = require("fs");

const DOMINIOS = [
  "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com", "google-analytics.com", "googletagmanager.com", "googletagservices.com",
  "adnxs.com", "adsrvr.org", "taboola.com", "outbrain.com", "criteo.com", "criteo.net", "pubmatic.com", "rubiconproject.com", "openx.net", "casalemedia.com",
  "amazon-adsystem.com", "scorecardresearch.com", "quantserve.com", "moatads.com", "adform.net", "smartadserver.com", "advertising.com", "yieldmo.com",
  "hotjar.com", "mixpanel.com", "segment.io", "facebook.net", "connect.facebook.net", "ads.twitter.com", "analytics.tiktok.com", "ads.linkedin.com",
  "popads.net", "propellerads.com", "exoclick.com", "juicyads.com", "adcolony.com", "applovin.com", "unity3d.com/ads", "mgid.com", "revcontent.com",
];

/** true se o endereço (ou um domínio acima dele) está na lista. */
function dominioBloqueado(url, lista = DOMINIOS) {
  let host = "";
  try { host = new URL(url).hostname.toLowerCase(); } catch { return false; }
  return lista.some((d) => !d.includes("/") && (host === d || host.endsWith("." + d)));
}

class Bloqueio {
  constructor(pastaCache) {
    this.pasta = pastaCache;
    this.ligado = true;
    this.bloqueados = 0;
    this.sessoes = new Set();
    this.motor = null;
    this.modo = "desligado"; // "ghostery" | "lista" | "desligado"
    this.aoMudar = null;
  }

  contar() { this.bloqueados++; if (this.aoMudar) this.aoMudar(this.bloqueados); }

  async iniciar(sessoes) {
    for (const s of sessoes) this.sessoes.add(s);
    try {
      const { ElectronBlocker } = require("@ghostery/adblocker-electron");
      fs.mkdirSync(this.pasta, { recursive: true });
      const cache = path.join(this.pasta, "motor.bin");
      this.motor = await ElectronBlocker.fromPrebuiltAdsAndTracking(fetch, {
        path: cache,
        read: fs.promises.readFile,
        write: fs.promises.writeFile,
      });
      this.motor.on("request-blocked", () => this.contar());
      this.modo = "ghostery";
      if (this.ligado) for (const s of this.sessoes) this.motor.enableBlockingInSession(s);
    } catch (e) {
      console.error("bloqueio: motor do Ghostery indisponível, usando a lista própria:", e && e.message ? e.message : e);
      this.modo = "lista";
      for (const s of this.sessoes) this.ligarLista(s);
    }
  }

  ligarLista(ses) {
    ses.webRequest.onBeforeRequest({ urls: ["http://*/*", "https://*/*"] }, (detalhes, resposta) => {
      if (this.ligado && detalhes.resourceType !== "mainFrame" && dominioBloqueado(detalhes.url)) { this.contar(); resposta({ cancel: true }); }
      else resposta({});
    });
  }

  definir(ligado) {
    this.ligado = !!ligado;
    if (this.modo === "ghostery" && this.motor) {
      for (const s of this.sessoes) { if (this.ligado) this.motor.enableBlockingInSession(s); else this.motor.disableBlockingInSession(s); }
    }
  }
}

module.exports = { Bloqueio, dominioBloqueado, DOMINIOS };

/* Bloqueio de anúncios do YouTube (Android e Windows usam este mesmo arquivo).
   Roda no começo da página, no mundo da própria página. Faz o melhor possível: o YouTube muda seguido. */
(function () {
  if (window.__veloxYT) return;
  window.__veloxYT = true;
  var ANUNCIOS = window.__veloxAd !== false;
  /* segundo plano (Android): a página acha que continua visível, então o YouTube não pausa o som */
  if (window.__veloxBg) {
    try {
      Object.defineProperty(document, "hidden", { configurable: true, get: function () { return false; } });
      Object.defineProperty(document, "visibilityState", { configurable: true, get: function () { return "visible"; } });
      Object.defineProperty(document, "webkitHidden", { configurable: true, get: function () { return false; } });
      Object.defineProperty(document, "webkitVisibilityState", { configurable: true, get: function () { return "visible"; } });
      ["visibilitychange", "webkitvisibilitychange", "pagehide", "blur"].forEach(function (n) {
        document.addEventListener(n, function (e) { e.stopImmediatePropagation(); }, true);
        window.addEventListener(n, function (e) { e.stopImmediatePropagation(); }, true);
      });
    } catch (e) { /* segue */ }
  }
  if (!ANUNCIOS) return;
  var CAMPOS = ["adPlacements", "playerAds", "adSlots", "adBreakHeartbeatParams"];

  /* 1) tira os campos de anúncio das respostas do player */
  function limpar(o) {
    try {
      if (!o || typeof o !== "object") return o;
      for (var i = 0; i < CAMPOS.length; i++) if (CAMPOS[i] in o) { o[CAMPOS[i]] = CAMPOS[i] === "adBreakHeartbeatParams" ? undefined : []; }
      if (o.playerResponse) limpar(o.playerResponse);
      if (o.response) limpar(o.response);
      if (Array.isArray(o)) for (var j = 0; j < o.length; j++) limpar(o[j]);
    } catch (e) { /* segue */ }
    return o;
  }
  var parse = JSON.parse;
  JSON.parse = function () { return limpar(parse.apply(this, arguments)); };
  var json = Response.prototype.json;
  Response.prototype.json = function () { return json.apply(this, arguments).then(limpar); };
  try {
    var guardado;
    Object.defineProperty(window, "ytInitialPlayerResponse", {
      configurable: true,
      get: function () { return guardado; },
      set: function (v) { guardado = limpar(v); }
    });
  } catch (e) { /* já definido */ }

  /* 2) esconde o que sobrar na tela */
  var css = [
    "#masthead-ad", "#player-ads", ".video-ads", ".ytp-ad-module", ".ytp-ad-overlay-container", ".ytp-ad-text-overlay",
    "ytd-ad-slot-renderer", "ytd-in-feed-ad-layout-renderer", "ytd-banner-promo-renderer", "ytd-statement-banner-renderer",
    "ytd-promoted-sparkles-web-renderer", "ytd-display-ad-renderer", "ytd-companion-slot-renderer", "ytd-action-companion-ad-renderer",
    "ytd-player-legacy-desktop-watch-ads-renderer", "ytm-promoted-sparkles-web-renderer", "ytm-companion-ad-renderer",
    "ytm-promoted-video-renderer", ".ad-container", ".ytd-merch-shelf-renderer", "#related #player-ads"
  ].join(",") + "{display:none!important}";
  function estilo() {
    if (document.getElementById("velox-yt-css")) return;
    var s = document.createElement("style"); s.id = "velox-yt-css"; s.textContent = css;
    (document.head || document.documentElement).appendChild(s);
  }
  estilo();
  document.addEventListener("DOMContentLoaded", estilo);

  /* 3) se um anúncio passar, pula: acelera, silencia e clica em "Pular" */
  var SKIP = ".ytp-skip-ad-button,.ytp-ad-skip-button,.ytp-ad-skip-button-modern,.ytp-ad-skip-button-slot button,.ytp-skip-ad button";
  function pular() {
    try {
      var p = document.querySelector(".html5-video-player");
      if (!p) return;
      var b = document.querySelectorAll(SKIP);
      for (var i = 0; i < b.length; i++) b[i].click();
      if (p.classList.contains("ad-showing") || p.classList.contains("ad-interrupting")) {
        var v = p.querySelector("video.html5-main-video") || document.querySelector("video");
        if (v) {
          v.muted = true;
          if (isFinite(v.duration) && v.duration > 0) v.currentTime = v.duration;
          v.playbackRate = 16;
        }
      }
      var f = document.querySelector(".ytp-ad-overlay-close-button");
      if (f) f.click();
    } catch (e) { /* segue */ }
  }
  setInterval(pular, 300);
  new MutationObserver(pular).observe(document.documentElement, { childList: true, subtree: true });
})();

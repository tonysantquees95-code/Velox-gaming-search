// Traduzir página: abre a mesma página pelo Google Tradutor. O endereço da página é enviado ao Google só quando a pessoa pede (os termos explicam).
const IDIOMAS = { pt: "pt-BR", en: "en", es: "es" };

/** Endereço da página traduzida para o português (ou null se a página não é http/https). */
function urlTraduzida(url, idioma = "pt") {
  let u;
  try { u = new URL(String(url || "")); } catch { return null; }
  if (!/^https?:$/.test(u.protocol) || ehTraduzida(url)) return null;
  const tl = IDIOMAS[idioma] || IDIOMAS.pt;
  return `https://translate.google.com/translate?sl=auto&tl=${encodeURIComponent(tl)}&hl=${encodeURIComponent(tl)}&u=${encodeURIComponent(u.toString())}`;
}

/** A página que aparece já é uma tradução do Google? */
function ehTraduzida(url) {
  try {
    const h = new URL(String(url || "")).hostname.toLowerCase();
    return h.endsWith(".translate.goog") || h === "translate.google.com" || h === "translate.googleusercontent.com";
  } catch { return false; }
}

/** O endereço original, quando o da tradução ainda leva o parâmetro u (senão null: use o que o app guardou). */
function urlOriginal(url) {
  try {
    const x = new URL(String(url || ""));
    if (x.hostname === "translate.google.com") { const u = x.searchParams.get("u"); return u && /^https?:\/\//i.test(u) ? u : null; }
    return null;
  } catch { return null; }
}

module.exports = { urlTraduzida, ehTraduzida, urlOriginal };

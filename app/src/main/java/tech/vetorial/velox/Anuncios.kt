package tech.vetorial.velox

/** Bloqueio de anúncios e rastreadores por domínio: o endereço e todos os subdomínios dele são barrados. */
object ListaDeAnuncios {
    private val dominios: Set<String> = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com", "pagead2.googlesyndication.com",
        "google-analytics.com", "analytics.google.com", "stats.g.doubleclick.net", "adsense.google.com",
        "taboola.com", "outbrain.com", "adnxs.com", "criteo.com", "criteo.net", "pubmatic.com", "rubiconproject.com", "openx.net",
        "amazon-adsystem.com", "scorecardresearch.com", "quantserve.com", "hotjar.com", "ads-twitter.com", "advertising.com",
        "moatads.com", "adsrvr.org", "casalemedia.com", "yieldmo.com", "zedo.com", "mgid.com", "popads.net", "propellerads.com",
        "exoclick.com", "trafficjunky.net", "adform.net", "smartadserver.com", "teads.tv", "media.net", "bidswitch.net",
        "adroll.com", "revcontent.com", "sharethrough.com", "contextweb.com", "lijit.com", "sovrn.com", "33across.com",
        "ads.yahoo.com", "adtechus.com", "serving-sys.com", "2mdn.net", "demdex.net", "krxd.net", "bluekai.com", "everesttech.net",
        "mathtag.com", "tapad.com", "turn.com", "rlcdn.com", "adition.com", "inmobi.com", "applovin.com", "unityads.unity3d.com",
        "ads.mopub.com", "mopub.com", "adcolony.com", "vungle.com", "chartboost.com", "ironsrc.com", "supersonicads.com",
        "ads.linkedin.com", "ads.pinterest.com", "analytics.tiktok.com", "ads.tiktok.com", "bat.bing.com", "clarity.ms",
        "hotjar.io", "mouseflow.com", "fullstory.com", "crazyegg.com", "segment.io", "mixpanel.com", "branch.io",
        "googletagservices.com", "adsafeprotected.com", "doubleverify.com", "iasds01.com", "pixel.quantserve.com",
        "ads.facebook.com", "an.facebook.com", "pixel.facebook.com", "ads.reddit.com", "events.reddit.com", "alb.reddit.com",
        "ad.doubleclick.net", "securepubads.g.doubleclick.net", "adsymptotic.com", "agkn.com", "bidr.io", "bttrack.com", "cdn.mxpnl.com",
        "stackadapt.com", "triplelift.com", "gumgum.com", "lockerdome.com", "nativo.com", "plista.com", "adblade.com", "adbrite.com",
        "adtelligent.com", "improvedigital.com", "indexww.com", "richaudience.com", "seedtag.com", "smaato.net", "spotxchange.com",
        "springserve.com", "undertone.com", "videoamp.com", "yandex.ru/ads", "mc.yandex.ru", "an.yandex.ru", "ads.yandex.com",
        "hotmart-ads.com", "adskeeper.com", "adsterra.com", "highperformancecpm.com", "highperformanceformat.com", "profitablecpmrate.com",
        "clickadu.com", "hilltopads.net", "monetag.com",
        "adservice.google.com.br", "pagead2.googleadservices.com", "partner.googleadservices.com", "tpc.googlesyndication.com",
        "gemius.pl", "chartbeat.com", "chartbeat.net", "parsely.com", "newrelic.com", "nr-data.net", "optimizely.com", "heap.io", "heapanalytics.com",
        "amplitude.com", "kissmetrics.com", "luckyorange.com", "inspectlet.com", "smartlook.com", "statcounter.com", "histats.com", "yadro.ru",
        "tiktokcdn-us.com/ads", "snapads.com", "sc-static.net", "tr.snapchat.com", "ads.snapchat.com", "static.ads-twitter.com", "analytics.twitter.com"
    ).filter { !it.contains('/') }.toSet()

    fun tamanho(): Int = dominios.size

    fun bloqueia(hospede: String?): Boolean {
        if (hospede.isNullOrEmpty()) return false
        var h = hospede.lowercase()
        while (true) {
            if (h in dominios) return true
            val ponto = h.indexOf('.')
            if (ponto < 0) return false
            h = h.substring(ponto + 1)
        }
    }
}

/**
 * Esconde anúncios e avisos de cookies que passaram pela lista de domínios: um estilo colocado antes de a página aparecer.
 * Só esconde (nunca mexe no resto da página). Seguem o interruptor "Bloquear anúncios".
 */
object LimpezaDePagina {
    private val SELETORES = listOf(
        // anúncios
        "ins.adsbygoogle", ".adsbygoogle", "[id^=\"google_ads_\"]", "[id^=\"div-gpt-ad\"]", "[data-ad-slot]", "[data-google-query-id]",
        "iframe[src*=\"doubleclick.net\"]", "iframe[src*=\"googlesyndication.com\"]", "iframe[id^=\"google_ads_iframe\"]",
        ".ad-banner", ".ad-container", ".ad-slot", ".advert", ".advertisement", "#ad-banner", ".banner-ads", ".sponsored-ad",
        "[class*=\"taboola\"]", "[id*=\"taboola\"]", "[class*=\"outbrain\"]", "[id^=\"outbrain\"]", ".OUTBRAIN",
        // avisos de cookies e de notificações
        "#onetrust-banner-sdk", "#onetrust-consent-sdk", "#CybotCookiebotDialog", "#CybotCookiebotDialogBodyUnderlay",
        "#cookie-banner", ".cookie-banner", ".cookie-consent", ".cookie-notice", "#cookie-notice", "#cookieConsent", "#cookie-law-info-bar",
        ".cc-window", ".cc-banner", ".fc-consent-root", "#didomi-host", ".truste_overlay", "#qc-cmp2-container", "#usercentrics-root",
        ".osano-cm-window", "#cmpbox", "#sp_message_container_", "[id^=\"sp_message_container\"]"
    )

    /** Uma linha só: "seletor1,seletor2{display:none!important}". */
    fun css(): String = SELETORES.joinToString(",") + "{display:none!important}"

    /** Script que entra antes da página rodar e coloca o estilo (uma vez só por página). */
    fun js(): String =
        "(function(){if(window.__vxLimpo)return;window.__vxLimpo=1;var s=document.createElement('style');s.id='__vxcss';" +
            "s.textContent='" + css().replace("\\", "\\\\").replace("'", "\\'") + "';" +
            "(document.head||document.documentElement).appendChild(s);})();"
}

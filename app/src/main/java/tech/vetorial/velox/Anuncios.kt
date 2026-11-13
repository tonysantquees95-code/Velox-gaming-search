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
        "hotjar.io", "mouseflow.com", "fullstory.com", "crazyegg.com", "segment.io", "mixpanel.com", "branch.io"
    )

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

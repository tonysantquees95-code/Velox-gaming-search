(function () {
  var V = window.velox, q = new URLSearchParams(location.search);
  // ícones de traço, todos no mesmo estilo (mesma espessura e cor do texto da barra)
  var I = {
    voltar: '<path d="M15 5l-7 7 7 7"/>',
    recarregar: '<path d="M19 12a7 7 0 1 1-2.1-5M19 4v4h-4"/>',
    captura: '<path d="M4 8h4l1.5-2.5h5L16 8h4v11H4z"/><circle cx="12" cy="13.5" r="3"/>',
    som: '<path d="M4 10h4l4-3.5v11L8 14H4z"/><path d="M15 9a4 4 0 0 1 0 6M17.5 6.5a8 8 0 0 1 0 11"/>',
    mudo: '<path d="M4 10h4l4-3.5v11L8 14H4z"/><path d="M15 9.5l5 5M20 9.5l-5 5"/>',
    escudo: '<path d="M12 3 5 5.8v5.6c0 4.2 2.9 7.6 7 9.1 4.1-1.5 7-4.9 7-9.1V5.8z"/>',
    lado: '<path d="M4 8h16M16 4.5L20 8l-4 3.5M20 16H4M8 12.5L4 16l4 3.5"/>',
    recolher: '<path d="M9 5l7 7-7 7"/>',
    sair: '<path d="M6 6l12 12M18 6 6 18"/>'
  };
  function svg(nome) { return '<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' + I[nome] + '</svg>'; }
  Array.prototype.forEach.call(document.querySelectorAll("button[data-i]"), function (b) { b.innerHTML = svg(b.dataset.i); });
  function aplicar(e) {
    var lado = e.pos === "esq" || e.pos === "dir";
    document.body.className = (lado ? "lado" : "") + (e.rec ? " rec" : "");
    // a seta aponta para a borda onde a barra mora; recolhida, aponta para dentro (abre)
    var graus = { dir: 0, esq: 180, cima: 270, baixo: 90 }[e.pos] || 0;
    var r = document.getElementById("b-recolher");
    r.firstChild.style.transform = "rotate(" + (graus + (e.rec ? 180 : 0)) + "deg)";
    r.title = e.rec ? "Abrir a barra" : "Recolher a barra";
    var s = document.getElementById("b-som"); s.innerHTML = svg(e.mudo ? "mudo" : "som");
    document.getElementById("b-bloqueio").className = e.bloqueio ? "ligado" : "";
  }
  document.getElementById("barra").onclick = function (ev) {
    var b = ev.target.closest("button"); if (!b) return;
    V.enviar("jogo:acao", b.dataset.a);
    if (b.dataset.a === "captura") { var o = b.innerHTML; b.innerHTML = '<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12.5l4.5 4.5L19 7.5"/></svg>'; setTimeout(function () { b.innerHTML = o; }, 1400); }
  };
  function ponha(id, texto, nivel) {
    var el = document.getElementById(id); el.firstChild.nextSibling.textContent = texto;
    el.className = "m" + (nivel ? " " + nivel : "");
  }
  var porUso = function (v) { return v == null ? "" : v >= 90 ? "alto" : v >= 70 ? "aviso" : "ok"; };
  function medidas(m) {
    ponha("m-fps", m.fps == null ? "–" : String(m.fps), m.fps == null ? "" : m.fps >= 55 ? "ok" : m.fps >= 30 ? "aviso" : "alto");
    ponha("m-cpu", m.cpu == null ? "–" : m.cpu + "%", porUso(m.cpu));
    ponha("m-gpu", m.gpu == null ? "–" : m.gpu + "%", porUso(m.gpu));
    var t = m.tempGpu != null ? m.tempGpu : m.tempCpu;
    ponha("m-temp", t == null ? "–" : t + "°", t == null ? "" : t >= 85 ? "alto" : t >= 72 ? "aviso" : "ok");
    document.getElementById("m-temp").title = (m.tempGpu != null ? "Temperatura da placa de vídeo" : m.tempCpu != null ? "Temperatura do processador" : "O Windows não informou a temperatura");
    ponha("m-ram", m.ram == null ? "–" : m.ram + "%", porUso(m.ram));
    ponha("m-ping", m.ping == null ? "–" : m.ping + " ms", m.ping == null ? "" : m.ping <= 60 ? "ok" : m.ping <= 120 ? "aviso" : "alto");
    if (m.gpuNome) document.getElementById("m-gpu").title = m.gpuNome;
  }
  V.ouvir("jogo:medidas", medidas);
  V.ouvir("jogo:estado", aplicar);
  aplicar({ pos: q.get("pos") || "dir", rec: q.get("rec") === "1", mudo: false, bloqueio: true });
})();

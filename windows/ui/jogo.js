(function () {
  var V = window.velox, q = new URLSearchParams(location.search);
  function aplicar(e) {
    var lado = e.pos === "esq" || e.pos === "dir";
    document.body.className = (lado ? "lado" : "") + (e.rec ? " rec" : "");
    var seta = { dir: e.rec ? "‹" : "›", esq: e.rec ? "›" : "‹", cima: e.rec ? "⌄" : "⌃", baixo: e.rec ? "⌃" : "⌄" }[e.pos] || "›";
    document.getElementById("b-recolher").textContent = seta;
    document.getElementById("b-recolher").title = e.rec ? "Abrir a barra" : "Recolher a barra";
    document.getElementById("b-som").textContent = e.mudo ? "🔇" : "🔊";
    document.getElementById("b-bloqueio").className = e.bloqueio ? "ligado" : "";
  }
  document.getElementById("barra").onclick = function (ev) {
    var b = ev.target.closest("button"); if (!b) return;
    V.enviar("jogo:acao", b.dataset.a);
    if (b.dataset.a === "captura") { var o = b.textContent; b.textContent = "✔"; setTimeout(function () { b.textContent = o; }, 1400); }
  };
  V.ouvir("jogo:estado", aplicar);
  aplicar({ pos: q.get("pos") || "dir", rec: q.get("rec") === "1", mudo: false, bloqueio: true });
})();

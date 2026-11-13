(function () {
  var $ = function (id) { return document.getElementById(id); };
  var V = window.velox, estado = null;
  function desenhar(e) {
    estado = e;
    var abas = $("abas"); abas.textContent = "";
    e.abas.forEach(function (a) {
      var d = document.createElement("div");
      d.className = "aba" + (a.id === e.ativa ? " ativa" : "") + (a.carregando ? " carregando" : "");
      var t = document.createElement("span"); t.textContent = a.titulo; t.title = a.url || a.titulo;
      var x = document.createElement("b"); x.textContent = "×"; x.title = "Fechar aba";
      x.onclick = function (ev) { ev.stopPropagation(); V.enviar("fechar-aba", a.id); };
      d.onclick = function () { V.enviar("mostrar-aba", a.id); };
      d.onauxclick = function (ev) { if (ev.button === 1) V.enviar("fechar-aba", a.id); };
      d.appendChild(t); d.appendChild(x); abas.appendChild(d);
    });
    var m = document.createElement("button"); m.id = "mais"; m.textContent = "+"; m.title = "Nova aba (Ctrl+T)";
    m.onclick = function () { V.enviar("nova-aba"); }; abas.appendChild(m);
    $("voltar").disabled = !e.voltar; $("avancar").disabled = !e.avancar;
    $("estrela").textContent = e.favorita ? "★" : "☆"; $("estrela").className = e.favorita ? "cheia" : "";
    $("estrela").disabled = !e.abas.some(function (a) { return a.id === e.ativa && /^https?:/i.test(a.url); });
    var at = e.abas.filter(function (a) { return a.id === e.ativa; })[0];
    if (document.activeElement !== $("endereco")) $("endereco").value = at ? at.url : "";
    document.documentElement.style.setProperty("--alfa", String(Math.max(0.15, 1 - e.transparencia / 100)));
    $("fundo").style.backgroundImage = e.fundo ? 'url("' + e.fundo + '")' : "none";
    $("g-trans").value = e.transparencia; $("g-bus").value = e.buscador;
  }
  V.ouvir("estado", desenhar);
  V.ouvir("focar-endereco", function () { $("endereco").focus(); $("endereco").select(); });
  $("voltar").onclick = function () { V.enviar("voltar"); };
  $("avancar").onclick = function () { V.enviar("avancar"); };
  $("recarregar").onclick = function () { V.enviar("recarregar"); };
  $("inicio").onclick = function () { V.enviar("inicio"); };
  $("estrela").onclick = function () { V.enviar("favorito"); };
  $("baixados").onclick = function () { V.enviar("downloads"); };
  $("endereco").addEventListener("keydown", function (ev) { if (ev.key === "Enter") { V.enviar("ir", $("endereco").value); $("endereco").blur(); } });
  $("endereco").addEventListener("focus", function () { setTimeout(function () { $("endereco").select(); }, 0); });
  $("menu").onclick = function () { $("gaveta").hidden = !$("gaveta").hidden; };
  document.addEventListener("click", function (ev) { if (!$("gaveta").contains(ev.target) && ev.target !== $("menu")) $("gaveta").hidden = true; });
  $("g-fundo").onclick = function () { V.enviar("escolher-fundo"); };
  $("g-sem-fundo").onclick = function () { V.enviar("remover-fundo"); };
  $("g-trans").oninput = function () { V.enviar("transparencia", this.value); };
  $("g-bus").onchange = function () { V.enviar("buscador", this.value); };
  $("g-anonima").onclick = function () { V.enviar("nova-anonima"); $("gaveta").hidden = true; };
  V.enviar("barra-pronta");
})();

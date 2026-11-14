(function () {
  var $ = function (id) { return document.getElementById(id); };
  var V = window.velox, estado = null;
  function desenhar(e) {
    estado = e;
    var abas = $("abas"); abas.textContent = "";
    e.abas.forEach(function (a) {
      var d = document.createElement("div");
      d.className = "aba" + (a.id === e.ativa ? " ativa" : "") + (a.carregando ? " carregando" : "") + (a.anonima ? " anonima" : "");
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
    var https = at && /^https:/i.test(at.url), http = at && /^http:/i.test(at.url);
    $("cadeado").textContent = https ? "🔒" : http ? "⚠" : "";
    $("cadeado").title = https ? "Conexão segura" : http ? "Conexão sem proteção" : "";
    $("chave").className = e.cofreAberto ? "aberta" : "";
    $("g-bloqueio").checked = !!e.bloqueio;
    $("g-contador").textContent = e.bloqueados ? "(" + e.bloqueados + " bloqueados)" : "";
    var fav = $("favoritos"); fav.textContent = "";
    fav.hidden = !e.favoritos.length;
    e.favoritos.forEach(function (f) {
      var b = document.createElement("button"); b.textContent = f.n; b.title = f.u;
      b.onclick = function () { V.enviar("abrir-favorito", f.u); };
      fav.appendChild(b);
    });
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
  $("chave").onclick = function () { V.enviar("chave"); };
  $("g-bloqueio").onchange = function () { V.chamar("ajustes:bloqueio", this.checked); };
  $("g-ajustes").onclick = function () { V.enviar("abrir-interna", "ajustes"); $("gaveta").hidden = true; };
  $("g-historico").onclick = function () { V.enviar("abrir-interna", "historico"); $("gaveta").hidden = true; };
  /* busca na página (Ctrl+F) */
  function abrirBusca() { $("campo").hidden = true; $("busca").hidden = false; $("busca-texto").focus(); $("busca-texto").select(); }
  function fecharBusca() { $("busca").hidden = true; $("campo").hidden = false; $("busca-n").textContent = ""; }
  V.ouvir("abrir-busca", abrirBusca);
  V.ouvir("fechar-busca", fecharBusca);
  V.ouvir("busca-resultado", function (r) { $("busca-n").textContent = r.total ? r.ordinal + "/" + r.total : "0"; });
  $("busca-texto").addEventListener("input", function () { if (this.value) V.enviar("busca", { texto: this.value, proximo: true }); });
  $("busca-texto").addEventListener("keydown", function (ev) {
    if (ev.key === "Enter") V.enviar("busca", { texto: this.value, proximo: !ev.shiftKey });
    if (ev.key === "Escape") V.enviar("busca-fechar");
  });
  $("busca-prox").onclick = function () { V.enviar("busca", { texto: $("busca-texto").value, proximo: true }); };
  $("busca-ant").onclick = function () { V.enviar("busca", { texto: $("busca-texto").value, proximo: false }); };
  $("busca-fechar").onclick = function () { V.enviar("busca-fechar"); };
  V.enviar("barra-pronta");
})();

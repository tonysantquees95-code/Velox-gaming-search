(function () {
  var $ = function (id) { return document.getElementById(id); };
  var V = window.velox, estado = null;
  /* ícones no mesmo traço do celular (grade 24x24, pacote "linha") */
  var ICONES = {
    voltar: '<path d="M15 5L8 12L15 19"/>',
    avancar: '<path d="M9 5L16 12L9 19"/>',
    inicio: '<path d="M4 11L12 4L20 11"/><path d="M6 10V20H18V10"/>',
    recarregar: '<path d="M19 12A7 7 0 1 1 16.95 7.05"/><path d="M19 4V8H15"/>',
    ajustes: '<path d="M10.32 5.00 L10.87 2.47 L13.13 2.47 L13.68 5.00 L15.76 5.86 L17.94 4.46 L19.54 6.06 L18.14 8.24 L19.00 10.32 L21.53 10.87 L21.53 13.13 L19.00 13.68 L18.14 15.76 L19.54 17.94 L17.94 19.54 L15.76 18.14 L13.68 19.00 L13.13 21.53 L10.87 21.53 L10.32 19.00 L8.24 18.14 L6.06 19.54 L4.46 17.94 L5.86 15.76 L5.00 13.68 L2.47 13.13 L2.47 10.87 L5.00 10.32 L5.86 8.24 L4.46 6.06 L6.06 4.46 L8.24 5.86 Z" stroke-linejoin="round"/><circle cx="12" cy="12" r="3.1"/>',
    chave: '<circle cx="7.5" cy="15.5" r="3.5"/><path d="M10 13L20 3M17 6L20 9M14 9L16.5 11.5"/>',
    estrela: '<path d="M12 3.5L14.6 8.8L20.5 9.7L16.2 13.8L17.2 19.6L12 16.9L6.8 19.6L7.8 13.8L3.5 9.7L9.4 8.8Z"/>',
    menu: '<path d="M12 5.5h.01M12 12h.01M12 18.5h.01"/>',
    gamer: '<path d="M7 8H17Q21 8 21 12V14C21 17 17 18 15 15L14 14H10L9 15C7 18 3 17 3 14V12Q3 8 7 8Z"/><path d="M8 10.5V13.5M6.5 12H9.5"/><path d="M16 11.5h.01M18 13h.01"/>'
  };
  /* contornos em SVG com cantos cortados (o mesmo desenho do celular) */
  var NS = "http://www.w3.org/2000/svg";
  function contorno(el) {
    var w = el.clientWidth, h = el.clientHeight; if (!w || !h) return;
    var svg = el.querySelector(":scope > svg.contorno");
    if (!svg) { svg = document.createElementNS(NS, "svg"); svg.setAttribute("class", "contorno"); svg.innerHTML = '<polygon class="cheio"/><polyline class="linha"/>'; el.insertBefore(svg, el.firstChild); }
    svg.setAttribute("width", w); svg.setAttribute("height", h); svg.setAttribute("viewBox", "0 0 " + w + " " + h);
    var m = 0.75, x2 = w - m, y2 = h - m, pts, fecha = true;
    if (el.dataset.corte === "aba") { var k = Math.min(13, h * 0.6); pts = [[m, y2 + 2], [k, m], [x2 - k, m], [x2, y2 + 2]]; fecha = false; }
    else { var c = el.dataset.corte.split(",").map(Number), tl = c[0], tr = c[1], br = c[2], bl = c[3];
      pts = [[m, tl + m], [tl + m, m], [x2 - tr, m], [x2, tr + m], [x2, y2 - br], [x2 - br, y2], [bl + m, y2], [m, y2 - bl]]; }
    var s = pts.map(function (p) { return p[0].toFixed(1) + "," + p[1].toFixed(1); }).join(" ");
    svg.querySelector(".cheio").setAttribute("points", s);
    svg.querySelector(".linha").setAttribute("points", s + (fecha ? " " + s.split(" ")[0] : ""));
  }
  function contornos() { document.querySelectorAll(".corte").forEach(contorno); }
  window.addEventListener("resize", contornos);
  if (window.ResizeObserver) { var ro = new ResizeObserver(contornos); ["botoes", "campo", "gamer"].forEach(function (id) { ro.observe($(id)); }); }
  var uid = 0, pacote = "", cores = "";
  function pintar(el, nome) {
    el.innerHTML = window.VGX.pacoteSvg(pacote, ICONES[nome], cores.split("/")[0], cores.split("/")[1], ++uid);
  }
  function pintarTudo() {
    ["voltar", "avancar", "inicio", "recarregar", "chave", "estrela", "menu", "ajustes"].forEach(function (n) { pintar($(n), n); });
    pintar(document.querySelector("#gamer .inc"), "gamer");
  }
  /* tema do navegador (o mesmo do celular): cores nas variáveis e pacote de ícones */
  function aplicarTema(texto) {
    var P = new URLSearchParams(texto || ""), S = document.documentElement.style;
    [["c1", "--c1"], ["c2", "--c2"], ["bg", "--bg"], ["sup", "--sup"], ["campo", "--campo"], ["su", "--su"], ["tx", "--tx"]].forEach(function (p) { if (/^[0-9a-f]{6}$/i.test(P.get(p[0]) || "")) S.setProperty(p[1], "#" + P.get(p[0])); });
    if (/^[0-9a-f]{6}$/i.test(P.get("c1") || "")) { S.setProperty("--neon", "#" + P.get("c1")); S.setProperty("--neon-rgb", parseInt(P.get("c1").slice(0, 2), 16) + "," + parseInt(P.get("c1").slice(2, 4), 16) + "," + parseInt(P.get("c1").slice(4, 6), 16)); }
    if (/^[0-9a-f]{6}$/i.test(P.get("c2") || "")) { S.setProperty("--neon2", "#" + P.get("c2")); S.setProperty("--neon2-rgb", parseInt(P.get("c2").slice(0, 2), 16) + "," + parseInt(P.get("c2").slice(2, 4), 16) + "," + parseInt(P.get("c2").slice(4, 6), 16)); }
    var novoPacote = P.get("pk") || "linha", novasCores = (P.get("c1") || "22d3ee") + "/" + (P.get("c2") || "a78bfa");
    if (novoPacote !== pacote || novasCores !== cores) { pacote = novoPacote; cores = novasCores; pintarTudo(); }
  }
  aplicarTema("");
  /* download: anel + seta, igual ao botão do celular */
  $("baixados").innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true"><g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="8.6" stroke-width="1.7" stroke-opacity=".3"/><path d="M12 8.6V15.4M9.4 12.8L12 15.4L14.6 12.8" stroke-width="2"/></g></svg>';
  function desenhar(e) {
    estado = e;
    aplicarTema(e.tema);
    var abas = $("abas"); abas.textContent = "";
    e.abas.forEach(function (a) {
      var d = document.createElement("div"); d.dataset.corte = "aba";
      d.className = "aba corte" + (a.id === e.ativa ? " ativa" : "") + (a.carregando ? " carregando" : "") + (a.anonima ? " anonima" : "");
      var t = document.createElement("span"); t.textContent = a.titulo; t.title = a.url || a.titulo;
      var x = document.createElement("b"); x.textContent = "×"; x.title = "Fechar aba";
      x.onclick = function (ev) { ev.stopPropagation(); V.enviar("fechar-aba", a.id); };
      d.onclick = function () { V.enviar("mostrar-aba", a.id); };
      d.onauxclick = function (ev) { if (ev.button === 1) V.enviar("fechar-aba", a.id); };
      d.appendChild(t); d.appendChild(x);
      if (a.carregando) { var pg = document.createElement("i"); pg.className = "prog"; pg.style.width = Math.max(a.progresso || 0, 6) + "%"; d.appendChild(pg); }
      abas.appendChild(d);
    });
    var m = document.createElement("button"); m.id = "mais"; m.textContent = "+"; m.title = "Nova aba (Ctrl+T)";
    m.onclick = function () { V.enviar("nova-aba"); }; abas.appendChild(m);
    $("voltar").disabled = !e.voltar; $("avancar").disabled = !e.avancar;
    $("estrela").className = "ic" + (e.favorita ? " cheia" : "");
    $("estrela").disabled = !e.abas.some(function (a) { return a.id === e.ativa && /^https?:/i.test(a.url); });
    var at = e.abas.filter(function (a) { return a.id === e.ativa; })[0];
    if (document.activeElement !== $("endereco")) $("endereco").value = at ? at.url : "";
    var https = at && /^https:/i.test(at.url), http = at && /^http:/i.test(at.url);
    $("cadeado").textContent = https ? "🔒" : http ? "⚠" : "";
    $("cadeado").title = https ? "Conexão segura" : http ? "Conexão sem proteção" : "";
    $("chave").className = "ic" + (e.cofreAberto ? " aberta" : "");
    var fav = $("favoritos"); fav.textContent = "";
    e.favoritos.forEach(function (f) {
      var b = document.createElement("button"); b.title = f.n + "\n" + f.u + "\nBotão direito: renomear";
      if (f.i) { var im = document.createElement("img"); im.src = f.i; im.alt = ""; b.appendChild(im); }
      b.appendChild(document.createTextNode(f.n));
      b.oncontextmenu = function (ev) { ev.preventDefault(); V.enviar("renomear-favorito", f.u); };
      b.onclick = function () { V.enviar("abrir-favorito", f.u); };
      fav.appendChild(b);
    });
    contornos();
    document.documentElement.style.setProperty("--alfa", String(Math.max(0.15, 1 - e.transparencia / 100)));
    $("fundo").style.backgroundImage = e.fundo ? 'url("' + e.fundo + '")' : "none";
  }
  V.ouvir("estado", desenhar);
  V.ouvir("focar-endereco", function () { $("endereco").focus(); $("endereco").select(); });
  $("voltar").onclick = function () { V.enviar("voltar"); };
  $("avancar").onclick = function () { V.enviar("avancar"); };
  $("recarregar").onclick = function () { V.enviar("recarregar"); };
  $("inicio").onclick = function () { V.enviar("inicio"); };
  $("estrela").onclick = function () { V.enviar("favorito"); };
  $("baixados").onclick = function () { V.enviar("pop-abrir", "downloads", Math.round($("baixados").getBoundingClientRect().right)); };
  $("endereco").addEventListener("keydown", function (ev) { if (ev.key === "Enter") { V.enviar("ir", $("endereco").value); $("endereco").blur(); } });
  $("endereco").addEventListener("focus", function () { setTimeout(function () { $("endereco").select(); }, 0); });
  $("gamer").onclick = function () { V.enviar("modo-gamer"); };
  function ancoras() { return { menu: Math.round($("menu").getBoundingClientRect().right), baixados: Math.round($("baixados").getBoundingClientRect().right) }; }
  function enviarAncoras() { V.enviar("ancoras", ancoras()); }
  window.addEventListener("resize", enviarAncoras); enviarAncoras();
  $("menu").onclick = function () { V.enviar("pop-abrir", "menu", ancoras().menu); };
  document.addEventListener("pointerdown", function (ev) { if (!ev.target.closest("#menu, #baixados")) V.enviar("pop-fechar"); });
  $("ajustes").onclick = function () { V.enviar("abrir-interna", "ajustes"); };
  $("chave").onclick = function () { V.enviar("chave"); };
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
  setTimeout(contornos, 0);
  V.enviar("barra-pronta");
})();

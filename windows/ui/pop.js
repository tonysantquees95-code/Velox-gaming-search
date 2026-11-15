(function () {
  var V = window.velox, $ = function (id) { return document.getElementById(id); };
  var D = null;
  var IC = {
    aba: '<rect x="4" y="5" width="16" height="14" rx="2"/><path d="M4 9h16"/>',
    anon: '<path d="M4 13l2-6h12l2 6M3 13h18M8 18a2 2 0 100-4 2 2 0 000 4zM16 18a2 2 0 100-4 2 2 0 000 4z"/>',
    hist: '<path d="M4 12a8 8 0 108-8 8 8 0 00-6 2.7M4 4v4h4M12 8v4l3 2"/>',
    down: '<path d="M12 4v11M7.5 11L12 15.5 16.5 11M5 20h14"/>',
    abrir: '<path d="M14 4h6v6M20 4l-9 9M18 14v5H5V6h5"/>',
    pasta: '<path d="M3 7a2 2 0 012-2h4l2 2h8a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2z"/>',
    fav: '<path d="M12 3.5L14.6 8.8L20.5 9.7L16.2 13.8L17.2 19.6L12 16.9L6.8 19.6L7.8 13.8L3.5 9.7L9.4 8.8Z"/>',
    zoom: '<circle cx="11" cy="11" r="6"/><path d="M16 16l4 4M11 8v6M8 11h6"/>',
    imp: '<path d="M7 9V4h10v5M7 17H5v-6h14v6h-2M7 14h10v6H7z"/>',
    achar: '<circle cx="11" cy="11" r="6"/><path d="M16 16l4 4"/>',
    gamer: '<path d="M7 8H17Q21 8 21 12V14C21 17 17 18 15 15L14 14H10L9 15C7 18 3 17 3 14V12Q3 8 7 8Z"/><path d="M8 10.5V13.5M6.5 12H9.5"/>',
    cfg: '<path d="M10.32 5.00 L10.87 2.47 L13.13 2.47 L13.68 5.00 L15.76 5.86 L17.94 4.46 L19.54 6.06 L18.14 8.24 L19.00 10.32 L21.53 10.87 L21.53 13.13 L19.00 13.68 L18.14 15.76 L19.54 17.94 L17.94 19.54 L15.76 18.14 L13.68 19.00 L13.13 21.53 L10.87 21.53 L10.32 19.00 L8.24 18.14 L6.06 19.54 L4.46 17.94 L5.86 15.76 L5.00 13.68 L2.47 13.13 L2.47 10.87 L5.00 10.32 L5.86 8.24 L4.46 6.06 L6.06 4.46 L8.24 5.86 Z" stroke-linejoin="round"/><circle cx="12" cy="12" r="3.1"/>',
    cores: '<circle cx="12" cy="12" r="8"/><circle cx="9" cy="10" r="1"/><circle cx="14" cy="9" r="1"/><circle cx="15" cy="14" r="1"/>'
  };
  function el(tag, cls, texto) { var e = document.createElement(tag); if (cls) e.className = cls; if (texto != null) e.textContent = texto; return e; }
  function icone(nome) { var s = el("span"); s.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true">' + (IC[nome] || "") + "</svg>"; return s.firstChild; }
  function item(rotulo, o) {
    o = o || {};
    var b = el("button", "item"); b.type = "button";
    if (o.icone) b.appendChild(icone(o.icone)); else if (o.vazioIcone) b.appendChild(el("span"));
    b.appendChild(el("span", "rot", rotulo));
    if (o.atalho) b.appendChild(el("span", "atalho", o.atalho));
    if (o.seta) b.appendChild(el("span", "seta", "›"));
    if (o.aberto) b.classList.add("aberto");
    b.onclick = o.clique || null;
    if (o.sobre) b.onmouseenter = o.sobre;
    return b;
  }
  function acao(nome, arg) { return function () { V.enviar("pop:acao", nome, arg); }; }
  function sub(nome) { return function () { if (D && D.sub === nome) return; V.enviar("pop:sub", nome); }; }
  function sep() { return el("div", "sep"); }
  function tamanho(b) { if (!b) return ""; var u = ["B", "KB", "MB", "GB"], i = 0; while (b >= 1024 && i < 3) { b /= 1024; i++; } return b.toFixed(i > 1 ? 1 : 0).replace(".", ",") + " " + u[i]; }

  function desenharMenu(c) {
    c.appendChild(item("Nova aba", { icone: "aba", atalho: "Ctrl+T", clique: acao("nova-aba") }));
    c.appendChild(item("Nova aba anônima", { icone: "anon", atalho: "Ctrl+Shift+N", clique: acao("anonima") }));
    c.appendChild(sep());
    c.appendChild(item("Histórico", { icone: "hist", seta: true, aberto: D.sub === "historico", clique: sub("historico"), sobre: sub("historico") }));
    c.appendChild(item("Downloads", { icone: "down", atalho: "Ctrl+J", seta: true, aberto: D.sub === "downloads", clique: sub("downloads"), sobre: sub("downloads") }));
    c.appendChild(item("Favoritos", { icone: "fav", seta: true, aberto: D.sub === "favoritos", clique: sub("favoritos"), sobre: sub("favoritos") }));
    c.appendChild(sep());
    var z = el("div", "zoom"); z.appendChild(icone("zoom")); z.appendChild(el("span", "rot", "Zoom"));
    var m = el("button", null, "−"); m.onclick = acao("zoom-"); z.appendChild(m);
    z.appendChild(el("span", "valor", D.zoom + "%"));
    var p = el("button", null, "+"); p.onclick = acao("zoom+"); z.appendChild(p);
    var t = el("button", null, D.tela ? "⤡" : "⛶"); t.title = D.tela ? "Sair da tela cheia (F11)" : "Tela cheia (F11)"; t.onclick = acao("tela-cheia"); z.appendChild(t);
    c.appendChild(z);
    c.appendChild(sep());
    c.appendChild(item("Imprimir…", { icone: "imp", atalho: "Ctrl+P", clique: acao("imprimir") }));
    c.appendChild(item("Localizar na página…", { icone: "achar", atalho: "Ctrl+F", clique: acao("localizar") }));
    c.appendChild(item("Modo gamer", { icone: "gamer", atalho: "Ctrl+Shift+G", clique: acao("gamer") }));
    c.appendChild(sep());
    c.appendChild(item("Personalizar…", { icone: "cores", clique: acao("personalizar") }));
    c.appendChild(item("Ajustes", { icone: "cfg", clique: acao("ajustes") }));
  }

  function desenharDownloads(c, comTitulo) {
    var cab = el("div", "cab"); cab.appendChild(el("span", null, "Downloads recentes"));
    var x = el("button", null, "×"); x.title = "Fechar"; x.onclick = acao("fechar"); cab.appendChild(x); c.appendChild(cab);
    if (!D.downloads.length) c.appendChild(el("div", "vazio", "Nenhum download por aqui ainda."));
    D.downloads.forEach(function (d) {
      var b = el("div", "dl");
      b.appendChild(el("b", null, d.nome));
      var pct = d.total ? Math.round((d.recebido / d.total) * 100) : 0;
      var linha = d.estado === "baixando" ? (d.total ? tamanho(d.recebido) + " de " + tamanho(d.total) + " · " + pct + "%" : tamanho(d.recebido)) + (d.velocidade ? " · " + tamanho(d.velocidade) + "/s" : "")
        : d.estado === "pronto" ? tamanho(d.total || d.recebido) + " · concluído" : d.estado === "pausado" ? "Pausado" : d.estado === "cancelado" ? "Cancelado" : "Não deu certo";
      b.appendChild(el("small", null, linha));
      if (d.estado === "baixando") { var bar = el("div", "barra"), i = el("i"); i.style.width = (d.total ? pct : 30) + "%"; bar.appendChild(i); b.appendChild(bar); }
      b.title = d.nome;
      if (d.estado === "pronto") {
        // dois botões com os ícones do app: abrir o arquivo agora, ou só mostrar onde ele ficou
        var acoes = el("div", "dl-acoes");
        [["abrir", "Abrir", "download-abrir"], ["pasta", "Abrir pasta", "download-pasta"]].forEach(function (a) {
          var bt = el("button", "dl-bt"); bt.type = "button"; bt.title = a[1];
          bt.appendChild(icone(a[0])); bt.appendChild(el("span", null, a[1]));
          bt.onclick = acao(a[2], d.id); acoes.appendChild(bt);
        });
        b.appendChild(acoes);
      }
      c.appendChild(b);
    });
    c.appendChild(sep());
    c.appendChild(item("Histórico completo de downloads", { icone: "down", atalho: "", clique: acao("downloads-tudo") }));
  }

  function desenharHistorico(c) {
    c.appendChild(item("Abrir página de histórico", { icone: "hist", atalho: "Ctrl+H", clique: acao("historico") }));
    c.appendChild(sep());
    c.appendChild(el("div", "titulo", "Visitados recentemente"));
    if (!D.historico.length) c.appendChild(el("div", "vazio", "Nada visitado ainda."));
    D.historico.forEach(function (h) { var b = item(h.n || h.u, { vazioIcone: true, clique: acao("ir", h.u) }); b.title = h.u; c.appendChild(b); });
  }

  function desenharFavoritos(c) {
    c.appendChild(item(D.favorita ? "Remover esta aba dos favoritos" : "Adicionar esta aba aos favoritos", { icone: "fav", atalho: "Ctrl+D", clique: acao("favoritar") }));
    c.appendChild(item((D.mostrarFavoritos ? "✓ " : "") + "Mostrar a barra de favoritos", { vazioIcone: true, clique: acao("fav-barra") }));
    c.appendChild(sep());
    c.appendChild(el("div", "titulo", "Favoritos"));
    if (!D.favoritos.length) c.appendChild(el("div", "vazio", "Nenhum favorito ainda."));
    D.favoritos.forEach(function (f) { var b = item(f.n, { vazioIcone: !f.i, clique: acao("ir", f.u) }); b.title = f.u + "\nBotão direito: renomear";
      if (f.i) { var im = el("img", "fav-ic"); im.src = f.i; im.alt = ""; b.insertBefore(im, b.firstChild); }
      b.oncontextmenu = function (ev) { ev.preventDefault(); V.enviar("pop:renomear", f.u); }; c.appendChild(b); });
  }

  function aplicarTema(texto) {
    var P = new URLSearchParams(texto || ""), S = document.documentElement.style;
    [["c1", "--c1"], ["c2", "--c2"], ["bg", "--bg"], ["sup", "--sup"], ["campo", "--campo"], ["su", "--su"], ["tx", "--tx"]].forEach(function (p) { if (/^[0-9a-f]{6}$/i.test(P.get(p[0]) || "")) S.setProperty(p[1], "#" + P.get(p[0])); });
  }

  function desenhar(d) {
    D = d; aplicarTema(d.tema);
    var p = $("principal"), s = $("sub"); p.textContent = ""; s.textContent = ""; s.hidden = true;
    var direita = Math.max(8, d.larg - d.x);
    p.style.right = direita + "px"; p.hidden = false;
    if (d.tipo === "menu") {
      p.style.width = "300px"; desenharMenu(p);
      if (d.sub) {
        s.hidden = false; s.style.width = d.sub === "downloads" ? "340px" : "320px";
        s.style.right = (direita + 300 + 8) + "px";
        if (d.sub === "downloads") desenharDownloads(s); else if (d.sub === "historico") desenharHistorico(s); else desenharFavoritos(s);
      }
    } else { p.style.width = "340px"; desenharDownloads(p); }
    if (s.getBoundingClientRect().left < 4) { s.style.right = "auto"; s.style.left = "8px"; }
  }

  V.ouvir("pop:estado", desenhar);
  document.body.addEventListener("click", function (e) { if (e.target === document.body || e.target === document.documentElement) V.enviar("pop:fechar"); });
  document.addEventListener("keydown", function (e) { if (e.key === "Escape") V.enviar("pop:fechar"); });
  V.enviar("pop:pronto");
})();

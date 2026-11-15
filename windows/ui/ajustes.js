(function () {
  var V = window.velox, $ = function (id) { return document.getElementById(id); };
  function el(tag, texto, cls) { var e = document.createElement(tag); if (texto != null) e.textContent = texto; if (cls) e.className = cls; return e; }
  function btn(texto, fn, cls) { var b = el("button", texto, cls); b.onclick = fn; return b; }
  var PERM = { media: "câmera/microfone", geolocation: "localização", notifications: "notificações", midi: "MIDI", "clipboard-read": "área de transferência", "display-capture": "gravação de tela" };

  // preenche a parte "já andada" das barras (a cor do tema) conforme o valor
  function faixas() {
    Array.prototype.forEach.call(document.querySelectorAll("input[type=range]"), function (r) {
      var mn = +r.min || 0, mx = +r.max || 100;
      r.style.setProperty("--p", (mx > mn ? ((+r.value - mn) / (mx - mn)) * 100 : 0) + "%");
    });
  }
  document.addEventListener("input", function (ev) { if (ev.target && ev.target.type === "range") faixas(); });

  function desenhar(e) {
    $("versao").textContent = "Versão " + e.versao;
    $("bloqueio").checked = e.bloqueio; $("continuar").checked = e.continuar; $("barrafav").checked = e.mostrarFavoritos;
    $("bloq-n").textContent = e.bloqueados ? "· " + e.bloqueados + " bloqueados nesta sessão" : "";
    $("bloq-modo").textContent = e.modoBloqueio === "ghostery" ? "Motor completo com listas atualizadas (EasyList e uBlock)." : e.modoBloqueio === "lista" ? "Motor básico por lista de domínios (o completo não carregou — precisa de internet na primeira vez)." : "Carregando as listas…";

    if (document.activeElement !== $("rol-larg")) $("rol-larg").value = e.rolagem.largura;
    $("rol-modo").value = e.rolagem.modo; $("rol-cor").value = e.rolagem.cor; $("rol-n").textContent = "· " + e.rolagem.largura + " px";
    desenharTema(e.tema);
    faixas();
    $("f-estado").textContent = e.temFundo ? "· foto de fundo ligada" : "· sem foto de fundo";
    ["favoritos", "tema", "senhas"].forEach(function (k) { $("si-" + k).checked = !!e.sincItens[k]; });
    var c = e.cofre;
    $("cofre-estado").textContent = !c.existe ? "Ainda não há cofre." : c.aberto ? "Cofre aberto" : "Cofre trancado";
    $("cofre-estado").className = c.aberto ? "ok" : "";
    $("cofre-abrir").textContent = c.existe ? "Abrir cofre" : "Criar cofre";
    $("cofre-abrir").hidden = c.aberto; $("cofre-travar").hidden = !c.aberto; $("cofre-trocar").hidden = !c.aberto;
    var l = $("cofre-lista"); l.textContent = "";
    c.itens.forEach(function (i) {
      var linha = el("div", null, "item"), t = el("div", null, "t");
      t.appendChild(el("div", i.origem.replace(/^https?:\/\//, ""))); t.appendChild(el("div", i.usuario || "(sem usuário)", "s"));
      var a = el("span");
      var ver = btn("Mostrar", function () { V.chamar("cofre:revelar", i.id).then(function (r) { if (r.ok) { ver.textContent = r.senha; setTimeout(function () { ver.textContent = "Mostrar"; }, 8000); } else $("cofre-erro").textContent = r.erro; }); });
      a.appendChild(ver); a.appendChild(document.createTextNode(" "));
      a.appendChild(btn("Copiar", function () { V.chamar("cofre:copiar", i.id); }));
      a.appendChild(document.createTextNode(" "));
      a.appendChild(btn("Apagar", function () { V.chamar("cofre:apagar", i.id).then(recarregar); }, "perigo"));
      linha.appendChild(t); linha.appendChild(a); l.appendChild(linha);
    });
    if (c.aberto && !c.itens.length) l.appendChild(el("p", "Nenhuma senha salva ainda. Entre em um site e o Velox pergunta se quer salvar.", "nota"));

    var xl = $("ext-lista"); xl.textContent = "";
    e.extensoes.forEach(function (x) {
      var linha = el("div", null, "item"), t = el("div", null, "t");
      t.appendChild(el("div", x.nome)); t.appendChild(el("div", x.erro ? "Erro: " + x.erro : x.ligada ? "Ligada" : "Desligada", "s"));
      var a = el("span");
      a.appendChild(btn(x.ligada ? "Desligar" : "Ligar", function () { V.chamar("ext:alternar", x.id).then(function (r) { $("ext-msg").textContent = r.ok ? "" : r.erro; recarregar(); }); }));
      a.appendChild(document.createTextNode(" "));
      a.appendChild(btn("Remover", function () { V.chamar("ext:remover", x.id).then(recarregar); }, "perigo"));
      linha.appendChild(t); linha.appendChild(a); xl.appendChild(linha);
    });

    var sl = $("sites-lista"); sl.textContent = "";
    if (!e.sites.length) sl.appendChild(el("p", "Nenhuma permissão guardada.", "nota"));
    e.sites.forEach(function (s) {
      var linha = el("div", null, "item"), t = el("div", null, "t");
      t.appendChild(el("div", s.origem.replace(/^https?:\/\//, "")));
      t.appendChild(el("div", Object.keys(s.permissoes).map(function (k) { return (PERM[k] || k) + ": " + (s.permissoes[k] === "permitir" ? "permitido" : "bloqueado"); }).join(" · "), "s"));
      linha.appendChild(t); linha.appendChild(btn("Remover", function () { V.chamar("sites:remover", s.origem).then(desenhar); }, "perigo")); sl.appendChild(linha);
    });

    $("sync-fora").hidden = e.sync.entrou; $("sync-dentro").hidden = !e.sync.entrou;
    $("sync-info").textContent = e.sync.email + " · " + e.sync.favoritos + " favoritos" + (e.sync.ultimo ? " · sincronizado às " + new Date(e.sync.ultimo).toLocaleTimeString("pt-BR") : "");
    $("sync-msg").textContent = e.sync.resumo || $("sync-msg").textContent;
  }
  var VGX = window.VGX, temaAtual = null;
  var GEAR = '<path d="M10.32 5.00 L10.87 2.47 L13.13 2.47 L13.68 5.00 L15.76 5.86 L17.94 4.46 L19.54 6.06 L18.14 8.24 L19.00 10.32 L21.53 10.87 L21.53 13.13 L19.00 13.68 L18.14 15.76 L19.54 17.94 L17.94 19.54 L15.76 18.14 L13.68 19.00 L13.13 21.53 L10.87 21.53 L10.32 19.00 L8.24 18.14 L6.06 19.54 L4.46 17.94 L5.86 15.76 L5.00 13.68 L2.47 13.13 L2.47 10.87 L5.00 10.32 L5.86 8.24 L4.46 6.06 L6.06 4.46 L8.24 5.86 Z" stroke-linejoin="round"/><circle cx="12" cy="12" r="3.1"/>';
  function chips(id, lista, ativo, fazer, pinta) {
    var box = $(id); box.textContent = "";
    lista.forEach(function (x) {
      var b = el("button"); b.type = "button"; b.dataset.ativa = String(x[0] === ativo);
      var m = pinta ? pinta(x) : null; if (m) b.appendChild(m);
      b.appendChild(document.createTextNode(x[1])); b.onclick = function () { fazer(x); }; box.appendChild(b);
    });
  }
  function salvarTema(mud) { var t = Object.assign({}, temaAtual, mud); V.chamar("ajustes:tema", VGX.params(t)).then(desenhar); }
  function desenharTema(texto) {
    temaAtual = VGX.doEndereco(new URLSearchParams(texto));
    var t = temaAtual;
    chips("t-cores", VGX.CORES, (VGX.CORES.filter(function (c) { return c[2] === t.c1 && c[3] === t.c2; })[0] || [])[0], function (c) { salvarTema({ c1: c[2], c2: c[3] }); }, function (c) { var i = el("i"); i.style.background = "linear-gradient(135deg,#" + c[2] + ",#" + c[3] + ")"; return i; });
    chips("t-bases", VGX.BASES, t.base, function (b) { salvarTema({ base: b[0] }); });
    chips("t-fundos", VGX.FUNDOS, t.fu, function (f) { salvarTema({ fu: f[0] }); });
    chips("t-estilos", VGX.ESTILOS, t.ti, function (e) { salvarTema({ ti: e[0] }); });
    chips("t-pacotes", VGX.PACOTES, t.pk, function (p) { salvarTema({ pk: p[0] }); }, function (p) { var s = el("span"); s.innerHTML = VGX.pacoteSvg(p[0], GEAR, t.c1, t.c2, p[0]); return s; });
    chips("t-formato", [["futurista", "Futurista"], ["redondo", "Redondo"]], t.fo, function (f) { salvarTema({ fo: f[0] }); });
    chips("t-luz", VGX.LUZES, t.lz, function (l) { salvarTema({ lz: l[0] }); });
    chips("t-luzcor", t.lz === "cor" ? VGX.CORES : [], t.lc, function (c) { salvarTema({ lc: c[2] }); }, function (c) { var i = el("i"); i.style.background = "#" + c[2]; i.style.width = "14px"; i.style.height = "14px"; return i; });
    $("t-an").checked = t.an;
    if (document.activeElement !== $("t-br")) $("t-br").value = t.br;
    if (document.activeElement !== $("t-tr")) $("t-tr").value = t.tr;
    if (document.activeElement !== $("t-dim")) $("t-dim").value = t.dim;
  }
  function buscarFotos() { V.chamar("ajustes:buscar-fotos", $("f-busca").value.trim() || "wallpaper"); }
  $("f-buscar").onclick = buscarFotos;
  $("f-busca").onkeydown = function (e) { if (e.key === "Enter") buscarFotos(); };
  $("f-escolher").onclick = function () { V.chamar("ajustes:escolher-fundo").then(desenhar); };
  $("f-remover").onclick = function () { V.chamar("ajustes:remover-fundo").then(desenhar); };
  $("t-an").onchange = function () { salvarTema({ an: this.checked }); };
  $("t-br").onchange = function () { salvarTema({ br: +this.value }); };
  $("t-tr").onchange = function () { salvarTema({ tr: +this.value }); };
  $("t-dim").onchange = function () { salvarTema({ dim: +this.value }); };
  function salvarItens() { V.chamar("ajustes:sync-itens", { favoritos: $("si-favoritos").checked, tema: $("si-tema").checked, senhas: $("si-senhas").checked }).then(desenhar); }
  ["favoritos", "tema", "senhas"].forEach(function (k) { $("si-" + k).onchange = salvarItens; });
  function recarregar() { return V.chamar("ajustes:estado").then(desenhar); }

  $("bloqueio").onchange = function () { V.chamar("ajustes:bloqueio", this.checked).then(desenhar); };
  function salvarRolagem() { V.chamar("ajustes:rolagem", { modo: $("rol-modo").value, largura: +$("rol-larg").value, cor: $("rol-cor").value }).then(desenhar); }
  $("rol-modo").onchange = salvarRolagem; $("rol-cor").onchange = salvarRolagem; $("rol-larg").onchange = salvarRolagem;
  $("continuar").onchange = function () { V.chamar("ajustes:continuar", this.checked).then(desenhar); };
  $("barrafav").onchange = function () { V.chamar("ajustes:favoritos-barra", this.checked).then(desenhar); };
  $("cofre-abrir").onclick = function () { $("cofre-erro").textContent = ""; V.chamar("ajustes:estado").then(function (e) { return V.chamar(e.cofre.existe ? "cofre:abrir" : "cofre:criar"); }).then(function (r) { if (r && !r.ok) $("cofre-erro").textContent = r.erro; recarregar(); }); };
  $("cofre-travar").onclick = function () { V.chamar("cofre:travar").then(desenhar); };
  $("cofre-trocar").onclick = function () { $("cofre-erro").textContent = ""; V.chamar("cofre:trocar").then(function (r) { if (!r.ok) $("cofre-erro").textContent = r.erro; }); };
  $("ext-instalar").onclick = function () {
    $("ext-msg").textContent = "Instalando…";
    V.chamar("ext:instalar", $("ext-id").value).then(function (r) { $("ext-msg").textContent = r.ok ? "Instalada: " + (r.nome || "") : r.erro; if (r.ok) $("ext-id").value = ""; recarregar(); });
  };
  $("ext-pasta").onclick = function () { V.chamar("ext:pasta").then(function (r) { $("ext-msg").textContent = r.ok ? "Carregada: " + (r.nome || "") : r.erro; recarregar(); }); };
  $("sync-entrar").onclick = function () {
    $("sync-msg").textContent = "Entrando…";
    V.chamar("sync:entrar", $("sync-email").value, $("sync-senha").value).then(function (r) { $("sync-msg").textContent = r.ok ? "Pronto: " + r.resumo : r.erro; $("sync-senha").value = ""; recarregar(); });
  };
  $("atu-procurar").onclick = function () {
    var b = this; b.disabled = true; $("atu-msg").textContent = "Procurando…";
    V.chamar("ajustes:procurar-atualizacao").then(function (r) {
      var m = !r ? "Não consegui procurar agora." : r.estado === "em-dia" ? "O Velox já está na versão mais nova (" + r.versao + ")." : r.estado === "nova" ? "Há uma versão nova (" + r.nova + "). A janela de instalação já apareceu." : r.estado === "ocupado" ? "Já estou procurando…" : r.estado === "teste" ? "Atualização desligada neste modo de teste." : (r.erro || "Não consegui procurar agora.");
      $("atu-msg").textContent = m; b.disabled = false;
    }).catch(function () { $("atu-msg").textContent = "Não consegui procurar agora."; b.disabled = false; });
  };
  ["tour", "termos", "novidades"].forEach(function (q) { $("sobre-" + q).onclick = function () { V.chamar("ajustes:sobre", q); }; });
  $("sync-agora").onclick = function () { V.chamar("sync:agora").then(function (r) { $("sync-msg").textContent = r.ok ? "Sincronizado: " + r.resumo : r.erro; recarregar(); }); };
  $("sync-sair").onclick = function () { V.chamar("sync:sair").then(desenhar); };
  recarregar();
  setInterval(recarregar, 4000);
})();

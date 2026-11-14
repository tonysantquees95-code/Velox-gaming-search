(function () {
  var V = window.velox, $ = function (id) { return document.getElementById(id); };
  function el(tag, texto, cls) { var e = document.createElement(tag); if (texto != null) e.textContent = texto; if (cls) e.className = cls; return e; }
  function btn(texto, fn, cls) { var b = el("button", texto, cls); b.onclick = fn; return b; }
  var PERM = { media: "câmera/microfone", geolocation: "localização", notifications: "notificações", midi: "MIDI", "clipboard-read": "área de transferência", "display-capture": "gravação de tela" };

  function desenhar(e) {
    $("versao").textContent = "Versão " + e.versao;
    $("bloqueio").checked = e.bloqueio; $("continuar").checked = e.continuar; $("barrafav").checked = e.mostrarFavoritos;
    $("bloq-n").textContent = e.bloqueados ? "· " + e.bloqueados + " bloqueados nesta sessão" : "";
    $("bloq-modo").textContent = e.modoBloqueio === "ghostery" ? "Motor completo com listas atualizadas (EasyList e uBlock)." : e.modoBloqueio === "lista" ? "Motor básico por lista de domínios (o completo não carregou — precisa de internet na primeira vez)." : "Carregando as listas…";

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
  }
  function recarregar() { return V.chamar("ajustes:estado").then(desenhar); }

  $("bloqueio").onchange = function () { V.chamar("ajustes:bloqueio", this.checked).then(desenhar); };
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
    V.chamar("sync:entrar", $("sync-email").value, $("sync-senha").value).then(function (r) { $("sync-msg").textContent = r.ok ? "Pronto: favoritos sincronizados." : r.erro; $("sync-senha").value = ""; recarregar(); });
  };
  $("sync-agora").onclick = function () { V.chamar("sync:agora").then(function (r) { $("sync-msg").textContent = r.ok ? "Sincronizado." : r.erro; recarregar(); }); };
  $("sync-sair").onclick = function () { V.chamar("sync:sair").then(desenhar); };
  recarregar();
  setInterval(recarregar, 4000);
})();

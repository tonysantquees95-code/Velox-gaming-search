(function () {
  var V = window.velox, $ = function (id) { return document.getElementById(id); };
  function desenhar(l) {
    var el = $("lista"); el.textContent = "";
    if (!l.length) { var p = document.createElement("p"); p.className = "nota"; p.textContent = "Nada por aqui."; el.appendChild(p); return; }
    var dia = "";
    l.forEach(function (h) {
      var d = new Date(h.t).toLocaleDateString("pt-BR", { weekday: "long", day: "numeric", month: "long" });
      if (d !== dia) { dia = d; var t = document.createElement("h2"); t.style.marginTop = "18px"; t.textContent = d; el.appendChild(t); }
      var i = document.createElement("div"); i.className = "item";
      var a = document.createElement("div"); a.className = "t"; a.style.cursor = "pointer";
      var n = document.createElement("div"); n.textContent = h.n || h.u;
      var u = document.createElement("div"); u.className = "s"; u.textContent = new Date(h.t).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" }) + " · " + h.u;
      a.appendChild(n); a.appendChild(u); a.onclick = function () { V.enviar("historico:abrir", h.u); };
      var x = document.createElement("button"); x.textContent = "×"; x.title = "Apagar"; x.onclick = function () { V.enviar("historico:apagar", h.u); carregar(); };
      i.appendChild(a); i.appendChild(x); el.appendChild(i);
    });
  }
  function carregar() { V.chamar("historico:lista", $("q").value).then(desenhar); }
  $("q").oninput = carregar;
  $("limpar").onclick = function () { V.enviar("historico:limpar"); setTimeout(carregar, 100); };
  carregar();
})();

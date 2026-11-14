(function () {
  var def = {};
  try { def = JSON.parse(decodeURIComponent(location.hash.slice(1))); } catch (e) { def = {}; }
  var $ = function (id) { return document.getElementById(id); };
  $("t").textContent = def.titulo || "Velox"; $("x").textContent = def.texto || "";
  var campos = def.campos || [], botoes = def.botoes || [{ id: "ok", rotulo: "OK", primario: true }];
  campos.forEach(function (c) {
    var l = document.createElement("label"); l.textContent = c.rotulo; l.htmlFor = "c-" + c.id;
    var i = document.createElement("input"); i.type = c.tipo === "password" ? "password" : "text"; i.id = "c-" + c.id; i.autocomplete = "off";
    $("campos").appendChild(l); $("campos").appendChild(i);
  });
  function responder(botao) {
    var valores = {}; campos.forEach(function (c) { valores[c.id] = $("c-" + c.id).value; });
    window.velox.enviar("dialogo:" + def.id, { botao: botao, valores: valores });
  }
  botoes.forEach(function (b) {
    var e = document.createElement("button"); e.textContent = b.rotulo; if (b.primario) e.className = "p";
    e.onclick = function () { responder(b.id); }; $("botoes").appendChild(e);
  });
  var primeiro = campos.length ? $("c-" + campos[0].id) : null;
  if (primeiro) primeiro.focus();
  document.addEventListener("keydown", function (ev) {
    if (ev.key === "Enter") { var p = botoes.filter(function (b) { return b.primario; })[0]; if (p) responder(p.id); }
    if (ev.key === "Escape") responder("cancelar");
  });
})();

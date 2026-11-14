/* Tema do Velox nas telas internas: lê as cores do endereço e também recebe mudanças ao vivo (sem recarregar a página). */
(function () {
  var S = document.documentElement.style;
  function aplicar(texto) {
    var P = new URLSearchParams(texto || "");
    [["c1", "--c1"], ["c2", "--c2"], ["bg", "--bg"], ["sup", "--sup"], ["campo", "--campo"], ["su", "--su"], ["tx", "--tx"]].forEach(function (p) {
      var v = P.get(p[0]); if (/^[0-9a-f]{6}$/i.test(v || "")) S.setProperty(p[1], "#" + v);
    });
    window.dispatchEvent(new CustomEvent("velox-tema", { detail: texto }));
  }
  aplicar(location.search.slice(1));
  if (window.velox && window.velox.ouvir) window.velox.ouvir("tema", aplicar);
})();

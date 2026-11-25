/* Ajustes em página com menu à esquerda (como as configurações do Edge): escolhe a seção no menu ou pesquisa por nome. */
(function () {
  var main = document.querySelector("main");
  if (!main) return;
  var secoes = [].slice.call(main.querySelectorAll(":scope > section.cartao"));
  var ICONES = {
    "Privacidade e bloqueio": '<path d="M12 3l7 3v5c0 5-3.2 8.2-7 10-3.8-1.8-7-5-7-10V6z"/>',
    "Desempenho": '<path d="M20 14.5A8 8 0 019.5 4a8 8 0 1010.5 10.5z"/>',
    "Tema": '<circle cx="12" cy="12" r="8"/><circle cx="9" cy="10" r="1"/><circle cx="14" cy="9" r="1"/><circle cx="15" cy="14" r="1"/>',
    "Barra de rolagem": '<rect x="8" y="3" width="8" height="18" rx="4"/><path d="M12 7v4"/>',
    "Senhas": '<circle cx="7.5" cy="15.5" r="3.5"/><path d="M10 13L20 3M17 6L20 9M14 9L16.5 11.5"/>',
    "Extensões": '<path d="M5 9h4V7a2 2 0 114 0v2h4v4h-2a2 2 0 100 4h2v3H5z"/>',
    "Sites e permissões": '<circle cx="12" cy="12" r="8.5"/><path d="M3.5 12h17M12 3.5c3 3.5 3 13.5 0 17M12 3.5c-3 3.5-3 13.5 0 17"/>',
    "Atualizações": '<path d="M19 12A7 7 0 1 1 16.95 7.05"/><path d="M19 4V8H15"/>',
    "Sobre o Velox": '<circle cx="12" cy="12" r="8.5"/><path d="M12 11v5M12 8h.01"/>',
    "Sincronização com o celular": '<path d="M7 17a4 4 0 010-8 5 5 0 019.5-1A4.5 4.5 0 0117 17z"/>'
  };
  var titulo = main.querySelector("h1"), sub = main.querySelector(".sub");
  var pagina = document.createElement("div"); pagina.className = "pagina";
  var nav = document.createElement("nav"); nav.className = "menu-aj"; nav.setAttribute("aria-label", "Seções dos ajustes");
  var corpo = document.createElement("div"); corpo.className = "corpo-aj";
  var cab = document.createElement("div"); cab.className = "cab-aj"; if (titulo) cab.appendChild(titulo); if (sub) cab.appendChild(sub);
  var busca = document.createElement("input"); busca.type = "text"; busca.placeholder = "Pesquisar nos ajustes"; busca.setAttribute("aria-label", "Pesquisar nos ajustes"); busca.className = "busca-aj";
  var ver = document.createElement("div"); ver.className = "ver-aj";
  nav.appendChild(cab); nav.appendChild(busca);
  var botoes = [];
  var atual = 0;
  function nome(s) { var h = s.querySelector("h2"); return h ? h.textContent.trim() : ""; }
  secoes.forEach(function (s, i) {
    var b = document.createElement("button"); b.type = "button"; b.className = "nav-item";
    b.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true">' + (ICONES[nome(s)] || '<circle cx="12" cy="12" r="8"/>') + "</svg>";
    var t = document.createElement("span"); t.textContent = nome(s); b.appendChild(t);
    b.onclick = function () { busca.value = ""; atual = i; mostrar(); };
    nav.appendChild(b); botoes.push(b);
    ver.appendChild(s);
  });
  corpo.appendChild(ver);
  pagina.appendChild(nav); pagina.appendChild(corpo);
  main.textContent = ""; main.appendChild(pagina); main.classList.add("com-menu");
  function sem(t) { return t.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase(); }
  function mostrar() {
    var q = sem(busca.value.trim()), achou = 0;
    secoes.forEach(function (s, i) {
      var ok = q ? sem(s.textContent).indexOf(q) >= 0 : i === atual;
      s.hidden = !ok; if (ok) achou++;
      botoes[i].classList.toggle("ativo", q ? ok : i === atual);
    });
    var vazio = ver.querySelector(".vazio-aj");
    if (!achou) { if (!vazio) { vazio = document.createElement("p"); vazio.className = "nota vazio-aj"; vazio.textContent = "Nada encontrado nos ajustes."; ver.appendChild(vazio); } }
    else if (vazio) vazio.remove();
  }
  busca.addEventListener("input", mostrar);
  mostrar();
  /* abre direto numa seção: ajustes.html#Tema */
  var alvo = decodeURIComponent(location.hash.slice(1) || "");
  if (alvo) secoes.forEach(function (s, i) { if (nome(s) === alvo) { atual = i; mostrar(); } });
})();

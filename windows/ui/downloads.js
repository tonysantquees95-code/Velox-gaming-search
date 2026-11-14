var V = window.velox;
function mb(n){return n>=1048576?(n/1048576).toFixed(1)+" MB":(n/1024).toFixed(0)+" KB"}
function desenhar(l){
  var el=document.getElementById("lista"); el.textContent="";
  if(!l.length){var v=document.createElement("div");v.className="vazio";v.textContent="Nenhum download ainda.";el.appendChild(v);return}
  l.forEach(function(d){
    var c=document.createElement("div");c.className="item";
    var n=document.createElement("div");n.className="nome";n.textContent=d.nome;c.appendChild(n);
    var i=document.createElement("div");i.className="info";
    var pct=d.total?Math.round(d.recebido*100/d.total):0;
    i.textContent=d.estado==="baixando"?mb(d.recebido)+(d.total?" de "+mb(d.total)+" · "+pct+"%":"")+" · "+mb(d.velocidade||0)+"/s":d.estado==="pronto"?mb(d.recebido)+" · pronto":d.estado;
    c.appendChild(i);
    if(d.estado==="baixando"){var b=document.createElement("div");b.className="barra";var f=document.createElement("i");f.style.width=(d.total?pct:30)+"%";b.appendChild(f);c.appendChild(b)}
    var a=document.createElement("div");a.className="acoes";
    function bt(t,fn){var x=document.createElement("button");x.textContent=t;x.onclick=fn;a.appendChild(x)}
    if(d.estado==="pronto"){bt("Executar",function(){V.enviar("download-abrir",d.id)});bt("Mostrar na pasta",function(){V.enviar("download-pasta",d.id)})}
    if(d.estado==="baixando")bt("Cancelar",function(){V.enviar("download-cancelar",d.id)});
    c.appendChild(a);el.appendChild(c);
  });
}
V.ouvir("downloads",desenhar);V.chamar("downloads-lista").then(desenhar);
document.getElementById("limpar").onclick=function(){V.enviar("download-limpar")};

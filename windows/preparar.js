// Copia a página inicial do Velox Android (a mesma do celular) para dentro do app do Windows.
const fs = require("fs"), path = require("path");
const de = path.join(__dirname, "..", "app", "src", "main", "assets");
const para = path.join(__dirname, "inicio");
fs.mkdirSync(para, { recursive: true });
for (const f of ["inicio.html", "vgx.js", "vgx.css", "marcas.js", "youtube.js"]) fs.copyFileSync(path.join(de, f), path.join(para, f));
console.log("página inicial copiada");

const { contextBridge, ipcRenderer } = require("electron");
contextBridge.exposeInMainWorld("velox", {
  enviar: (canal, ...a) => ipcRenderer.send(canal, ...a),
  chamar: (canal, ...a) => ipcRenderer.invoke(canal, ...a),
  ouvir: (canal, fn) => ipcRenderer.on(canal, (_e, ...a) => fn(...a)),
});

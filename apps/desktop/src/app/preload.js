const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('versions', {
  // we can also expose variables, not just functions
  node: () => process.versions.node,
  chrome: () => process.versions.chrome,
  electron: () => process.versions.electron,
  
  ping: () => ipcRenderer.invoke('ping'),
});

contextBridge.exposeInMainWorld('electronAPI', {
  // Definimos una función que el renderer podrá llamar
  cargarVista: (nombre) => ipcRenderer.invoke('obtener-vista', nombre)
});
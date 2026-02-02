// preload.js
const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  sendData: (channel, data) => ipcRenderer.send(channel, data),
  //receiveData: (channel, func) => ipcRenderer.on(channel, (event, ...args) => func(...args)), // mem leak
  receiveData: (channel, func) => {
    const subscription = (event, ...args) => func(...args);
    ipcRenderer.on(channel, subscription);
    
    // Retorna una función para remover el listener
    return () => ipcRenderer.removeListener(channel, subscription);
  },

  invokeAction: (channel, data) => ipcRenderer.invoke(channel, data)
});

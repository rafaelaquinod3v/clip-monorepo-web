// preload.js
const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  sendData: (channel, data) => ipcRenderer.send(channel, data),
  receiveData: (channel, func) => ipcRenderer.on(channel, (event, ...args) => func(...args)),
  invokeAction: (channel, data) => ipcRenderer.invoke(channel, data)
});

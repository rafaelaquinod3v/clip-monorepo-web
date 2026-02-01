const { app, BrowserWindow, Menu } = require('electron');
const path = require('path');

// Fix for WSL freezing
app.disableHardwareAcceleration();
app.commandLine.appendSwitch('no-sandbox');
app.commandLine.appendSwitch('disable-software-rasterizer');

function createWindow() {
  const win = new BrowserWindow({
    width: 900,
    height: 700,
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false,
      // Prevents the renderer from trying to use GPU features
      offscreen: false 
    }
  });

  win.loadFile(path.join(__dirname, 'index.html'));
  
  // Optional: Open DevTools to see if there are console errors
  // win.webContents.openDevTools();
}

app.whenReady().then(() => {
  const template = [{ label: 'Clip Desktop', submenu: [{ role: 'quit' }] }];
  const menu = Menu.buildFromTemplate(template);
  Menu.setApplicationMenu(menu);

  createWindow();
});

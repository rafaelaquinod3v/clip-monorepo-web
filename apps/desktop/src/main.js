const { app, BrowserWindow, Menu, ipcMain } = require('electron');
const path = require('path');
const fs = require('fs');

function createWindow() {
  const win = new BrowserWindow({
    width: 900,
    height: 700,
    // Optional: Add a native icon for Windows (.ico)
    // icon: path.join(__dirname, 'assets/icon.ico'), 
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'app', 'preload.js'),
    }
  });

  win.loadFile(path.join(__dirname, 'index.html'));
  
  // Optional: Open DevTools to see if there are console errors
  // win.webContents.openDevTools();
}

app.whenReady().then(() => {
  // Native Windows menus are fast, you can expand this template 
  // without fear of lag now.
  const template = [
    { 
      label: 'Clip Desktop', 
      submenu: [
        { role: 'about' },
        { type: 'separator' },
        { role: 'quit' }
      ] 
    },
    {
      label: 'View',
      submenu: [
        { role: 'reload' },
        { role: 'toggleDevTools' }
      ]
    }
  ];
  
  const menu = Menu.buildFromTemplate(template);
  Menu.setApplicationMenu(menu);
  
  ipcMain.handle('ping', () => 'pong');

  // Escuchamos la petición de "obtener-vista"
  ipcMain.handle('obtener-vista', async (event, nombreVista) => {
    try {
      // Construimos la ruta absoluta de forma segura
      const ruta = path.join(__dirname, 'app', 'views', `${nombreVista}.html`);
      const contenido = fs.readFileSync(ruta, 'utf8');
      return contenido;
    } catch (error) {
      return `<h1>Error 404</h1><p>No se encontró la vista: ${nombreVista}</p>`;
    }
  });


  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

// Windows specific: Quit when all windows are closed
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
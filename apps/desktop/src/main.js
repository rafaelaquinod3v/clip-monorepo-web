const { app, BrowserWindow, Menu } = require('electron');
const path = require('path');

function createWindow() {
  const win = new BrowserWindow({
    width: 900,
    height: 700,
    // Optional: Add a native icon for Windows (.ico)
    // icon: path.join(__dirname, 'assets/icon.ico'), 
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false,
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

  createWindow();
});

// Windows specific: Quit when all windows are closed
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
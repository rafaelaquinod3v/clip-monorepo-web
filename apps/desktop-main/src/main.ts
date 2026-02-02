import { app, BrowserWindow } from 'electron';
import * as path from 'path';

let win: BrowserWindow;

function createWindow() {
  win = new BrowserWindow({
    width: 800,
    height: 600,
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false
    }
  });

  // En desarrollo carga la URL de nx serve, en prod el archivo físico
  const isDev = !app.isPackaged;
  if (isDev) {
    win.loadURL('http://localhost:4200');
  } else {
    win.loadFile(path.join(__dirname, '../frontend-app/browser/index.html')); // TODO: conf
  }
}

app.on('ready', createWindow);

// Windows specific: Quit when all windows are closed
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
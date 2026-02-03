// apps/desktop-main/src/main.ts
import { app, BrowserWindow } from 'electron';
import * as path from 'path';
import { registerIpcHandlers } from './ipc-handlers';

let win: BrowserWindow;
let store: any;

const isDev = !app.isPackaged;
const preloadPath = isDev 
  ? path.join(app.getAppPath(), 'src', 'preload.js') 
  : path.join(app.getAppPath(), 'dist', 'preload.js');

async function initStore() {
  const StoreClass = (await import('electron-store')).default;
  store = new StoreClass();
}

function createWindow() {
  win = new BrowserWindow({
    width: 800,
    height: 600,
    show: false,
    webPreferences: {
      preload: preloadPath,
      contextIsolation: true,
      nodeIntegration: false
    }
  });

  // Register the external handlers
  registerIpcHandlers(win, store);

  win.webContents.on('before-input-event', (event, input) => {
    if (input.key === 'Escape') {
      if (win.isFullScreen()) win.setFullScreen(false);
      else if (win.isMaximized()) win.unmaximize();
    }
  });

  if (isDev) {
    win.loadURL('http://localhost:4200');
  } else {
    const indexPath = path.join(app.getAppPath(), 'dist/apps/desktop-ui/browser/index.html'); //TODO: verify
    win.loadFile(indexPath);
  }
}

app.whenReady().then(async () => {
  await initStore();
  createWindow();
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

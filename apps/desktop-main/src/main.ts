import { app, BrowserWindow, ipcMain, net, safeStorage } from 'electron';
import * as path from 'path';
import axios from 'axios';
import type Store from 'electron-store';

// --- 1. TYPES & CONFIGURATION ---
interface AppStorage {
  auth_token?: string;
}

let win: BrowserWindow;
let store: Store<AppStorage>;

const isDev = !app.isPackaged;
const preloadPath = isDev 
  ? path.join(app.getAppPath(), 'src', 'preload.js') 
  : path.join(app.getAppPath(), 'dist', 'preload.js');

// --- 2. HELPER FUNCTIONS ---

async function initStore() {
  const StoreClass = (await import('electron-store')).default;
  store = new StoreClass<AppStorage>();
}

function isTokenExpired(token: string): boolean {
  try {
    const base64Payload = token.split('.')[1];
    const payload = JSON.parse(Buffer.from(base64Payload, 'base64').toString());
    const currentTime = Math.floor(Date.now() / 1000);
    return payload.exp < currentTime;
  } catch (error) {
    return true;
  }
}

// --- 3. WINDOW MANAGEMENT ---

function createWindow() {
  win = new BrowserWindow({
    width: 800,
    height: 600,
    show: false, // Hidden until Angular is ready
    webPreferences: {
      preload: preloadPath,
      contextIsolation: true,
      nodeIntegration: false
    }
  });

  // Global Input Shortcuts
  win.webContents.on('before-input-event', (event, input) => {
    if (input.key === 'Escape') {
      if (win.isFullScreen()) win.setFullScreen(false);
      else if (win.isMaximized()) win.unmaximize();
    }
  });

  // Load Content
  if (isDev) {
    win.loadURL('http://localhost:4200');
  } else {
    const indexPath = path.join(app.getAppPath(), 'dist/apps/desktop-ui/browser/index.html'); //TODO: check
    win.loadFile(indexPath);
  }
}

// --- 4. APP LIFECYCLE ---

app.whenReady().then(async () => {
  await initStore();
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

// --- 5. IPC HANDLERS ---

// Window Controls
ipcMain.on('ready-to-reveal', () => {
  if (win) {
    win.maximize();
    win.show();
    win.focus();
  }
});

ipcMain.handle('set-fullscreen', (event, shouldBeFull) => {
  win.setFullScreen(shouldBeFull);
});

// Auth Logic
ipcMain.handle('check-auth', () => {
  const encrypted = (store as any).get('auth_token');
  if (!encrypted) return { isAuthenticated: false };

  try {
    const buffer = Buffer.from(encrypted, 'latin1');
    const token = safeStorage.decryptString(buffer);

    if (!token || isTokenExpired(token)) {
      (store as any).delete('auth_token'); 
      return { isAuthenticated: false, reason: 'expired' };
    }
    return { isAuthenticated: true };
  } catch (e) {
    return { isAuthenticated: false };
  }
});

ipcMain.handle('login-request', async (event, credentials) => {
  try {
    const response = await axios.post('http://localhost:8080/api/users/login', credentials);
    const token = response.data.token;
    const encryptedToken = safeStorage.encryptString(token);
    
    (store as any).set('auth_token', encryptedToken.toString('latin1'));
    return { success: true };
  } catch (error) {
    return { success: false, error: 'Login failed' };
  }
});

ipcMain.handle('logout-request', async () => {
  try {
    (store as any).delete('auth_token');
    return { success: true };
  } catch (error) {
    return { success: false };
  }
});

// Data Fetches
ipcMain.handle('get-users', async (event, token: string) => {
  return new Promise((resolve, reject) => {
    const request = net.request({
      method: 'GET',
      protocol: 'http:',
      hostname: 'localhost',
      port: 8080,
      path: '/api/users/list'
    });

    request.setHeader('Authorization', `Bearer ${token}`);
    request.setHeader('Content-Type', 'application/json');

    request.on('response', (response) => {
      let data = '';
      response.on('data', (chunk) => { data += chunk; });
      response.on('end', () => {
        if (response.statusCode === 200) resolve(JSON.parse(data));
        else reject(`Error: ${response.statusCode}`);
      });
    });
    request.on('error', reject);
    request.end();
  });
});

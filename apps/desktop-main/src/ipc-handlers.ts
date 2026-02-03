// apps/desktop-main/src/ipc-handlers.ts
import { ipcMain, net, safeStorage, BrowserWindow } from 'electron';
import axios from 'axios';

// Helper: Token Expiration
function isTokenExpired(token: string): boolean {
  try {
    const base64Payload = token.split('.')[1];
    const payload = JSON.parse(Buffer.from(base64Payload, 'base64').toString());
    return payload.exp < Math.floor(Date.now() / 1000);
  } catch {
    return true;
  }
}

export function registerIpcHandlers(win: BrowserWindow, store: any) {
  // --- Window Controls ---
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

  // --- Authentication ---
  ipcMain.handle('check-auth', () => {
    const encrypted = store.get('auth_token');
    if (!encrypted) return { isAuthenticated: false };

    try {
      const buffer = Buffer.from(encrypted, 'latin1');
      const token = safeStorage.decryptString(buffer);

      if (!token || isTokenExpired(token)) {
        store.delete('auth_token');
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
      store.set('auth_token', encryptedToken.toString('latin1'));
      return { success: true };
    } catch (error) {
      return { success: false, error: 'Login failed' };
    }
  });

  ipcMain.handle('logout-request', () => {
    try {
      store.delete('auth_token');
      return { success: true };
    } catch {
      return { success: false };
    }
  });

  // --- Data Fetching ---
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
      request.on('response', (res) => {
        let data = '';
        res.on('data', (chunk) => { data += chunk; });
        res.on('end', () => {
          if (res.statusCode === 200) resolve(JSON.parse(data));
          else reject(`Error: ${res.statusCode}`);
        });
      });
      request.on('error', reject);
      request.end();
    });
  });
}

import { app, BrowserWindow, ipcMain, net } from 'electron';
import * as path from 'path';
import { safeStorage } from 'electron';
const axios = require('axios');
import type Store from 'electron-store';
//import Store from 'electron-store';
//const Store = require('electron-store');

/* interface StorageSchema {
  auth_token: string;
} */

//const store = new Store<StorageSchema>();
interface AppStorage {
  auth_token?: string;
  //theme?: string;
}

// Use the specific type instead of 'any'
let store: Store<AppStorage>;
async function initStore() {
  const StoreClass = (await import('electron-store')).default;
  store = new StoreClass<AppStorage>();
}
let win: BrowserWindow;
// En desarrollo carga la URL de nx serve, en prod el archivo físico
//const isDev = !app.isPackaged;
const isDev = !app.isPackaged;
const preloadPath = isDev 
? path.join(app.getAppPath(), 'src', 'preload.js') // Ruta en desarrollo
: path.join(app.getAppPath(), 'dist', 'preload.js'); // Ruta en producción

function createWindow() {
  win = new BrowserWindow({
    width: 800,
    height: 600,
    show: false,
    webPreferences: {
/*       nodeIntegration: true,
      contextIsolation: false, funcionando */ 
      //preload: path.join(__dirname, 'preload.js'),
      preload: preloadPath,
      contextIsolation: true, // Requerido para contextBridge
      nodeIntegration: false  // Recomendado por seguridad
    }
  });
  win.once('ready-to-show', () => {
    win.show(); // Show only when the UI is painted
  });


  if (isDev) {
    win.loadURL('http://localhost:4200');
  } else {
   //const indexPath = path.join(__dirname, '../../desktop-ui/browser/index.html');
  const indexPath = path.join(
    app.getAppPath(), // monorepo fix
    'dist/apps/desktop-ui/browser/index.html'
  );
    console.log('Buscando index en:', indexPath); // Esto te ayudará a debuguear
    win.loadFile(indexPath); // TODO: conf
  }
}

//app.on('ready', createWindow);

app.whenReady().then(async () => {
  await initStore();
  createWindow()

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

// Windows specific: Quit when all windows are closed
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});


ipcMain.on('mensaje-canal', (event, data) => {
  console.log('¡Mensaje recibido en el Proceso Principal!', data);  
  // Opcional: Responder de vuelta al renderer
  //event.reply('respuesta-canal', 'Proceso principal recibió tu: ' + data);
});

// Se usa .handle para coincidir con .invoke del preload
ipcMain.handle('pedir-datos', async (event, data) => {
  console.log('Datos recibidos desde Angular:', data);

  // Realiza alguna acción (leer archivos, base de datos, etc.)
  const resultado = `Procesado: ${JSON.stringify(data)}`;

  // Lo que retornes aquí llegará como respuesta al await de Angular
  return resultado; 
});

/* ipcMain.handle('get-users', async () => {
  return new Promise((resolve, reject) => {
    const request = net.request('http://localhost:8080/api/users/list');
    request.on('response', (response) => {
      let data = '';
      response.on('data', (chunk) => { data += chunk; });
      response.on('end', () => { resolve(JSON.parse(data)); });
    });
    request.on('error', (err) => reject(err));
    request.end();
  });
}); */
// apps/desktop-main/src/main.ts
ipcMain.handle('get-users', async (event, token: string) => {
  return new Promise((resolve, reject) => {
    // 1. Setup the request
    const request = net.request({
      method: 'GET',
      protocol: 'http:',
      hostname: 'localhost',
      port: 8080,
      path: '/api/users/list'
    });

    // 2. Inject the JWT Token
    request.setHeader('Authorization', `Bearer ${token}`);
    request.setHeader('Content-Type', 'application/json');

    request.on('response', (response) => {
      let data = '';
      response.on('data', (chunk) => { data += chunk; });
      response.on('end', () => {
        if (response.statusCode === 200) {
          resolve(JSON.parse(data));
        } else {
          reject(`Backend Error: ${response.statusCode}`);
        }
      });
    });

    request.on('error', (err) => reject(err));
    request.end();
  });
});

// apps/desktop-main/src/main.ts
//ipcMain.on('set-store', (event, key, val) => store.set(key, val));
//ipcMain.handle('get-store', (event, key) => store.get(key));

ipcMain.handle('login-request', async (event, credentials) => {
  if (!store) await initStore(); // Seguridad extra
  console.log(credentials);
  // 1. Call Spring Boot to get the token
  const response = await axios.post('http://localhost:8080/api/users/login', credentials);
  console.log(response);
  const token = response.data.token;
  console.log(token);
  // 2. Encrypt and Save locally
  const encryptedToken = safeStorage.encryptString(token);
  console.log(encryptedToken);
  (store as any).set('auth_token', encryptedToken.toString('latin1'));
  return { success: true };
});
/* 
ipcMain.handle('get-protected-data', async () => {
  // 3. Retrieve and Decrypt when needed for an API call
  const encrypted = Buffer.from(store.get('auth_token'), 'latin1');
  const token = safeStorage.decryptString(encrypted);

  // Use the token for your net.request/axios call...
}); */


// // Ejemplo: Enviar un mensaje cada 5 segundos
/* setInterval(() => {
  win.webContents.send('canal-notificaciones', 'Notificación desde el sistema MAIN!!');
}, 5000); */

// Ejemplo: Enviar tras completar una tarea
/* function tareaCompletada() {
  win.webContents.send('canal-notificaciones', { status: 'success', id: 123 });
} */

ipcMain.handle('check-auth', () => {
  const encrypted = (store as any).get('auth_token');
  if (!encrypted) return { isAuthenticated: false };

  try {
    const buffer = Buffer.from(encrypted, 'latin1');
    const token = safeStorage.decryptString(buffer);
    console.log("safeStorage token " + token)
    // Optional: Check if token is expired here using a JWT library
    if (!token || isTokenExpired(token)) {
      // Clean up the store if it's expired
      (store as any).delete('auth_token'); 
      return { isAuthenticated: false, reason: 'expired' };
    }
    return { isAuthenticated: !!token };
  } catch (e) {
    return { isAuthenticated: false };
  }
});


// apps/desktop-main/src/main.ts

function isTokenExpired(token: string): boolean {
  try {
    // 1. Split the token and get the payload (index 1)
    const base64Payload = token.split('.')[1];
    // 2. Decode Base64 to string, then parse to JSON
    const payload = JSON.parse(Buffer.from(base64Payload, 'base64').toString());

    // 3. 'exp' is in seconds, Date.now() is in milliseconds
    const currentTime = Math.floor(Date.now() / 1000);

    return payload.exp < currentTime;
  } catch (error) {
    // If decoding fails, treat it as expired/invalid
    return true;
  }
}

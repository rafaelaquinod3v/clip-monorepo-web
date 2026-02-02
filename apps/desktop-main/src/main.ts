import { app, BrowserWindow, ipcMain } from 'electron';
import * as path from 'path';

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
    webPreferences: {
/*       nodeIntegration: true,
      contextIsolation: false, funcionando */ 
      //preload: path.join(__dirname, 'preload.js'),
      preload: preloadPath,
      contextIsolation: true, // Requerido para contextBridge
      nodeIntegration: false  // Recomendado por seguridad
    }
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

app.whenReady().then(() => {
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
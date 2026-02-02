window.addEventListener('DOMContentLoaded', () => {
    // 1. Initial Load
    navegar('inicio');

    // 2. Navigation Setup
    document.getElementById('inicio')?.addEventListener('click', () => navegar('inicio'));
    document.getElementById('users')?.addEventListener('click', () => navegar('users'));
});

async function navegar(view) {
    const contenedor = document.getElementById('contenedor-principal');
    if (!contenedor) return;

    try {
        //const response = await fetch(`./app/views/${view}.html`);
/*         const html = await response.text();
        contenedor.innerHTML = html; */
          // Llamamos al puente seguro
        const html = await window.electronAPI.cargarVista(view);
        
        // Inyectamos el contenido
        contenedor.innerHTML = html;
        
        if(view === 'inicio') {
            // --- KEY CHANGE: Check for 'info' AFTER the HTML is injected ---
            const information = document.getElementById('info');
            if (information) {
                information.innerText = `Chrome v${versions.chrome()}, Node v${versions.node()}, Electron v${versions.electron()}`;
            }
        }

        // If you have buttons INSIDE the loaded view, attach them here:
        if (view === 'users') {
            setupUserButtons();
        }

    } catch (error) {
        console.error("Error al cargar la vista:", error);
    }
}

// Function to handle logic inside the "Users" view
function setupUserButtons() {
/*     const btn = document.getElementById('btn-save-user');
    btn?.addEventListener('click', () => {
        console.log("Saving user...");
    }); */
}






/* const information = document.getElementById('info')
if(information) information.innerText = `This app is using Chrome (v${versions.chrome()}), Node.js (v${versions.node()}), and Electron (v${versions.electron()})`

const func = async () => {
  const response = await window.versions.ping()
  console.log(response) // prints out 'pong'
}

func()

async function navegar(view) {
  const contenedor = document.getElementById('contenedor-principal');
  
  try {
    // Buscamos el archivo .html de la carpeta 'vistas'
    const response = await fetch(`./app/views/${view}.html`);
    const html = await response.text();
    
    // Inyectamos el contenido sin recargar la ventana
    contenedor.innerHTML = html;
  } catch (error) {
    console.error("Error al cargar la vista:", error);
  }
}


const inicioNav = document.getElementById('inicio')
const usersNav = document.getElementById('users')

inicioNav.addEventListener('click', () => {
    navegar('inicio')
})

usersNav.addEventListener('click', () => {
    navegar('users')
})
 */
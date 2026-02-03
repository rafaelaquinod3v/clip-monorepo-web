import { Component, inject, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ElectronService } from './services/electron-service';
import { Router } from '@angular/router';
import { User } from './User'
@Component({
  imports: [RouterModule],
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App implements OnInit {
  mensajes: string[] = [];
  users: User[] = [];
  electron = inject(ElectronService);
  router = inject(Router);
  async ngOnInit() {
    try {
      const result: any = await this.electron.invoke('check-auth');
      console.log(result);
      if(result.isAuthenticated){
        this.router.navigate(['/dashboard']);
      }else {
        this.router.navigate(['/login']);
      }
    }finally {
      // Una vez que el router terminó de navegar, revelamos la ventana
      // Usamos un pequeño timeout de 50ms para asegurar que el DOM se pintó
      setTimeout(() => {
        this.electron.send('ready-to-reveal', true);
      }, 50);
    }

/*     window.electronAPI.receiveData('canal-notificaciones', (data) => {
      console.log('Mensaje recibido en Angular:', data);
    }); */
/*     this.electron.on<string>('canal-notificaciones').subscribe((msg) => {
      this.mensajes.push(msg);
      console.log('Nuevo mensaje de Electron:', msg);
    }) */
  }
  protected title = 'desktop-ui';
  enviarMensaje() {
    console.log('msg')
    window.electronAPI.sendData('mensaje-canal', 'Hola desde Angular');
  }

  async obtenerInfo() {
    const respuesta = await window.electronAPI.invokeAction('pedir-datos', { id: 1 });
    console.log(respuesta);
  }

/*   async cargarUsuarios() {
    this.users = await this.electron.invoke<User[]>('get-users');
  } */
  // app.component.ts
  async loadUsers() {
     const myToken = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGVzIjpbIlJPTEVfQURNSU4iXSwiaWF0IjoxNzcwMDc1OTc4LCJleHAiOjE3NzAxNjIzNzh9.3uIdnZlYmzmBvMLEsPl5a_foMVDwlkU7cUpmC5nZpFk'; // Get this from your AuthService/LocalStorage
    try {
      const users = await this.electron.invoke('get-users', myToken);
      console.log('Users authorized:', users);
    } catch (error) {
      console.error('Failed to fetch users:', error);
    }
  }


/*   async solicitarConfig() {
    const config = await this.electronService.invoke('get-config');
    console.log('Configuración recibida:', config);
  } */
}

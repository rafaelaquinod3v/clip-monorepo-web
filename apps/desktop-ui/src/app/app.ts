import { Component, inject, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { NxWelcome } from './nx-welcome';
import { ElectronService } from './services/electron-service';

@Component({
  imports: [NxWelcome, RouterModule],
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App implements OnInit {
  mensajes: string[] = [];
  electron = inject(ElectronService);
  ngOnInit(): void {
/*     window.electronAPI.receiveData('canal-notificaciones', (data) => {
      console.log('Mensaje recibido en Angular:', data);
    }); */
    this.electron.on<string>('canal-notificaciones').subscribe((msg) => {
      this.mensajes.push(msg);
      console.log('Nuevo mensaje de Electron:', msg);
    })
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

/*   async solicitarConfig() {
    const config = await this.electronService.invoke('get-config');
    console.log('Configuración recibida:', config);
  } */
}

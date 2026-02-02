import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { NxWelcome } from './nx-welcome';

@Component({
  imports: [NxWelcome, RouterModule],
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected title = 'desktop-ui';
  enviarMensaje() {
    console.log('msg')
    window.electronAPI.sendData('mensaje-canal', 'Hola desde Angular');
  }

  async obtenerInfo() {
    const respuesta = await window.electronAPI.invokeAction('pedir-datos', { id: 1 });
    console.log(respuesta);
  }
}

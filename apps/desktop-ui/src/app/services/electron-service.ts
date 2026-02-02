// electron.service.ts
import { inject, Injectable, NgZone } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ElectronService {
  zone = inject(NgZone);
  //constructor(private zone: NgZone) {}

  // Escuchar eventos del Main (Main -> Renderer)
  on<T>(channel: string): Observable<T> {
    return new Observable<T>(observer => {
      window.electronAPI.receiveData(channel, (data: T) => {
        // NgZone.run asegura que Angular detecte el cambio en la UI
        this.zone.run(() => observer.next(data));
      });
    });
  }

  // Enviar y esperar respuesta (Renderer <-> Main)
  invoke<T>(channel: string, data?: any): Promise<T> {
    return window.electronAPI.invokeAction(channel, data);
  }

  // Enviar sin esperar (Renderer -> Main)
  send(channel: string, data?: any): void {
    window.electronAPI.sendData(channel, data);
  }
}

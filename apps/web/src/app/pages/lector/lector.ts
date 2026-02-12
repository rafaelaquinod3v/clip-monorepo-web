import { Component, ElementRef, inject, ViewChild } from '@angular/core';
import { SpeechService } from '../../services/speech-service';

@Component({
  selector: 'app-lector',
  imports: [],
  templateUrl: './lector.html',
  styleUrl: './lector.css',
})
export class Lector {
  
  audioService = inject(SpeechService);
  @ViewChild('audioPlayer') audioPlayer!: ElementRef<HTMLAudioElement>;
  
  palabrasLibro: string[] = [];
  indicePalabraActiva = -1;
  timestamps: any[] = []; // Guardará { word: string, start: number, end: number }

  constructor(){
    // 1. Nos suscribimos a los metadatos que vienen del stream
    this.audioService.wordMetadata$.subscribe(metadata => {
      this.timestamps.push(metadata);
    });
  }

  

  async comenzarLectura() {
    const textoCompleto = "Welcome to the jungle";
    this.palabrasLibro = textoCompleto.split(' ');
    this.timestamps = [];
    this.indicePalabraActiva = -1;

    // 2. Iniciamos el streaming (el servicio llena los timestamps y el audio)
    // Nota: Aquí el servicio debería proveer también la URL del stream al audioPlayer
    this.audioPlayer.nativeElement.src = 'http://localhost:8080/api/audio/stream-book?text=' + encodeURIComponent(textoCompleto);
    this.audioPlayer.nativeElement.play();
    
    await this.audioService.streamBookAudio(textoCompleto);
  }

  // 3. Esta función corre cada vez que el audio avanza (milisegundos)
  sincronizarTexto(event: any) {
    const currentTime = this.audioPlayer.nativeElement.currentTime;

    // Buscamos qué palabra corresponde al segundo actual
    const palabraEncontrada = this.timestamps.findIndex(t => 
      currentTime >= t.start_time && currentTime <= t.end_time
    );

    if (palabraEncontrada !== -1) {
      this.indicePalabraActiva = palabraEncontrada;
      this.hacerScrollAPalabra(palabraEncontrada);
    }
  }

  hacerScrollAPalabra(index: number) {
    const element = document.getElementById('word-' + index);
    element?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }
}

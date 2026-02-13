import { Component, ElementRef, inject, ViewChild } from '@angular/core';
import { SpeechService } from '../../services/speech-service';
import { TEST_TTS } from '../../components/reader/text.const';

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
  private mediaSource!: MediaSource;
  private sourceBuffer!: SourceBuffer;
  private chunkQueue: ArrayBuffer[] = [];
  private isFirstChunk = true;

constructor() {
  this.audioService.wordMetadata$.subscribe(m => this.timestamps.push(m));
  
  this.audioService.audioChunk$.subscribe(bytes => {
    // Usamos slice() para asegurar que pasamos una copia limpia del buffer
    this.chunkQueue.push(bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength) as ArrayBuffer);
    this.processQueue();
  });
}

processQueue() {
  if (this.sourceBuffer && !this.sourceBuffer.updating && this.chunkQueue.length > 0) {
    const chunk = this.chunkQueue.shift();
    if (chunk) {
      this.sourceBuffer.appendBuffer(chunk);
      
      // ESPERAR al primer append exitoso para dar Play
      if (this.isFirstChunk) {
        this.isFirstChunk = false;
        // Escuchamos 'updateend' una sola vez para saber que el primer chunk ya entró
        this.sourceBuffer.addEventListener('updateend', () => {
          this.audioPlayer.nativeElement.play().catch(console.error);
        }, { once: true });
      }
    }
  }
}

async comenzarLectura() {
  this.isFirstChunk = true;
  this.chunkQueue = [];
  this.timestamps = [];
  
  this.mediaSource = new MediaSource();
  this.audioPlayer.nativeElement.src = URL.createObjectURL(this.mediaSource);

  // Esperamos a que MediaSource esté listo
  await new Promise<void>((resolve) => {
    this.mediaSource.addEventListener('sourceopen', () => {
      // Intentamos con audio/mpeg (MP3)
      this.sourceBuffer = this.mediaSource.addSourceBuffer('audio/mpeg');
      this.sourceBuffer.addEventListener('updateend', () => this.processQueue());
      resolve();
    }, { once: true });
  });

  // Ahora sí pedimos el audio al backend
  const texto = TEST_TTS;
  this.palabrasLibro = texto.split(' ');
  await this.audioService.streamBookAudiov2(texto);
}

/*   constructor() {
    this.audioService.wordMetadata$.subscribe(m => this.timestamps.push(m));
    this.audioService.audioChunk$.subscribe(bytes => {
      this.chunkQueue.push(bytes.buffer as ArrayBuffer);
      this.processQueue();
    });
    this.audioService.audioChunk$.subscribe(async bytes => {
  this.chunkQueue.push(bytes.buffer as ArrayBuffer);
  this.processQueue();

  // Si es el primer fragmento y el audio está pausado, dale play
  if (this.audioPlayer.nativeElement.paused) {
    try {
      await this.audioPlayer.nativeElement.play();
    } catch (e) {
      // A veces el navegador bloquea el play automático si no hubo interacción previa
      console.warn("Esperando interacción para reproducir");
    }
  }
});

  }
  processQueue() {
    if (this.sourceBuffer && !this.sourceBuffer.updating && this.chunkQueue.length > 0) {
      const chunk = this.chunkQueue.shift();
      if (chunk) this.sourceBuffer.appendBuffer(chunk);
    }
  }
  async comenzarLectura() {
    const textoCompleto = "Welcome to the jungle";
    this.palabrasLibro = textoCompleto.split(' ');
    this.timestamps = [];
    this.chunkQueue = [];

    this.mediaSource = new MediaSource();
    const url = URL.createObjectURL(this.mediaSource);
    this.audioPlayer.nativeElement.src = url;

    
    await new Promise<void>((resolve) => {
      this.mediaSource.addEventListener('sourceopen', () => {
        
        
        if (!this.mediaSource.sourceBuffers.length) {
          this.sourceBuffer = this.mediaSource.addSourceBuffer('audio/mpeg');
          this.sourceBuffer.addEventListener('updateend', () => this.processQueue());
        }
        resolve();
      }, { once: true });
    });

    
    try {
      await this.audioService.streamBookAudio(textoCompleto);
      //await this.audioPlayer.nativeElement.play();
      
    } catch (err) {
      console.error("Playback failed", err);
    }
  } */

    
  sincronizarTexto(event: any) {
    const currentTime = this.audioPlayer.nativeElement.currentTime;

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

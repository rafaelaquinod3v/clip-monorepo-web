import { Component, ElementRef, inject, ViewChild } from '@angular/core';
import { STRESS_TTS, TEST_TTS, TEST_TTS_2, WELCOME_TO_THE_JUNGLE } from '../../components/reader/text.const';
import { SpeechService } from '../../features/audio/data-access/speech-service';
import { LibraryService } from '../../features/library/data-access/library.service';

@Component({
  selector: 'app-lector',
  imports: [],
  templateUrl: './lector.html',
  styleUrl: './lector.css',
})
export class Lector {

  selectedFile: File | null = null;

  libraryService = inject(LibraryService);

  onFileSelected(event: any) {
    const input = event.target as HTMLInputElement;
    const allowedTypes = ['.epub', '.pdf'];

    if (input.files) {
      Array.from(input.files).forEach(file => {
        const extension = '.' + file.name.split('.').pop()?.toLowerCase();
        
        if (allowedTypes.includes(extension)) {
          console.log('Archivo válido:', file.name);
          // procesar archivo...
        } else {
          console.warn('Tipo no permitido:', file.name);
        }
      });
    }
    this.selectedFile = event.target.files[0]; // Captura el primer archivo
  }

  onUpload() {
    if (this.selectedFile) {
      this.libraryService.upload(this.selectedFile).subscribe({
        next: (res) => console.log('Subida exitosa', res),
        error: (err) => console.error('Error al subir', err)
      });
    }
  }






  audioService = inject(SpeechService);
  @ViewChild('audioPlayer') audioPlayer!: ElementRef<HTMLAudioElement>;
  palabrasLibro: string[] = [];
  indicePalabraActiva = -1;
  timestamps: any[] = []; // Guardará { word: string, start: number, end: number }
  private mediaSource!: MediaSource;
  private sourceBuffer!: SourceBuffer;
  private chunkQueue: ArrayBuffer[] = [];
  private isFirstChunk = true;
  private offsetGlobal = 0;

  constructor() {
    // Escuchamos metadatos: los metadatos definen qué palabras existen
    this.audioService.wordMetadata$.subscribe(newWords => {
      // newWords es el array de objetos {word, start_time, end_time}
      newWords.forEach((m: any) => {
       // this.timestamps.push(m);
       // this.palabrasLibro.push(m.word);
      });
    });

    this.audioService.audioChunk$.subscribe(bytes => {
      this.chunkQueue.push(bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength) as ArrayBuffer);
      this.processQueue();
    });

    this.audioService.wordMetadata$.subscribe(metadata => {
      let sentenceDuration = 0;
      const adjusted = metadata.map((m: any) => {
        if (m.end_time > sentenceDuration) sentenceDuration = m.end_time;
        return { ...m, 
          start_time: m.start_time + this.offsetGlobal, 
          end_time: m.end_time + this.offsetGlobal 
        };
      });
      this.timestamps.push(...adjusted);
      this.offsetGlobal += sentenceDuration; // Prepare offset for the next sentence in the stream
    });

  }

// En lector.ts
processQueue() {
  if (this.sourceBuffer && !this.sourceBuffer.updating && this.chunkQueue.length > 0) {
    const chunk = this.chunkQueue.shift();
    if (chunk) {
      this.sourceBuffer.appendBuffer(chunk);

      // Si es el primer trozo, esperamos a que termine de insertarse para dar Play
      if (this.isFirstChunk) {
        this.isFirstChunk = false;
        const onFirstUpdate = () => {
          this.audioPlayer.nativeElement.play().catch(err => {
             console.warn("Auto-play bloqueado, el usuario debe interactuar", err);
          });
          this.sourceBuffer.removeEventListener('updateend', onFirstUpdate);
        };
        this.sourceBuffer.addEventListener('updateend', onFirstUpdate);
      }
    }
  }
}


async comenzarLectura() {
  const texto = TEST_TTS;
  // 1. Render everything IMMEDIATELY
  // We use a regex that matches words and punctuation separately to match Kokoro's behavior
  this.palabrasLibro = texto.match(/(\w+|[^\w\s])/g) || [];

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
  
  await this.audioService.streamBookAudiov2(texto);
  
}



    
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

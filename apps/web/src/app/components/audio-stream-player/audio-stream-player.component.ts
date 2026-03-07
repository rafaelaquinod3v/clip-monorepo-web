import { Component, inject, NgZone, OnDestroy, output, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { SpeechService } from '../../services/speech-service';

@Component({
  selector: 'app-audio-stream-player',
  imports: [],
  templateUrl: './audio-stream-player.component.html',
  styleUrl: './audio-stream-player.component.css',
})
export class AudioStreamPlayerComponent implements OnDestroy {
 isPlaying = signal(false);
  isLoaded = signal(false);
  timeChange = output<number>();

  private zone = inject(NgZone);
  private speechService = inject(SpeechService);

  private audio = new Audio();
  private mediaSource: MediaSource | null = null;
  private sourceBuffer: SourceBuffer | null = null;
  private currentUrl: string | null = null;
  private animationFrameId: number | null = null;
  //private chunkQueue: Uint8Array[] = [];
  private chunkQueue: Uint8Array<ArrayBuffer>[] = [];
  private streamEnded = false;
  private chunkSubscription: Subscription | null = null;

  constructor() {
    this.chunkSubscription = this.speechService.audioChunk$.subscribe(chunk => {
      this.appendChunk(chunk);
    });

    this.speechService.streamEnd$.subscribe(() => {
      this.endStream();
    });
  }

  ngOnDestroy(): void {
    this.cleanup();
    this.chunkSubscription?.unsubscribe();
  }

  // Llamar esto antes de cada nuevo stream
  initStream() {
    this.cleanup();
    this.streamEnded = false;
    this.chunkQueue = [];

    this.mediaSource = new MediaSource();
    this.currentUrl = URL.createObjectURL(this.mediaSource);
    this.audio.src = this.currentUrl;

    this.mediaSource.addEventListener('sourceopen', () => {
      this.sourceBuffer = this.mediaSource!.addSourceBuffer('audio/mpeg');

      this.sourceBuffer.addEventListener('updateend', () => {
        // Procesar cola si había chunks esperando
        if (this.chunkQueue.length > 0 && !this.sourceBuffer!.updating) {
          this.sourceBuffer!.appendBuffer(this.chunkQueue.shift()!);
        }
        // Si el stream terminó y ya no hay cola, cerrar
        if (this.streamEnded && this.chunkQueue.length === 0) {
          if (this.mediaSource?.readyState === 'open') {
            this.mediaSource.endOfStream();
          }
        }
      });

      this.isLoaded.set(true);
    });

    this.audio.addEventListener('ended', () => {
      this.zone.run(() => {
        this.stopSyncLoop();
        this.isPlaying.set(false);
        this.timeChange.emit(-1);
      });
    });

    this.audio.onerror = (e) => {
      console.error('StreamAudioPlayer error:', e);
      this.isLoaded.set(false);
      this.stopSyncLoop();
    };
  }

  // Llamar esto cuando el backend termine de enviar chunks
  endStream() {
    this.streamEnded = true;
    // Si el buffer ya no está procesando, cerramos directamente
    if (this.sourceBuffer && !this.sourceBuffer.updating && this.chunkQueue.length === 0) {
      if (this.mediaSource?.readyState === 'open') {
        this.mediaSource.endOfStream();
      }
    }
  }

  togglePlay() {
    if (!this.isLoaded()) return;

    if (this.audio.paused) {
      this.audio.play().then(() => {
        this.zone.run(() => {
          this.isPlaying.set(true);
          this.startSyncLoop();
        });
      }).catch(err => console.error('Error al reproducir:', err));
    } else {
      this.audio.pause();
      this.isPlaying.set(false);
      this.stopSyncLoop();
    }
  }

  private appendChunk(chunk: Uint8Array) {
    if (!this.sourceBuffer) return;

    // Garantizamos ArrayBuffer puro, no SharedArrayBuffer
    const buffer = chunk.buffer.slice(
        chunk.byteOffset, 
        chunk.byteOffset + chunk.byteLength
      ) as ArrayBuffer;

      const safeChunk = new Uint8Array(buffer);

      if (this.sourceBuffer.updating) {
        this.chunkQueue.push(safeChunk);
      } else {
        this.sourceBuffer.appendBuffer(safeChunk);
      }
  }

  private startSyncLoop() {
    this.stopSyncLoop();
    const update = () => {
      if (!this.audio.paused && !this.audio.ended) {
        this.zone.run(() => {
          this.timeChange.emit(this.audio.currentTime);
        });
        this.animationFrameId = requestAnimationFrame(update);
      }
    };
    this.animationFrameId = requestAnimationFrame(update);
  }

  private stopSyncLoop() {
    if (this.animationFrameId !== null) {
      cancelAnimationFrame(this.animationFrameId);
      this.animationFrameId = null;
    }
  }

  private cleanup() {
    this.stopSyncLoop();
    this.audio.pause();
    this.audio.src = '';
    this.mediaSource = null;
    this.sourceBuffer = null;
    this.isLoaded.set(false);
    this.isPlaying.set(false);
    if (this.currentUrl) {
      URL.revokeObjectURL(this.currentUrl);
      this.currentUrl = null;
    }
  }

}

import { Component, effect, input, OnDestroy, output, signal } from '@angular/core';

@Component({
  selector: 'app-audio-player',
  imports: [],
  templateUrl: './audio-player.html',
  styleUrl: './audio-player.css',
})
export class AudioPlayer implements OnDestroy {
  ngOnDestroy(): void {
    this.cleanup();
  }
  isPlaying = signal(false);
  isLoaded = signal(false);
  audioBlob = input<Blob | null>(null);
  // Emit the current second to the parent
  timeChange = output<number>();
  private audio = new Audio();
  private currentUrl: string | null = null;
  constructor() {
    // 2. This effect runs every time 'audioBlob' changes in the parent
    effect(() => {
      const blob = this.audioBlob();
      if (blob) {
        this.setupNewAudio(blob);
      }
    });
  }
/*   loadAudio(blob: Blob) {
    // 1. Limpiamos cualquier audio previo de la memoria
    this.cleanup();

    // 2. Creamos la nueva URL y cargamos el audio
    this.currentUrl = URL.createObjectURL(blob);
    this.audio.src = this.currentUrl;
    this.audio.load(); // Carga el audio sin reproducir (autoplay: off)
    
    this.isLoaded.set(true);
    this.isPlaying.set(false);

    // 3. Listener para saber cuándo termina
    this.audio.onended = () => this.isPlaying.set(false);
  } */
  private setupNewAudio(blob: Blob) {
    this.cleanup(); // Important: free previous memory

    this.currentUrl = URL.createObjectURL(blob);
    this.audio.src = this.currentUrl;
    this.audio.load();
    this.audio.oncanplaythrough = () => {
      this.isLoaded.set(true);
    };

    this.audio.ontimeupdate = () => {
      // Send the current time to the parent
      this.timeChange.emit(this.audio.currentTime);
    };
    
    /* this.isPlaying.set(false); */
    this.audio.onended = () => {
      this.isPlaying.set(false);
      this.timeChange.emit(-1); // Reset highlight when finished
    };

    /* this.audio.onended = () => this.isPlaying.set(false); */
    this.audio.onerror = (e) => {
      console.error("Error cargando el audio:", e);
      this.isLoaded.set(false);
    };
  }
 togglePlay() {
    if (!this.isLoaded()) return;

    if (this.audio.paused) {
      this.audio.play().catch(err => console.error("Error al reproducir:", err));
      this.isPlaying.set(true);
    } else {
      this.audio.pause();
      this.isPlaying.set(false);
    }
  }

  private cleanup() {
    this.audio.pause();
    this.isLoaded.set(false);
    this.isPlaying.set(false);
    if (this.currentUrl) {
      URL.revokeObjectURL(this.currentUrl);
      this.currentUrl = null;
    }
  }



}

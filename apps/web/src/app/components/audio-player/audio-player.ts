import { Component, effect, inject, input, NgZone, OnDestroy, output, signal } from '@angular/core';

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
  private zone = inject(NgZone);
  private audio = new Audio();
  private currentUrl: string | null = null;
  private animationFrameId: number | null = null;
  constructor() {
    // 2. This effect runs every time 'audioBlob' changes in the parent
    effect(() => {
      const blob = this.audioBlob();
      if (blob) {
        this.setupNewAudio(blob);
      }
    });
  }

  private setupNewAudio(blob: Blob) {
    this.cleanup(); // Important: free previous memory

    this.currentUrl = URL.createObjectURL(blob);
    this.audio.src = this.currentUrl;
    this.audio.load();
    //this.audio.playbackRate = 16000 / 48000;
    this.audio.oncanplaythrough = () => {
      this.isLoaded.set(true);
    };

/*     this.audio.ontimeupdate = () => {
      // Send the current time to the parent
      this.timeChange.emit(this.audio.currentTime);
    }; */
/*     this.audio.addEventListener('timeupdate', () => {
      this.zone.run(() => { // Force Angular to notice the change
        this.timeChange.emit(this.audio.currentTime);
      });
    }); */
    
    /* this.isPlaying.set(false); */
/*     this.audio.onended = () => {
      this.isPlaying.set(false);
      this.timeChange.emit(-1); // Reset highlight when finished
    }; */
/*     this.audio.addEventListener('ended', () => {
      this.zone.run(() => {
        this.isPlaying.set(false);
        this.timeChange.emit(-1);
      });
    }); */

    /* this.audio.onended = () => this.isPlaying.set(false); */
/*     this.audio.onerror = (e) => {
      console.error("Error cargando el audio:", e);
      this.isLoaded.set(false);
    }; */
        this.audio.addEventListener('ended', () => {
      this.zone.run(() => {
        this.stopSyncLoop(); // Stop polling when finished
        this.isPlaying.set(false);
        this.timeChange.emit(-1);
      });
    });

    this.audio.onerror = (e) => {
      console.error("Error cargando el audio:", e);
      this.isLoaded.set(false);
      this.stopSyncLoop();
    };
  }
/*  togglePlay() {
    if (!this.isLoaded()) return;

    if (this.audio.paused) {
      this.audio.play().catch(err => console.error("Error al reproducir:", err));
      this.isPlaying.set(true);
    } else {
      this.audio.pause();
      this.isPlaying.set(false);
    }
  } */

      togglePlay() {
    if (!this.isLoaded()) return;

    if (this.audio.paused) {
      this.audio.play().then(() => {
        this.zone.run(() => {
          this.isPlaying.set(true);
          this.startSyncLoop(); // 2. Start high-precision polling
        });
      }).catch(err => console.error("Error al reproducir:", err));
    } else {
      this.audio.pause();
      this.isPlaying.set(false);
      this.stopSyncLoop(); // 3. Stop polling on pause
    }
  }
  private startSyncLoop() {
    this.stopSyncLoop(); // Avoid multiple loops

    const update = () => {
      if (!this.audio.paused && !this.audio.ended) {
        // Emit the high-precision time
        // The zone.run is necessary because rAF runs outside Angular's cycle
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
/*   private cleanup() {
    this.audio.pause();
    this.isLoaded.set(false);
    this.isPlaying.set(false);
    if (this.currentUrl) {
      URL.revokeObjectURL(this.currentUrl);
      this.currentUrl = null;
    } */

  private cleanup() {
    this.stopSyncLoop();
    this.audio.pause();
    this.audio.src = ''; // Clear source to stop internal buffers
    this.isLoaded.set(false);
    this.isPlaying.set(false);
    if (this.currentUrl) {
      URL.revokeObjectURL(this.currentUrl);
      this.currentUrl = null;
    }
  }
  

/*   private cleanup() {
  if (this.audio) {
    this.audio.pause();
    this.audio.src = '';
    this.audio.load();
    //this.audio.removeEventListener('timeupdate', this.timeUpdateHandler); // If using named functions
    if (this.currentUrl) {
      URL.revokeObjectURL(this.currentUrl);
    }
  } */




}

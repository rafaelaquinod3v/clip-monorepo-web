import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Subject } from 'rxjs';

export interface WordAlignment {
  term: string;
  start: number;
  end: number;
  index: number;
}

export interface TtsResponse {
  audio: string; // Base64 string
  alignment: WordAlignment[];
}

@Injectable({
  providedIn: 'root',
})
export class SpeechService {

  private audioChunkSubject = new Subject<Uint8Array>();
  audioChunk$ = this.audioChunkSubject.asObservable();

  private streamEndSubject = new Subject<void>();
  streamEnd$ = this.streamEndSubject.asObservable();

  private totalChunksSubject = new Subject<number>();
  totalChunks$ = this.totalChunksSubject.asObservable();
  private chunkCount = 0;
  
  wordMetadata$ = new Subject<any>();

  private prefetchChunks: Uint8Array[] = [];
  private isPrefetching = false;

  private isStreaming = false;
  private timeOffset = 0;
  private readonly AUDIO_START_DELAY = 0.5;

/*   private processJsonObject(jsonString: string) {
    try {
      const data = JSON.parse(jsonString);
      
    if (data.timestamps && data.timestamps.length > 0) {
        this.wordMetadata$.next(data.timestamps);
      }
      if (data.timestamps && data.timestamps.length > 0) {
        // Aplicar offset acumulado a cada timestamp
        const adjusted = data.timestamps.map((t: any) => ({
          ...t,
          start_time: t.start_time + this.timeOffset,
          end_time: t.end_time + this.timeOffset,
        }));

        // El nuevo offset es el end_time máximo de esta frase
        const maxEnd = Math.max(...data.timestamps.map((t: any) => t.end_time));
        this.timeOffset += maxEnd;

        this.wordMetadata$.next(adjusted);
      }
      
      if (data.audio) {
        const binary = atob(data.audio);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);

        // Solo contar chunks reales, ignorar padding (1920 bytes con timestamps vacíos)
        const isPadding = bytes.length <= 1920 && (!data.timestamps || data.timestamps.length === 0);
        
        if (!isPadding) {
          this.chunkCount++;
        }

        console.log(`Chunk ${isPadding ? '(padding)' : '#' + this.chunkCount}, tamaño: ${bytes.length}`);
        this.audioChunkSubject.next(bytes); // igual lo enviamos para reproducir
      }
    } catch (e) {
      console.debug("JSON incompleto");
    }
  } */

private async processJsonObject(jsonString: string) {
  try {
    const data = JSON.parse(jsonString);

    if (data.audio) {
      const binary = atob(data.audio);
      const bytes = new Uint8Array(binary.length);
      for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);

      const isPadding = bytes.length <= 1920 && (!data.timestamps || data.timestamps.length === 0);

      if (!isPadding && data.timestamps?.length > 0) {
        // Calcular duración real del chunk
        const duration = await this.getAudioDuration(bytes);
        console.log(`maxEnd timestamps: ${Math.max(...data.timestamps.map((t: any) => t.end_time))}s, duración real: ${duration}s`);

        const adjusted = data.timestamps.map((t: any) => ({
          ...t,
          start_time: t.start_time + this.timeOffset + this.AUDIO_START_DELAY,
          end_time: t.end_time + this.timeOffset + this.AUDIO_START_DELAY,
        }));

        this.timeOffset += duration; // ← usar duración real
        this.wordMetadata$.next(adjusted);
        this.chunkCount++;
      }

      this.audioChunkSubject.next(bytes);
    }
  } catch (e) {
    console.debug("JSON incompleto");
  }
}

private getAudioDuration(bytes: Uint8Array): Promise<number> {
  return new Promise(resolve => {
    const safeBuffer = bytes.buffer.slice(
      bytes.byteOffset,
      bytes.byteOffset + bytes.byteLength
    ) as ArrayBuffer;
    
    const blob = new Blob([safeBuffer], { type: 'audio/mpeg' });
    const url = URL.createObjectURL(blob);
    const audio = new Audio(url);
    audio.addEventListener('loadedmetadata', () => {
      URL.revokeObjectURL(url);
      resolve(audio.duration);
    });
    audio.addEventListener('error', () => {
      URL.revokeObjectURL(url);
      resolve(0);
    });
  });
}

  private http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/audio';

  speak(text: string) {
    return this.http.get(`${this.apiUrl}/speak?text=${text}`, {
      responseType: 'blob'
    });
  }
  synthesize(text: string) {
    return this.http.get<TtsResponse>(`${this.apiUrl}/synthesize?text=${text}`);
  }  

    private streamAbortController: AbortController | null = null;
private prefetchAbortController: AbortController | null = null;


  // En vez de limpiar siempre, solo cancelar si aún está en progreso
  cancelAll() {
    this.streamAbortController?.abort();
    this.isStreaming = false;

    // Solo cancelar prefetch si AÚN está descargando
    // Si ya terminó, conservar los chunks
    if (this.isPrefetching) {
      this.prefetchAbortController?.abort();
      this.prefetchChunks = [];
      this.isPrefetching = false;
    }
  }

async streamBookAudiov2(text: string, voice = 'af_heart') {
  this.timeOffset = 0;
  this.streamAbortController?.abort(); // cancelar anterior
  this.streamAbortController = new AbortController();
  this.isStreaming = true;
  this.chunkCount = 0;

  try {
    const response = await fetch('http://localhost:8080/api/audio/stream-book', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text, voice }),
      signal: this.streamAbortController.signal // ← permite cancelar
    });

    const reader = response.body?.getReader();
    const decoder = new TextDecoder();
    let buffer = "";

    while (reader) {
      const { done, value } = await reader.read();
      if (done) {
        console.log('Stream terminado, total chunks:', this.chunkCount);
        this.totalChunksSubject.next(this.chunkCount);
        this.isStreaming = false;
        this.streamEndSubject.next();
        break;
      }
      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop() || "";
      for (const line of lines) {
        if (line.trim()) this.processJsonObject(line);
      }
    }
  } catch (e: any) {
    if (e.name === 'AbortError') {
      console.log('Stream cancelado');
    } else {
      console.error('Stream error:', e);
    }
    this.isStreaming = false;
  }
}

async prefetchNextPage(text: string, voice = 'af_heart') {
  if (this.isPrefetching || this.isStreaming) return;

  this.prefetchAbortController?.abort();
  this.prefetchAbortController = new AbortController();
  this.isPrefetching = true;
  this.prefetchChunks = [];

  try {
    const response = await fetch('http://localhost:8080/api/audio/stream-book', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text, voice }),
      signal: this.prefetchAbortController.signal // ← permite cancelar
    });

    const reader = response.body?.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (reader) {
        const { done, value } = await reader.read();
        if (done) {
          this.isPrefetching = false;
          break;
        }
        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';
        for (const line of lines) {
          if (line.trim()) this.processPrefetchJson(line);
        }
      }
    } catch (e: any) {
      if (e.name === 'AbortError') {
        console.log('Prefetch cancelado');
        this.prefetchChunks = [];
      } else {
        console.error('Prefetch error:', e);
      }
      this.isPrefetching = false;
    }
  }

  private processPrefetchJson(jsonString: string) {
    try {
      const data = JSON.parse(jsonString);
      if (data.audio) {
        const binary = atob(data.audio);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
        this.prefetchChunks.push(bytes);
      }
    } catch (e) {
      console.log(`${e}`)
    }
  }

  // Devuelve los chunks prefetcheados y los limpia
  consumePrefetch(): Uint8Array[] {
    const chunks = [...this.prefetchChunks];
    this.prefetchChunks = [];
    return chunks;
  }

  hasPrefetch(): boolean {
    return this.prefetchChunks.length > 0 && !this.isPrefetching;
  }
}

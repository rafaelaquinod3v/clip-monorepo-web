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
  private wordMetadataSource = new Subject<any>();
  wordMetadata$ = this.wordMetadataSource.asObservable();

  async streamBookAudio(text: string, voice = 'af_heart') {
    const response = await fetch('http://localhost:8080/api/audio/stream-book', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text, voice })
    });

    if (!response.body) throw new Error('No se pudo recibir el stream');

    const reader = response.body.getReader();
    const decoder = new TextDecoder();

    // Procesar el stream
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      console.log("Chunk recibido:", value.length, "bytes"); // Si ves esto, el stream vive
      // Aquí decodificas el chunk. 
      // Dependiendo de cómo envíe Kokoro los datos, será un JSON con audio base64 o binario.
      const chunk = decoder.decode(value, { stream: true });
      
      console.log("Contenido del chunk:", chunk);
      try {
        const data = JSON.parse(chunk);
        if (data.word) {
          // Emitimos la palabra y sus tiempos para el componente
          this.wordMetadataSource.next(data);
        }
        // Aquí manejarías la reproducción del audio (ver abajo)
      } catch (e) {
        // A veces los chunks cortan un JSON a la mitad, necesitarías un buffer de texto
      }
    }
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

  downloadAudio(text: string) {
    return this.http.get(`${this.apiUrl}/download-audio?text=${text}`, {responseType: 'blob', observe: 'response'});
  }

  generateSpeech(text: string) {
    return this.http.get(`${this.apiUrl}/generate-speech?text=${text}`, {
      responseType: 'blob'
    });
  }

  
}

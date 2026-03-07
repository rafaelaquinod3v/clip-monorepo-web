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
  
  wordMetadata$ = new Subject<any>();
  
async streamBookAudiov2(text: string, voice = 'af_heart') {
  const response = await fetch('http://localhost:8080/api/audio/stream-book', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text, voice })
  });

  const reader = response.body?.getReader();
  const decoder = new TextDecoder();
  let buffer = ""; // Aquí acumulamos los pedazos de texto

  while (reader) {
    const { done, value } = await reader.read();
    if (done) {
      this.streamEndSubject.next();
      break;
    }

    buffer += decoder.decode(value, { stream: true });

    const lines = buffer.split('\n');
    
    buffer = lines.pop() || ""; // Guardamos el fragmento incompleto

    for (const line of lines) {
      if (line.trim()) this.processJsonObject(line);
    }
  }
}

private processJsonObject(jsonString: string) {
  try {
    const data = JSON.parse(jsonString);
    
    if (data.timestamps) this.wordMetadata$.next(data.timestamps);
    console.log(data.timestamps);
    if (data.audio) {
      const binary = atob(data.audio);
      const bytes = new Uint8Array(binary.length);
      for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
      this.audioChunkSubject.next(bytes);
    }
  } catch (e) {
    // Si falla, probablemente el JSON aún está incompleto
    console.debug("Esperando más datos para completar el JSON...");
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
}

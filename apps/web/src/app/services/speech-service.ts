import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

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
}

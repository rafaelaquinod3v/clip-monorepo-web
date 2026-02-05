import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class SpeechService {
  private http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/audio/speak';

  speak(text: string) {
    return this.http.get(`${this.apiUrl}?text=${text}`, {
      responseType: 'blob'
    });
  }  
}

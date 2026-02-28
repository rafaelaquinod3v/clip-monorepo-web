import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MediaContentResponse } from '../models/media-content.model';

export interface SentenceEntry{
  index: number;
  title: string;
  text: string;
}

@Injectable({
  providedIn: 'root',
})
export class EpubService {
  private apiUrl = 'http://localhost:8080/library';

  http = inject(HttpClient);

  upload(file: File): Observable<any> {
    const formData = new FormData();
    // El nombre 'file' debe coincidir con @RequestParam("file") en tu Kotlin
    formData.append('file', file); 

    return this.http.post(`${this.apiUrl}/upload/epub`, formData, {
      responseType: 'text'
    });
  }
  
  loadEpubJsonl(fileName: string, offset: number, limit: number) {
    return this.http.get<SentenceEntry[]>(`${this.apiUrl}/${fileName}/content?offset=${offset}&limit=${limit}`);
  }

  loadMediaContent(offset: number, limit: number, sortOrder: string) {
    return this.http.get<MediaContentResponse[]>(`${this.apiUrl}/epub/media-content?offset=${offset}&limit=${limit}&sortOrder=${sortOrder}`);
  }
}

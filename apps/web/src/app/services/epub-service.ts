import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MediaContentResponse, MediaType } from '../models/media-content.model';

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

    return this.http.post(`http://localhost:8080/media-content/upload`, formData, {
      responseType: 'text'
    });
  }
  
  loadEpubJsonl(fileName: string, offset: number, limit: number) {
    return this.http.get<SentenceEntry[]>(`${this.apiUrl}/${fileName}/content?offset=${offset}&limit=${limit}`);
  }

  loadMediaContent(offset: number, limit: number, sortField: string, sortOrder: string, mediaTypes: MediaType[]) {
    const params = new HttpParams()
      .set('offset', offset)
      .set('limit', limit)
      .set('sortField', sortField)
      .set('sortOrder', sortOrder)
      .append('mediaTypes', mediaTypes.join(','))
    return this.http.get<MediaContentResponse[]>(`${this.apiUrl}/media-content/ebook`, {params});
  }
}

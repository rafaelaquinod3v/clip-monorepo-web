import { inject, Injectable } from '@angular/core';
import { API_PATHS } from '../../../core/constants/api-paths';
import { environment } from '../../../../environments/environment';
import { HttpClient, HttpParams } from '@angular/common/http';
import { MediaContentResponse, MediaType } from '../../../models/media-content.model';
import { Observable } from 'rxjs';


@Injectable({
  providedIn: 'root',
})
export class LibraryService {
  private readonly apiUrl = `${environment.apiUrl}`;
  http = inject(HttpClient);


  upload(file: File): Observable<any> {
    const formData = new FormData();
    // El nombre 'file' debe coincidir con @RequestParam("file") en tu Kotlin
    formData.append('file', file); 

    return this.http.post(`${environment.apiUrl}${API_PATHS.library.upload}`, formData, {
      responseType: 'text'
    });
  }

  loadMediaContent(offset: number, limit: number, sortField: string, sortOrder: string, mediaTypes: MediaType[]) {
    const params = new HttpParams()
      .set('offset', offset)
      .set('limit', limit)
      .set('sortField', sortField)
      .set('sortOrder', sortOrder)
      .append('mediaTypes', mediaTypes.join(','))
    return this.http.get<MediaContentResponse[]>(`${this.apiUrl}${API_PATHS.library.content}`, {params});
  }
}

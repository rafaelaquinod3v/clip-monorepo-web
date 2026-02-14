import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

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

}

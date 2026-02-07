import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { WordAnalysis } from '../components/reader/helper';

@Injectable({
  providedIn: 'root',
})
export class AnalyzeText {
  private http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8080/text-analysis';

  analyzeText(rawText: string) {
    return this.http.post(`${this.API_URL}`, rawText);
  }
  
  analyzeSingleWord(rawText: string) {
    return this.http.post<WordAnalysis>(`${this.API_URL}/analyze-single-word`, rawText);
  }
}

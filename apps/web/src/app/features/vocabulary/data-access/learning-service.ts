import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class LearningService {
  private http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/learning/user-words';

  addUserWord(userWordRequest: any) {
    return this.http.post(this.apiUrl, userWordRequest);
  }

  updateUserWordStatus(userWordRequest: any) {
    return this.http.patch(this.apiUrl, userWordRequest);
  }

  deleteUserWord(userWordRequest: any){
    return this.http.delete(this.apiUrl, userWordRequest);
  }
}

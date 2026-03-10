import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { API_PATHS } from '../constants/api-paths';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private client = inject(HttpClient);
  private router = inject(Router);
  login(credentials: any) {
    return this.client.post(`${environment.apiUrl}${API_PATHS.auth.login}`, credentials).pipe(tap((response: any) => {
      localStorage.setItem('token', response.token);
    }));
  }

  logout() {
    localStorage.removeItem('token');
    this.router.navigate(['/login']);
  }
}

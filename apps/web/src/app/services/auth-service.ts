import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { Router } from '@angular/router';

const apiUrl = 'http://localhost:8080/api/users/login';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private client = inject(HttpClient);
  private router = inject(Router);
  login(credentials: any) {
    //const request = 
   return this.client.post(apiUrl, credentials).pipe(tap((response: any) => {
      localStorage.setItem('token', response.token);
    }));
/*     request.subscribe((response) => {
      console.log(response);
    }); */
  }
  logout() {
    localStorage.removeItem('token');
    this.router.navigate(['/login']);
  }

}

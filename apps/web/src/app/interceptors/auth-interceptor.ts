import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  
  const isLoginRequest = req.url.includes('/login');  
  if (isLoginRequest) {
    return next(req);
  }
  
  const token = localStorage.getItem('token');
  const router = inject(Router);
  if(token){
    const cloned = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(cloned).pipe(
      catchError((error) => {
        if (error.status === 401 || error.status === 403) {
          localStorage.removeItem('token');
          router.navigate(['/login']);
        }
        return throwError(() => error);
      })
    );
  }
  return next(req);
};

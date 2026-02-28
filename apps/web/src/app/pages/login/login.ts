import { Component, inject } from '@angular/core';
import { AuthService } from '../../services/auth-service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  imports: [],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  auth = inject(AuthService);
  router = inject(Router);
  onLogin(event: Event){
    event.preventDefault();
    const credentials = { 
      username: (event.target as any).user.value, 
      password: (event.target as any).pass.value 
    };
    try {
      this.auth.login(credentials).subscribe({
        next: (res) => {
          this.router.navigate(['/library']);
        },
        error: (err) => {
          console.log(err);
        }
      });
    }catch(e) {
      console.log("login error");
      console.log(e)
    }    
  }
}

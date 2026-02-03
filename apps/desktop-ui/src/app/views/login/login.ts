import { Component, inject } from '@angular/core';
import { ElectronService } from '../../services/electron-service';

@Component({
  selector: 'app-login',
  imports: [],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  // login.component.ts
electron = inject(ElectronService);
async onLogin(event: Event) {
  event.preventDefault();
  const credentials = { 
    username: (event.target as any).user.value, 
    password: (event.target as any).pass.value 
  };

  try {
    // We send data to Electron Main to perform the fetch & save the token
    const result = await this.electron.invoke<{success: boolean}>('login-request', credentials);
    console.log(result)
/*     if (result.success) {
      console.log('Token obtained and saved safely in Electron!');
      // Navigate to your main view
    } */
  } catch (err) {
    console.error('Login failed', err);
  }
}

}

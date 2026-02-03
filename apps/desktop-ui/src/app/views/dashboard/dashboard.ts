import { Component, inject, OnInit } from '@angular/core';
import { ElectronService } from '../../services/electron-service';

@Component({
  selector: 'app-dashboard',
  imports: [],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard { //implements OnInit {
  electron = inject(ElectronService);
/*   
  ngOnInit(): void {
    this.electron.invoke('set-fullscreen', true);
  } */
  logout() {
    this.electron.logout();
  }

}

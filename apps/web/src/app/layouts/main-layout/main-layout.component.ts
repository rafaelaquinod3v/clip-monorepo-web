import { Component } from '@angular/core';
import { TopAppBarComponent } from '../../components/top-app-bar/top-app-bar.component';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, TopAppBarComponent],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.css',
})
export class MainLayoutComponent {}

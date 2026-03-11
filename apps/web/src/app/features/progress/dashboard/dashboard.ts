import { Component, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpEventType, HttpResponse } from '@angular/common/http';
/* import { ProgressOmwService } from '../../services/progress-omw-service';
import { FileOmwService } from '../../services/file-omw-service'; */
import { RouterModule } from '@angular/router';
import { FileOmwService } from '../../../services/file-omw-service';
import { AuthService } from '../../../core/auth/auth-service';
import { ProgressOmwService } from '../../../services/progress-omw-service';
//import { AuthService } from '../../core/auth/auth-service';
@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  selectedLang = 'EN'; 
  selectedFile: File | null = null;
  progress = 0;
  mensaje = '';
  progressData: any = null;
  fileService = inject(FileOmwService);
  cd = inject(ChangeDetectorRef);
  auth = inject(AuthService);
  progressOmwService = inject(ProgressOmwService);
  
  onLogout(){
    this.auth.logout();
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      this.progress = 0; // Reinicia para la nueva selección
      this.mensaje = ''; 
    }
  }

  onUpload() {
    if (!this.selectedFile) {
      this.mensaje = 'Por favor, selecciona un archivo primero.';
      return;
    }
    this.progress = 1; // Inicia en 1 para que el *ngIf muestre la barra inmediatamente
    this.mensaje = 'Subiendo...';

    if (this.selectedFile) {
      this.fileService.upload(this.selectedFile, this.selectedLang).subscribe({
        next: (event: any) => {
          if (event.type === HttpEventType.UploadProgress) {
            // Calcular el porcentaje
            if (event.total) {
              this.progress = Math.round((100 * event.loaded) / event.total);
            }
            this.cd.detectChanges(); // FUERZA a Angular a pintar el progreso en el HTML
          }
          else if (event instanceof HttpResponse || event.type === HttpEventType.Response) {
            this.progress = 100; // Forzamos el 100% al recibir respuesta
            this.mensaje = 'Archivo subido. Procesado datos...';
            this.selectedFile = null; // Limpiamos la selección
            //
            this.startSocketTracking();
          }
        },
        error: (err) => {
          this.mensaje = 'Error al subir el archivo';
          this.progress = 0;
        }
      });

    }
  }

  private startSocketTracking() {
    this.progressOmwService.connect();
    this.progressOmwService.progress$.subscribe((data : any) => {
      this.progressData = {
        ...data,
        percentage: Number(data.percentage) || 0,
        current: Number(data.current) || 0,
        total: Number(data.total) || 100
      };
      console.log(`Procesado ${data.current} de ${data.total}`);
      if (data.status === 'completed') this.progressOmwService.disconnect();
      this.cd.detectChanges();
    });
  }
}

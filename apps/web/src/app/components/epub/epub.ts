import { Component, ElementRef, HostListener, inject, signal, ViewChild } from '@angular/core';
import { EpubService, SentenceEntry } from '../../services/epub-service';
import { debounceTime, Subject } from 'rxjs';

@Component({
  selector: 'app-epub',
  imports: [],
  templateUrl: './epub.html',
  styleUrl: './epub.css',
})
export class Epub {
  epubService = inject(EpubService);
  readonly epubName = "645392f4-421d-4177-b9db-2fa724b0d69e_pg64317-images.jsonl";
  private offset = 4;
  private proximoOffset = 4;
  private limit = 10;
  //content = signal("");
  // Índice de la primera frase que se ve actualmente en pantalla
  currentPageStart = signal<number>(0); 

  // Todas las frases cargadas del servicio
  allPhrases: SentenceEntry[] = []; 

  // El contenido que se muestra (un subconjunto de allPhrases)
  content = signal<string>("");
  // Creamos un disparador para el redimensionamiento
  private resizeSubject = new Subject<void>();
  constructor() {
    // Escuchamos el evento y esperamos 200ms después del último movimiento
    this.resizeSubject.pipe(debounceTime(200)).subscribe(() => {
      this.recalculatePage();
    });
  }
  // Este decorador detecta cambios de tamaño y rotación de móvil
  @HostListener('window:resize', ['$event'])
  onResize(event: any) {
    this.resizeSubject.next();
  }
  @HostListener('window:orientationchange', ['$event'])
  onOrientationChange(event: any) {
    this.resizeSubject.next();
  }
  recalculatePage() {
    console.log('Recalculando página por cambio de pantalla...');
    // Aquí vuelves a llamar a tu lógica de checkFit 
    // usando las frases que ya tienes cargadas
    this.renderCurrentPage(); 
  }
  renderCurrentPage() {
    if (this.allPhrases.length === 0) return;

    // 1. Obtenemos las frases desde donde nos quedamos
    const phrasesFromCurrent = this.allPhrases.slice(this.currentPageStart());

    // 2. Calculamos cuántas caben en el espacio actual
    const count = this.checkFit(phrasesFromCurrent);

    // 3. Tomamos solo esas frases y las unimos
    const visibleText = phrasesFromCurrent
      .slice(0, count)
      .map(p => p.text)
      .join(' ');

    // 4. Actualizamos la señal visual
    this.content.set(visibleText);
  }

  @ViewChild('ghost') ghostElement!: ElementRef<HTMLElement>;

  loadEpub() {
    this.epubService.loadEpubJsonl(this.epubName, this.offset, this.limit).subscribe((response: SentenceEntry[]) => {
      console.log(response);
/*       const cuantasCaben = this.checkFit(response);
      
      // Solo seteamos las que caben visualmente
      const textoVisible = response.slice(0, cuantasCaben).map(e => e.text).join('');
      this.content.set(textoVisible); */

      this.allPhrases = response; // Guardamos todo el "pool" de frases
      this.currentPageStart.set(0); // Empezamos por el principio
      this.renderCurrentPage();

      // El resto lo guardas para el offset de la siguiente página
      //this.proximoOffset += cuantasCaben;
    });
  }


// Usa setTimeout(0) para forzar al navegador a procesar el layout antes de medir
checkFit(frases: SentenceEntry[]): number {
  const ghost = this.ghostElement.nativeElement;
  ghost.innerHTML = ''; 
  let count = 0;

  for (let i = 0; i < frases.length; i++) {
    const previousHTML = ghost.innerHTML;
    ghost.innerHTML += frases[i].text + " "; // Añadimos la frase actual

    // IMPORTANTE: Si el contenido (scrollHeight) es estrictamente MAYOR 
    // al contenedor (clientHeight), la frase actual (i) es la que causó el desborde.
    if (ghost.scrollHeight > ghost.clientHeight) {
      console.log(`¡Desborde en frase ${i}! Solo caben ${i} frases.`);
      ghost.innerHTML = previousHTML; // Opcional: revertir para dejar el ghost exacto
      return i; // Retornamos la cantidad de frases que SÍ cupieron (0 hasta i-1)
    }
    
    count++;
  }
  return count; // Si recorre todo y no desborda, caben todas
}

}

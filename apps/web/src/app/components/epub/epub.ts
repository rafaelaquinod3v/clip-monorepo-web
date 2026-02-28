import { Component, ElementRef, HostListener, inject, OnInit, signal, ViewChild } from '@angular/core';
import { EpubService, SentenceEntry } from '../../services/epub-service';
import { debounceTime, Subject } from 'rxjs';
import { ActivatedRoute } from '@angular/router';

interface BookState {
  currentPageStart: number;
  pageHistory: number[];
  offset: number;
}

@Component({
  selector: 'app-epub',
  imports: [],
  templateUrl: './epub.html',
  styleUrl: './epub.css',
})
export class Epub implements OnInit {
  epubService = inject(EpubService);
  private route = inject(ActivatedRoute);
  readonly epubName = "ae116030-fa4c-4c91-8f15-7cc37598e382";
  fileName: string | null = '';
  private offset = 0;
  private readonly LIMIT = 100; // Cuántas frases pedimos por vez
  private readonly UMBRAL_PRECARGA = 20; // Si quedan menos de 20 frases, cargamos más
  private isLoading = false; // Evita peticiones duplicadas

  
  
  // Pila de índices donde comenzó cada página visitada
  pageHistory: number[] = [];
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
  
  ngOnInit(): void {
    this.fileName = this.route.snapshot.paramMap.get('fileName');
    this.loadEpub();
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
    // 1. Intentar recuperar estado previo
    const saved = localStorage.getItem(`book_progress_${this.fileName}`);
    
    if (saved) {
      const state: BookState = JSON.parse(saved);
      this.currentPageStart.set(state.currentPageStart);
      this.pageHistory = state.pageHistory;
      this.offset = state.offset;
    } else {
      // Estado inicial si es la primera vez
      this.offset = 0;
      this.currentPageStart.set(0);
      this.pageHistory = [];
    }

    // 2. Cargar las frases desde el offset guardado
    // Nota: Para que el historial funcione, necesitamos cargar desde el inicio 
    // hasta el offset actual, o al menos el bloque actual.
    this.fetchPhrases();


/*     this.epubService.loadEpubJsonl(this.epubName, this.offset, this.LIMIT).subscribe((response: SentenceEntry[]) => {
      console.log(response);

      this.allPhrases = response; // Guardamos todo el "pool" de frases
      this.currentPageStart.set(0); // Empezamos por el principio
      this.renderCurrentPage();

    }); */
  }
  prev() {
    console.log("prev");
    if (this.pageHistory.length > 0) {
      // Recuperamos el último índice guardado
      const lastIndex = this.pageHistory.pop();
      
      if (lastIndex !== undefined) {
        this.currentPageStart.set(lastIndex);
        this.renderCurrentPage();
      }
    }
    this.saveProgress();
  }
  next() {
    console.log("next");
    const phrasesFromCurrent = this.allPhrases.slice(this.currentPageStart());
    const count = this.checkFit(phrasesFromCurrent);

    // El nuevo inicio es el inicio anterior + las que acabamos de leer
    const nextIndex = this.currentPageStart() + count;

    if (nextIndex < this.allPhrases.length) {
      // GUARDAR HISTORIAL: El índice actual es el inicio de la página que estamos dejando
      this.pageHistory.push(this.currentPageStart());
      this.currentPageStart.set(nextIndex);
      this.renderCurrentPage();
      // VERIFICAR PRECARGA
      const frasesRestantes = this.allPhrases.length - nextIndex;
      if (frasesRestantes < this.UMBRAL_PRECARGA && !this.isLoading) {
        this.preloadNextPhrases();
      }
    } else {
      // Si llegamos al final absoluto y no hay más cargadas
      if (!this.isLoading) this.preloadNextPhrases();
      console.log("Fin del contendido cargado");
    }
    this.saveProgress();
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

  private preloadNextPhrases() {
    this.isLoading = true;
    this.offset += this.LIMIT; // Aumentamos el offset para la siguiente tanda

    this.epubService.loadEpubJsonl(this.fileName? this.fileName : this.epubName, this.offset, this.LIMIT)
      .subscribe({
        next: (newPhrases: SentenceEntry[]) => {
          // Unimos las frases nuevas a la lista maestra
          this.allPhrases = [...this.allPhrases, ...newPhrases];
          this.isLoading = false;
          console.log(`Precargadas ${newPhrases.length} frases nuevas. Total: ${this.allPhrases.length}`);
        },
        error: (err) => {
          this.isLoading = false;
          console.error("Error en precarga", err);
        }
      });
  }
  private saveProgress() {
    const state: BookState = {
      currentPageStart: this.currentPageStart(),
      pageHistory: this.pageHistory,
      offset: this.offset
    };
    // Guardamos usando el nombre del libro como llave única
    localStorage.setItem(`book_progress_${this.fileName}`, JSON.stringify(state));
  }

  private fetchPhrases() {
    this.isLoading = true;
    // Cargamos desde 0 hasta el offset actual + LIMIT para tener todo el historial disponible
    const totalToFetch = this.offset + this.LIMIT;
    
    this.epubService.loadEpubJsonl(this.fileName? this.fileName : this.epubName, 0, totalToFetch)
      .subscribe((response) => {
        this.allPhrases = response;
        this.renderCurrentPage();
        this.isLoading = false;
      });
  }
}

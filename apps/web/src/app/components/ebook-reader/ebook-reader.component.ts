import { ChangeDetectorRef, Component, ElementRef, HostListener, inject, OnInit, signal, ViewChild, ViewEncapsulation } from '@angular/core';
import { EpubService, SentenceEntry } from '../../services/epub-service';
import { debounceTime, Subject } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { Pagination } from './pagination';
import { WordTimestamp } from '../../models/ebook.model';
import { SpeechService } from '../../services/speech-service';
import { AudioStreamPlayerComponent } from '../audio-stream-player/audio-stream-player.component';

interface BookState {
  currentPageStart: number;
  pageHistory: number[];
  offset: number;
}

@Component({
  selector: 'app-ebook-reader',
  imports: [AudioStreamPlayerComponent],
  templateUrl: './ebook-reader.component.html',
  styleUrl: './ebook-reader.component.css',
  encapsulation: ViewEncapsulation.None,
})
export class EbookReaderComponent implements OnInit {
  private cdr = inject(ChangeDetectorRef);
  epubService = inject(EpubService);
  speechService = inject(SpeechService);
  private route = inject(ActivatedRoute);
  readonly epubName = "ae116030-fa4c-4c91-8f15-7cc37598e382";
  fileName: string | null = '';
  id: string | null = '';
  private offset = 0;
  private readonly LIMIT = 100; // Cuántas frases pedimos por vez
  private readonly UMBRAL_PRECARGA = 20; // Si quedan menos de 20 frases, cargamos más
  private isLoading = false; // Evita peticiones duplicadas  
  private wordTimestamps: WordTimestamp[] = [];
  private lastRenderedCount = 0;

  // Pila de índices donde comenzó cada página visitada
  pageHistory: number[] = [];
  // Índice de la primera frase que se ve actualmente en pantalla
  currentPageStart = signal<number>(0); 

  // Todas las frases cargadas del servicio
  allPhrases: SentenceEntry[] = []; 

  // El contenido que se muestra (un subconjunto de allPhrases)
  content = signal<string>("");

  isFirstPage = signal(true);

  private audioDebounceTimer: ReturnType<typeof setTimeout> | null = null;
  private readonly AUDIO_DEBOUNCE_MS = 800; // espera 800ms antes de pedir audio

  // Creamos un disparador para el redimensionamiento
  private resizeSubject = new Subject<void>();
  constructor() {
    // Escuchamos el evento y esperamos 200ms después del último movimiento
    this.resizeSubject.pipe(debounceTime(200)).subscribe(() => {
      this.recalculatePage();
    });
  }
  
  ngOnInit(): void {
    this.id = this.route.snapshot.paramMap.get('id');
    this.epubService.findEbookMediaContentById(this.id).subscribe(response => {
      this.fileName = response.fileName; 
      console.log(response);
      this.loadEpub();
    });
    this.speechService.wordMetadata$.subscribe((timestamps: WordTimestamp[]) => {
      this.wordTimestamps = [...this.wordTimestamps, ...timestamps];
      console.log('Timestamps recibidos:', timestamps[0]);
      // Añadir data-start y data-end a los spans ya renderizados
     // this.cdr.detectChanges(); 
      this.applyTimestampsToSpans();
      //setTimeout(() => this.applyTimestampsToSpans(), 0);
    });
    this.speechService.streamEnd$.subscribe(() => {
      console.log('streamEnd$ recibido, iniciando prefetch...');
      this.prefetchNextPage(this.lastRenderedCount);
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

  @ViewChild(AudioStreamPlayerComponent) streamPlayer!: AudioStreamPlayerComponent;

  renderCurrentPage() {
    if (this.allPhrases.length === 0) return;

    const phrasesFromCurrent = this.allPhrases
      .slice(this.currentPageStart())
      .map(s => s.text);

    const { html, plainText, count } = Pagination.generatePageContent(
      phrasesFromCurrent,
      this.ghostElement.nativeElement
    );

    this.lastRenderedCount = count;
    //this.content.set(html);
    this.mainReaderElement.nativeElement.innerHTML = html;
    this.wordTimestamps = [];

    
    // Cancelar todo lo que esté en curso y limpiar timer
    
    if (this.audioDebounceTimer) {
      clearTimeout(this.audioDebounceTimer);
      this.audioDebounceTimer = null;
      this.speechService.cancelAll(); // ← aborta fetch y prefetch      
    }

    this.streamPlayer.stopStream();
    this.streamPlayer.initStream(this.isFirstPage());

    this.audioDebounceTimer = setTimeout(() => {
      this.audioDebounceTimer = null;
      if (this.speechService.hasPrefetch()) {
        console.log('Usando prefetch disponible');
        const chunks = this.speechService.consumePrefetch();
        this.streamPlayer.feedPrefetchedChunks(chunks);
        // como ya tenemos todos los chunks, cerramos el stream
        this.streamPlayer.endStream();
      } else {
        this.speechService.streamBookAudiov2(plainText);
      }
    }, this.AUDIO_DEBOUNCE_MS);

  }

  @ViewChild('mainReader') mainReaderElement!: ElementRef<HTMLElement>;

  // Aplicar timestamps a los spans del reader
  private applyTimestampsToSpans() {
    const spans = this.mainReaderElement.nativeElement
      .querySelectorAll<HTMLElement>('span');

    spans.forEach((span, index) => {
      const ts = this.wordTimestamps[index];
      if (ts) {
        span.dataset['start'] = ts.start_time.toString();
        span.dataset['end'] = ts.end_time.toString();
      }
    });
    // Ver los primeros 3 spans con sus timestamps
    const allSpans = this.mainReaderElement.nativeElement.querySelectorAll<HTMLElement>('span');
    allSpans.forEach((span, i) => {
      if (i < 3) console.log(`span[${i}]: "${span.textContent}" start=${span.dataset['start']} end=${span.dataset['end']}`);
    });
  }

  onTimeChange(currentTime: number) {
    if (currentTime === -1) {
      this.clearHighlight();
      return;
    }

    const spans = this.mainReaderElement.nativeElement
      .querySelectorAll<HTMLElement>('span[data-start]');

/*     console.log(`currentTime: ${currentTime}, spans con timestamp: ${spans.length}`);

    spans.forEach(span => {
      if(span.dataset['start'] && span.dataset['end']) {
        const start = parseFloat(span.dataset['start']);
        const end = parseFloat(span.dataset['end']);
        span.classList.toggle('highlight', currentTime >= start && currentTime < end);
      }
      
    }); */
    // Ver si algún span hace match
    //let matched = false;
    spans.forEach(span => {
      if(span.dataset['start'] && span.dataset['end']) {
        const start = parseFloat(span.dataset['start']);
        const end = parseFloat(span.dataset['end']);
        const isActive = currentTime >= start && currentTime < end;
       // if (isActive) {
         // matched = true;
          //console.log(`MATCH: "${span.textContent}" start=${start} end=${end} currentTime=${currentTime}`);
        //}
        span.classList.toggle('highlight', isActive);
      }
    });
  }

  private clearHighlight() {
    this.mainReaderElement.nativeElement
      .querySelectorAll<HTMLElement>('.highlight')
      .forEach(s => s.classList.remove('highlight'));
  }

  private prefetchNextPage(currentCount: number) {
    const nextIndex = this.currentPageStart() + currentCount;
    if (nextIndex >= this.allPhrases.length) return;

    const nextPhrases = this.allPhrases
      .slice(nextIndex)
      .map(s => s.text);

    // Calculamos cuántas frases caben en la siguiente página usando el ghost
    const { plainText } = Pagination.generatePageContent(
      nextPhrases,
      this.ghostElement.nativeElement
    );

    console.log('Prefetching siguiente página...');
    this.speechService.prefetchNextPage(plainText);
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
  }
  prev() {
    this.isFirstPage.set(false);
    this.streamPlayer.endStream();
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
    this.isFirstPage.set(false);
    this.streamPlayer.endStream();
    console.log("next");
    const phrasesFromCurrent = this.allPhrases.slice(this.currentPageStart());
    //const count = this.checkFit(phrasesFromCurrent);
    const count = Pagination.checkFit(phrasesFromCurrent.map(s => s.text), this.ghostElement.nativeElement);

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

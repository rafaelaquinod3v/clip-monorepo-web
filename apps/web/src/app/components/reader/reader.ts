import { Component, computed, inject, OnInit, Signal, signal } from '@angular/core';
import { form, FormField, required, minLength } from '@angular/forms/signals';
import { TEST_TTS } from './text.const';
import { AnalyzeText } from '../../services/analyze-text';
import { LearningService } from '../../services/learning-service';
import { SpeechService, TtsResponse, WordAlignment } from '../../services/speech-service';
import { AudioPlayer } from '../audio-player/audio-player';
import { base64ToWav } from './audio';

// Definimos una interfaz para tener autocompletado y evitar errores
interface WordAnalysis {
  term: string;
  lemma: string;
  status: string;
  targetLemma?: string;
}

@Component({
  selector: 'app-reader',
  imports: [FormField, AudioPlayer],
  templateUrl: './reader.html',
  styleUrl: './reader.css',
})
export class Reader implements OnInit {
  ngOnInit(): void {
    //this.fetchAudio();
    this.speech.synthesize(this.textModel().text).subscribe((res: any) => {
      const data: TtsResponse = res;
      this.syncData.set(data.alignment);
      this.audioBlob.set(base64ToWav(data.audio));
    });
  }
  analyze = inject(AnalyzeText);
  learning = inject(LearningService);
  speech = inject(SpeechService);
  // 1. Crea el modelo de datos (Signal)
  textModel = signal({ text: TEST_TTS });
  audioBlob = signal<Blob | null>(null);
  syncData = signal<WordAlignment[]>([]);
  syncDataPlus: Signal<WordAlignment[]> = computed(() => this.interleave(this.syncData(), {
    term: " ",
    start: 0,
    end: 0,
    originalIndex: -1,
    newIndex: -1,
  }));
  activeIndex = signal<number>(-1);
    // 2. Inicializa el formulario con validaciones
  textForm = form(this.textModel, (s) => {
    required(s.text);
    minLength(s.text, 10);
  });

interleave(arr: any[], separatorBase: any): any[] {
  // 1. Verificación de seguridad
  if (!arr || arr.length === 0) return [];

  return arr.flatMap((item, i) => {
    const isLast = i === arr.length - 1;
    const wordIdx = i * 2;
    const sepIdx = i * 2 + 1;

    // La palabra original con su nuevo índice
    const word = { ...item, newIndex: wordIdx };

    // Si es el último, no añadimos el espacio para no descuadrar el final
    if (isLast) return [word];

    // EL ESPACIO: Para evitar 'undefined', el espacio debe 'llenar' 
    // el hueco entre el fin de esta palabra y el inicio de la siguiente.
    const space = { 
      ...separatorBase, 
      term: ' ', 
      start: item.end,        // Empieza justo cuando termina la palabra actual
      end: arr[i + 1].start,  // Termina justo cuando empieza la siguiente
      newIndex: sepIdx 
    };

    return [word, space];
  });
  // Eliminamos el .slice(0, -1) porque ya controlamos el último elemento con el 'if'
}




  handleTimeUpdate(currentTime: number) {
  // 1. Reset si el audio termina
  if (currentTime === -1) {
    this.activeIndex.set(-1);
    return;
  }

 // const data = this.syncData();
 // const dataPlus = this.syncDataPlus();
  
  // 2. Encontrar la palabra que suena en este segundo
  const currentEntry = this.syncDataPlus().find(w => currentTime > w.start && currentTime < w.end);
  console.log("evento audio");
  console.log(currentEntry);
console.log(this.syncDataPlus());
  if (currentEntry) {
    this.activeIndex.set(currentEntry.newIndex);
    console.log(currentEntry);
    const allItems = this.words();
    console.log(allItems);
   // console.log(data);
    console.log(this.syncDataPlus());
 }
}

  fetchAudio() {
    if (this.textForm.text().valid()) {
      this.speech.speak(this.textModel().text).subscribe(blob => {
        this.audioBlob.set(blob);
      });
    }
  }



  sendToApi() {
    if (this.textForm.text().valid()) {
      console.log('Enviando:', this.textModel().text);
      // Tu lógica de backend aquí
      this.analyze.analyzeText(this.textModel().text).subscribe(
        { next: (response: any) => {
            console.log(response);
            this.rawResponse.set(response.rawText);
            this.analysisData.set(response.words);
          },
          error: (err) => console.log('Error al procesar el text ', err)
        }
      );
    }
  }
  // ---- Interaction ----
    // Aquí guardas la respuesta "rawtext" del backend
  rawResponse = signal<string>("");
  
  // Signal para saber qué palabra ha tocado el usuario
  selectedWord = signal<string | null>(null);

  // Dividimos el texto en palabras individuales automáticamente
  words = computed(() => {
    const text = this.rawResponse();
    if (!text) return [];
      // Separamos por espacios y saltos de línea
      return text.split(/(\s+)/); 
    });

  selectedWordInfo = computed(() => {
    const word = this.selectedWord();
    const data = this.analysisData();
    
    if (!word || !data) return null;

    // Buscamos la palabra en el análisis (ignorando mayúsculas/minúsculas)
    return data.find(item => 
      item.term.toLowerCase() === word.toLowerCase()
    );
  });

  handleWordClick(word: string) {
    // Limpiamos puntuación básica para el análisis
    const cleanWord = word.trim().replace(/[.,!?;:]/g, '');
    if (cleanWord) {
      this.speech.speak(cleanWord).subscribe({
      next: (blob: Blob) => {
        console.log('Audio blob received:', blob);
        
        // Create a URL for the blob
        const url = window.URL.createObjectURL(blob);
        
        // Play it immediately
        const audio = new Audio();
        audio.src = url;
        // Release the URL as soon as the browser has finished loading the data
        audio.oncanplaythrough = () => {
            window.URL.revokeObjectURL(url);
        };
        audio.play();
      },
        error: (err) => console.log('Error obteniendo audio ', err)
      });
      this.selectedWord.set(cleanWord);
      console.log('Palabra interactiva:', cleanWord);
      const currentInfo = this.selectedWordInfo();
      if(currentInfo?.status === 'UNKNOWN'){
        this.fetchDeepAnalysis(cleanWord);
      }

    }
  }
  private fetchDeepAnalysis(word: string) {
    this.analyze.analyzeSingleWord(word).subscribe({
    next: (detailedInfo: any) => {
        console.log("Deep analysis");
        console.log(detailedInfo);
        this.updateSingleWordAnalysis(detailedInfo);
      },
      error: (err) => console.log('Error analizando palabra ', err)
    });
  }
  analysisData = signal<WordAnalysis[]>([]);  

  updateSingleWordAnalysis(newData: WordAnalysis) {
    this.analysisData.update(currentList => {
      // We map through the list: 
      // If the word matches, we swap it for newData. 
      // Otherwise, we keep the old item.
      return currentList.map(item => 
        item.term.toLowerCase() === newData.term.toLowerCase() 
          ? newData 
          : item
      );
    });
  }

  handleKeyDown(event: KeyboardEvent, word: string) {
    
    // 1. Detectar si Shift está presionado
    const isShift = event.shiftKey;

    // 2. Detectar si la tecla física es un número (Digit0 al Digit9)
    const isDigit = event.code.startsWith('Digit');
    const isNumpad = event.code.startsWith('Numpad');

    if (isShift && (isDigit || isNumpad)) {
      console.log('Evento de teclado!!');
      event.preventDefault(); // Evita que se escriba el símbolo en otros campos
      
      // Extraemos el número final del string "Digit1", "Digit2", etc.
      const numberPressed = event.code.replace('Digit', '').replace('Numpad', '');
      
      console.log(`Combinación detectada: Shift + ${numberPressed}`);
      // Validate it's a single digit (0-9)
      if (/^[0-9]$/.test(numberPressed)) {
        this.processStatusUpdate(word, numberPressed);
      }
    }
  }

  private updateOptimisticLocalUserWordStatus(currentList: WordAnalysis[], term: string, newStatus: string): WordAnalysis[] {
    return currentList.map(item => 
      item.term.toLowerCase() === term.toLowerCase()
      ? {...item, status: newStatus} : item
    );
  }

  private processStatusUpdate(word: string, key: string) {
    const currentInfo = this.selectedWordInfo();
    
    // Mapeamos números a tus estados del backend
    const statusMap: Record<string, string> = {
      '1': 'NEW',
      '2': 'RECOGNIZED',
      '3': 'FAMILIAR',
      '4': 'LEARNED',
      '5': 'KNOW'
    };

    const newStatus = statusMap[key];
    if (!newStatus) return;

    console.log("new Status: " + newStatus);
    if (currentInfo?.status === 'UNKNOWN') {
      // Si es desconocida, llamamos al endpoint de "crear"
      this.learning.addUserWord({term: word, statusCode: key}).subscribe({
    next: () => {
      // 2. Since response is empty, we update the Signal manually
      this.analysisData.update(currentList => 
        currentList.map(item => 
          item.term.toLowerCase() === word.toLowerCase() 
            ? { ...item, status: newStatus } // Spread old properties, overwrite status
            : item
        )
      );
      
      console.log(`Local sync: ${word} is now ${newStatus}`);
    },
    error: (err) => {
      console.error('Failed to update status on backend', err);
      // Optional: Show a "toast" notification to the user
    }
  });
      
    } else {
      // Si ya existe, llamamos al endpoint de "actualizar status"
      this.learning.updateUserWordStatus({term: word, statusCode: key}).subscribe({
    next: () => {
      // 2. Since response is empty, we update the Signal manually
      this.analysisData.update(currentList => 
        currentList.map(item => 
          item.term.toLowerCase() === word.toLowerCase() 
            ? { ...item, status: newStatus } // Spread old properties, overwrite status
            : item
        )
      );
      
      console.log(`Local sync: ${word} is now ${newStatus}`);
    },
    error: (err) => {
      console.error('Failed to update status on backend', err);
      // Optional: Show a "toast" notification to the user
    }
  });
      
    }
  }


}



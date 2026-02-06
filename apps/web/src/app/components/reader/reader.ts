import { Component, computed, inject, OnInit, Signal, signal } from '@angular/core';
import { form, FormField, required, minLength } from '@angular/forms/signals';
import { TEST_TTS } from './text.const';
import { AnalyzeText } from '../../services/analyze-text';
import { LearningService } from '../../services/learning-service';
import { SpeechService, TtsResponse, WordAlignment } from '../../services/speech-service';
import { AudioPlayer } from '../audio-player/audio-player';

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
      this.audioBlob.set(this.base64ToWav(data.audio));
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
  
  // Definición de la función interleave
/*   interleave<T>(arr: T[], x: T): T[] {
    return arr.flatMap(e => [e, x]).slice(0, -1);
  } */

/*     interleave<T extends object>(arr: T[], separator: T): T[] {
  return arr
    .flatMap((item, i) => {
      // Calculamos las posiciones en el nuevo array
      const itemIndex = i * 2;
      const separatorIndex = i * 2 + 1;

      // Retornamos el objeto original con su nuevo índice 
      // y el separador con su índice (si no es el último)
      return [
        { ...item, originalIndex: itemIndex },
        { ...separator, originalIndex: separatorIndex }
      ];
    })
    .slice(0, -1); // Eliminamos el último separador
} */
/* interleave(arr: any[], separatorBase: any) {
  return arr.flatMap((item, i) => {
    const isLast = i === arr.length - 1;
    
    // 1. Palabra original con su nuevo índice
    const word = { ...item, newIndex: i * 2 };

    if (isLast) return [word];

    // 2. El espacio hereda los tiempos de la palabra actual 
    // para que el resaltado no se apague entre palabras.
    const space = { 
      ...separatorBase, 
      start: item.start, 
      end: item.end, 
      newIndex: i * 2 + 1 
    };

    return [word, space];
  });
} */

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



  private base64ToWav(audio: string) : Blob {
    const byteCharacters = atob(audio);
    const byteNumbers = new Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
    byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);
    return new Blob([byteArray], { type: 'audio/wav' });
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
/*     */
/*      // El array que tiene ['Hello', ' ', 'there!', ...]
    let wordCounter = 0;

    // 3. Mapear originalIndex (backend) -> index visual (frontend)
    for (let i = 0; i < allItems.length; i++) {
      // Si no es un espacio en blanco, es una palabra que el backend contó
      if (allItems[i].trim().length > 0) {
        if (wordCounter === currentEntry.originalIndex) {
          // Solo actualizamos si el índice cambió para evitar re-renders innecesarios
          if (this.activeIndex() !== i) {
            this.activeIndex.set(i);
          }
          return; 
        }
        wordCounter++;
      }
    }
  } else {
    // Si estamos en un silencio entre palabras, opcionalmente podemos quitar el highlight
    // this.activeIndex.set(-1); 
  } */
 }
}

  // This runs whenever the player emits a new time
/*   handleTimeUpdate(currentTime: number) {
    if (currentTime === -1) {
      this.activeIndex.set(-1);
      return;
    }

    const data = this.syncData(); */
    // We only want to find the index among the actual words (not spaces)
    // If your words() array includes spaces, you might need a mapping logic
/*     const index = data.findIndex(w => currentTime >= w.start && currentTime <= w.end);
    
    if (index !== this.activeIndex()) {
      this.activeIndex.set(index);
    } */
/*      const currentEntry = data.find(w => 
      currentTime >= w.start && currentTime <= w.end
    ); */
/*     if (currentEntry) {
      this.activeIndex.set(currentEntry.originalIndex);
    }  */

/*     if (currentEntry) {
      // 2. Direct mapping: Set the active index to the original index
      // This will work even if your words() array has spaces/punctuation
      this.activeIndex.set(currentEntry.originalIndex);
    } */
/*      if (currentEntry) {
    const allWords = this.words();
    let wordCounter = 0;
    
    // Buscamos el índice en el array que tiene espacios
    for (let i = 0; i < allWords.length; i++) {
      if (allWords[i].trim().length > 0) { // Si es una palabra
        if (wordCounter === currentEntry.originalIndex) {
          this.activeIndex.set(i);
          return;
        }
        wordCounter++;
      }
    }
  } */
 //console.log(this.words().slice(0,5));
/*    if (currentEntry) {
    const allItems = this.words(); // ['Hello', ' ', 'there!', ' ', 'Welcome']
    let wordCounter = 0;
    
    // We iterate through every item (words and spaces)
    for (let i = 0; i < allItems.length; i++) {
      // Check if this specific item is a "word" (not just whitespace)
      if (allItems[i].trim().length > 0) {
        
        // If this is the Nth word the backend is talking about...
        if (wordCounter === currentEntry.originalIndex) {
          if (this.activeIndex() !== i) {
            this.activeIndex.set(i);
          }
          return; // Exit once found
        }
        
        // Only increment the counter if it was a word
        wordCounter++;
      } */


        /*    if (currentEntry) {
    const allWords = this.words();
    const targetTerm = currentEntry.term.trim().toLowerCase();

    // 2. Find the index in the HTML loop
    // Logic: Look for the word that matches the text at roughly that position
    const foundIndex = allWords.findIndex((word, index) => {
      const cleanWord = word.trim().toLowerCase().replace(/[.,!?;:]/g, '');
      const cleanTarget = targetTerm.replace(/[.,!?;:]/g, '');
      
      // Check if text matches AND we are around the originalIndex
      // (This handles duplicate words like "the... the...")
      return cleanWord === cleanTarget && 
             Math.abs(this.getWordCountUntil(index) - currentEntry.originalIndex) <= 1;
    });

    if (foundIndex !== -1 && foundIndex !== this.activeIndex()) {
      this.activeIndex.set(foundIndex);
    }
  } */
  
  //}

  // Helper to count non-empty words up to a specific index
private getWordCountUntil(targetIndex: number): number {
  return this.words()
    .slice(0, targetIndex)
    .filter(w => w.trim().length > 0).length;
}
  fetchAudio() {
    if (this.textForm.text().valid()) {
      this.speech.speak(this.textModel().text).subscribe(blob => {
        this.audioBlob.set(blob);
      });
    }
  }

  // 2. Inicializa el formulario con validaciones
  textForm = form(this.textModel, (s) => {
    required(s.text);
    minLength(s.text, 10);
  });

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
/*       this.speech.speak(this.textModel().text).subscribe({
      next: (blob: Blob) => {
        console.log('Audio blob received:', blob);
        
        // Create a URL for the blob
        const url = window.URL.createObjectURL(blob);
        
        // Play it immediately
        const audio = new Audio();
        audio.src = url;
        audio.play();
      },
        error: (err) => console.log('Error obteniendo audio ', err)
      }); */
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



/*   handleKeyDown(event: KeyboardEvent, word: string) {
    console.log(event);
    // Verificamos si se presionó Shift + un número (0-9)
    const isNumber = /^[0-9]$/.test(event.key);
    
    if (event.shiftKey && isNumber) {
      event.preventDefault(); // Evita comportamientos extraños del navegador
      this.processStatusUpdate(word, event.key);
    }
  } */
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
      
      
      
/*       .subscribe(res => {
        console.log('AddUserWord via shift + status');
        console.log(res);
        //this.updateSingleWordAnalysis(res);
      }); */
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
      
      
      
      
/*       .subscribe(res => {
        //this.updateSingleWordAnalysis(res);
        console.log('UpdateserWord via shift + status');
        console.log(res);
      }); */
    }
  }


}



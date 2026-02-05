import { Component, computed, inject, signal } from '@angular/core';
import { form, FormField, required, minLength } from '@angular/forms/signals';
import { TEST_TTS } from './text.const';
import { AnalyzeText } from '../../services/analyze-text';
import { LearningService } from '../../services/learning-service';
import { SpeechService } from '../../services/speech-service';

// Definimos una interfaz para tener autocompletado y evitar errores
interface WordAnalysis {
  term: string;
  lemma: string;
  status: string;
  targetLemma?: string;
}

@Component({
  selector: 'app-reader',
  imports: [FormField],
  templateUrl: './reader.html',
  styleUrl: './reader.css',
})
export class Reader {
  analyze = inject(AnalyzeText);
  learning = inject(LearningService);
  speech = inject(SpeechService);
  // 1. Crea el modelo de datos (Signal)
  textModel = signal({ text: TEST_TTS });

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
      this.speech.speak(this.textModel().text).subscribe({
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
      });
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

    if (currentInfo?.status === 'UNKNOWN') {
      // Si es desconocida, llamamos al endpoint de "crear"
      this.learning.addUserWord({term: word, status: newStatus}).subscribe({
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
      this.learning.updateUserWordStatus({term: word, status: newStatus}).subscribe({
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



import { Component, computed, inject, signal } from '@angular/core';
import { form, FormField, required, minLength } from '@angular/forms/signals';
import { WELCOME_TO_THE_JUNGLE } from './text.const';
import { AnalyzeText } from '../../services/analyze-text';

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
  // 1. Crea el modelo de datos (Signal)
  textModel = signal({ text: WELCOME_TO_THE_JUNGLE });

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
        //this.selectedWordInfo.set(response as WordAnalysis);
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

}



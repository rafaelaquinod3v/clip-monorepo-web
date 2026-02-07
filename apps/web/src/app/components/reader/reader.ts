import { Component, computed, inject, OnInit, Signal, signal } from '@angular/core';
import { form, FormField, required, minLength } from '@angular/forms/signals';
import { TEST_TTS } from './text.const';
import { AnalyzeText } from '../../services/analyze-text';
import { LearningService } from '../../services/learning-service';
import { SpeechService, TtsResponse, WordAlignment } from '../../services/speech-service';
import { AudioPlayer } from '../audio-player/audio-player';
import { base64ToWav, handleTimeUpdate } from './audio';
import { handleKeyDownHelper, handleWordClickEnterOrSpaceHelper } from './keyboard-mouse';
import { WordAnalysis, interleaveHelper, updateOptimisticLocalUserWordStatusHelper } from './helper';

@Component({
  selector: 'app-reader',
  imports: [FormField, AudioPlayer],
  templateUrl: './reader.html',
  styleUrl: './reader.css',
})
export class Reader implements OnInit {
  protected readonly handleTimeUpdate = handleTimeUpdate;

  // Services
  analyzeTextService = inject(AnalyzeText);
  learningService = inject(LearningService);
  speechService = inject(SpeechService);

  ngOnInit(): void {
    this.speechService.synthesize(this.textModel().text).subscribe((res: TtsResponse) => {      
      this.alignmentData.set(res.alignment);
      this.audioBlob.set(base64ToWav(res.audio));
    });
  }

  textModel = signal({ text: TEST_TTS });
  textForm = form(this.textModel, (s) => {
    required(s.text);
    minLength(s.text, 10);
  });

  // text analysis
  rawResponse = signal<string>("");
  wordAnalysisResponse = signal<WordAnalysis[]>([]);

  currentTime = signal<number>(0);
  audioBlob = signal<Blob | null>(null);

  // text interaction
  alignmentData = signal<WordAlignment[]>([]);    
  selectedWord = signal<string | null>(null);
 // activeIndex = signal<number>(-1);

  //computed
  syncAlignmentData: Signal<WordAlignment[]> = computed(() => interleaveHelper(this.alignmentData(), 
      { term: " ",
        start: 0,
        end: 0,
        originalIndex: -1,
        newIndex: -1,
      }
    )
  );

  activeIndex = computed(() => {
    const time = this.currentTime();
    const data = this.syncAlignmentData();
    console.log(time);
    // Find the word whose range includes the current time
    return data.findIndex(word => time >= word.start && time <= word.end);
  });

  words = computed(() => {
    const text = this.rawResponse();
    if (!text) return [];
      // Separamos por espacios y saltos de línea
    return text.split(/(\s+)/); 
  });

  selectedWordInfo = computed(() => {
      const word = this.selectedWord();
      const data = this.wordAnalysisResponse();
      
      if (!word || !data) return null;

      // Buscamos la palabra en el análisis (ignorando mayúsculas/minúsculas)
      return data.find(item => 
        item.term.toLowerCase() === word.toLowerCase()
      );
    }
  );

  // api
  fetchAudioOnly() {
    if (this.textForm.text().valid()) {
      this.speechService.speak(this.textModel().text).subscribe(blob => {
        this.audioBlob.set(blob);
      });
    }
  }

  fetchTextAnalysis() {
    if (this.textForm.text().valid()) {
      this.analyzeTextService.analyzeText(this.textModel().text).subscribe((response: any) => {
            this.rawResponse.set(response.rawText);
            this.wordAnalysisResponse.set(response.words);
      });
    }
  }

  onTimeUpdate(time: number) {
  //const driftCorrection = 0.727; 
  //this.currentTime.set(time * driftCorrection);
    // El factor es: SampleRateArchivo / SampleRateNativoNavegador
  // Ejemplo: 22050 / 48000 ≈ 0.459
/*   const factor = 22050 / 48000;
  this.currentTime.set(time * factor); */
    
  
  // If the audio is literally 3x faster (rare but happens if header is ignored):
  // const driftCorrection = 48000 / 16000; 
//const driftCorrection = 16600 / 16000; // Start with 1.0
  //this.currentTime.set(time * driftCorrection); 

    // 1.0375 is your current factor (16600/16000)
  const precisionFactor = 1.0375; 
  this.currentTime.set(time * precisionFactor);
  //this.currentTime.set(time);
    //this.currentTime.set(time); // <--- AQUÍ se le asigna el valor al Signal
  }

  updateSingleWordAnalysis(newData: WordAnalysis) {
    this.wordAnalysisResponse.update(currentList => {
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

  private fetchSingleWordAnalysis(term: string) {
    this.analyzeTextService.analyzeSingleWord(term)
      .subscribe((analysis: WordAnalysis) => 
        this.updateSingleWordAnalysis(analysis));
  }

  private UpdateStatusProcess(word: string, key: string) {
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
      this.learningService.addUserWord({term: word, statusCode: key})
        .subscribe(() => 
          this.wordAnalysisResponse.update(currentList => updateOptimisticLocalUserWordStatusHelper(currentList, word, newStatus)));      
    } else {
      // Si ya existe, llamamos al endpoint de "actualizar status"
      this.learningService.updateUserWordStatus({term: word, statusCode: key})
        .subscribe(() =>
          this.wordAnalysisResponse.update(currentList => updateOptimisticLocalUserWordStatusHelper(currentList, word, newStatus)));      
    }
  }

  // Keyboard / Mouse

  handleKeyDown(event: KeyboardEvent, term: string) {
    const keyCode = handleKeyDownHelper(event);
    if(term && keyCode){
      this.UpdateStatusProcess(term, keyCode);
    }
  }

  handleWordClickEnterOrSpace(term: string) {
    const data = this.syncAlignmentData();
    
    // Find the word whose range includes the current time
    console.log( data.findIndex(word => word.term.toLowerCase() === term.toLocaleLowerCase() ));
console.log( data.find(word => word.term.toLowerCase() === term.toLocaleLowerCase() )?.start);
console.log( data.find(word => word.term.toLowerCase() === term.toLocaleLowerCase() )?.newIndex);
/*     const cleanTerm = handleWordClickEnterOrSpaceHelper(word, this.speechService);
    this.selectedWord.set(cleanTerm);
    //console.log('Palabra interactiva:', cleanTerm);
    const currentInfo = this.selectedWordInfo();
    if(currentInfo?.status === 'UNKNOWN'){
      this.fetchSingleWordAnalysis(cleanTerm);
    } */
  }
}



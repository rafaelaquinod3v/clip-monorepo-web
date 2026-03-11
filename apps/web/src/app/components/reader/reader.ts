import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { form, FormField, required, minLength } from '@angular/forms/signals';
import { DELICIOUS_TTS, JUNGLE, STRESS_TTS, TEST_TTS, WELCOME_TO_THE_JUNGLE } from './text.const';
import { AnalyzeText } from '../../services/analyze-text';
/* import { LearningService } from '../../services/learning-service'; */
/* import { SpeechService, TtsResponse, WordAlignment } from '../../services/speech-service'; */
import { AudioPlayer } from '../audio-player/audio-player';
import { base64ToMp3Fast } from './audio';
import { handleKeyDownHelper, handleWordClickEnterOrSpaceHelper } from './keyboard-mouse';
import { WordAnalysis, updateOptimisticLocalUserWordStatusHelper } from './helper';
import { LearningService } from '../../features/vocabulary/data-access/learning-service';
import { SpeechService, TtsResponse, WordAlignment } from '../../features/audio/data-access/speech-service';

@Component({
  selector: 'app-reader',
  imports: [FormField, AudioPlayer],
  templateUrl: './reader.html',
  styleUrl: './reader.css',
})
export class Reader implements OnInit {
  // Services
  analyzeTextService = inject(AnalyzeText);
  learningService = inject(LearningService);
  speechService = inject(SpeechService);

  ngOnInit(): void {
   
    this.fetchTextAnalysis();
    this.speechService.synthesize(this.textModel().text).subscribe((res: TtsResponse) => {      
      this.alignmentData.set(res.alignment);
      this.audioBlob.set(base64ToMp3Fast(res.audio));
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

  activeIndex = computed(() => {
    const time = this.currentTime();
    const data = this.alignmentData();
    console.log(time);
    // Find the word whose range includes the current time
    return data.find(word => time >= word.start && time <= word.end)?.index;
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
    // 1.0375 is your current factor (16600/16000)
    //const precisionFactor = 1.0375; 
    this.currentTime.set(time);
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
    const cleanTerm = handleWordClickEnterOrSpaceHelper(term, this.speechService);
    this.selectedWord.set(cleanTerm);
    const currentInfo = this.selectedWordInfo();
    if(currentInfo?.status === 'UNKNOWN'){
      this.fetchSingleWordAnalysis(cleanTerm);
    }
  }
}



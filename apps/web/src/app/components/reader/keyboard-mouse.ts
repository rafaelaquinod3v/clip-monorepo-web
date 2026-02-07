import { SpeechService } from "../../services/speech-service";

// keyboard.ts
export function handleKeyDownHelper(event: KeyboardEvent): string | undefined {    
    // 1. Detectar si Shift está presionado
    const isShift = event.shiftKey;

    // 2. Detectar si la tecla física es un número (Digit0 al Digit9)
    const isDigit = event.code.startsWith('Digit');
    const isNumpad = event.code.startsWith('Numpad');

    if (isShift && (isDigit || isNumpad)) {
      event.preventDefault(); // Evita que se escriba el símbolo en otros campos
      
      // Extraemos el número final del string "Digit1", "Digit2", etc.
      const numberPressed = event.code.replace('Digit', '').replace('Numpad', '');
      
      //console.log(`Combinación detectada: Shift + ${numberPressed}`);
      // Validate it's a single digit (0-9)
      if (/^[0-9]$/.test(numberPressed)) {
       return numberPressed;
      }
    }
    return;
  }

  export function handleWordClickEnterOrSpaceHelper(term: string, speech: SpeechService): string {
    const cleanTerm = term.trim().replace(/[.,!?;:]/g, '');
    if (cleanTerm) {
      speech.speak(cleanTerm).subscribe({
      next: (blob: Blob) => {
        //console.log('Audio blob received:', blob);
        
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
    }
    return cleanTerm;
  }
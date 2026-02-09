export function base64ToWav(audio: string) : Blob {
/*     const binary = atob(audio.slice(0, 100)); // Decode only the start
    const buffer = new ArrayBuffer(binary.length);
    const view = new DataView(buffer);

    for (let i = 0; i < binary.length; i++) {
      view.setUint8(i, binary.charCodeAt(i));
    }

    // Byte 24-27: Sample Rate (Little Endian)
    const sampleRate = view.getUint32(24, true); 
    // Byte 28-31: Byte Rate (SampleRate * NumChannels * BitsPerSample/8)
    const byteRate = view.getUint32(28, true);

    console.log("--- WAV Header Check ---");
    console.log("Sample Rate:", sampleRate, "Hz");
    console.log("Byte Rate:", byteRate);
    console.log("Is consistent?", byteRate === sampleRate * 1 * 2 ? "✅ YES" : "❌ NO"); */
    const byteCharacters = atob(audio);
    const byteNumbers = new Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
    byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);
    return new Blob([byteArray], { type: 'audio/wav' });
}

export function base64ToMp3(base64: string): Blob {
    // 1. Decodificar Base64 a una cadena de bytes
    const byteCharacters = atob(base64);
    
    // 2. Crear un array de bytes eficiente (Uint8Array)
    const byteNumbers = new Uint8Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    
    // 3. Retornar el Blob con el tipo de contenido correcto para MP3
    return new Blob([byteNumbers], { type: 'audio/mpeg' });
}

export function base64ToMp3Fast(base64: string): Blob {
    const binaryString = atob(base64);
    const bytes = Uint8Array.from(binaryString, c => c.charCodeAt(0));
    return new Blob([bytes], { type: 'audio/mpeg' });
}


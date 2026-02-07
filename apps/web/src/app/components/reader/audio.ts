export function base64ToWav(audio: string) : Blob {
    const byteCharacters = atob(audio);
    const byteNumbers = new Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
    byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);
    return new Blob([byteArray], { type: 'audio/wav' });
}

export function handleTimeUpdate(currentTime: number) {
    console.log("audio player");
    console.log(currentTime);
}

/*   handleTimeUpdate(currentTime: number) {
  // 1. Reset si el audio termina
  if (currentTime === -1) {
    this.activeIndex.set(-1);
    return;
  }
  
  // 2. Encontrar la palabra que suena en este segundo
  const currentEntry = this.syncAlignmentData().find(w => currentTime > w.start && currentTime < w.end);
  console.log("evento audio");
  console.log(currentEntry);
console.log(this.syncAlignmentData());
  if (currentEntry) {
    this.activeIndex.set(currentEntry.newIndex);
    console.log(currentEntry);
    const allItems = this.words();
    console.log(allItems);
   // console.log(data);
    console.log(this.syncAlignmentData());
 }
} */
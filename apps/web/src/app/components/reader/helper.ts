export interface WordAnalysis {
  term: string;
  lemma: string;
  status: string;
  targetLemma?: string;
}

export function interleaveHelper(arr: any[], separatorBase: any): any[] {
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
}

export function updateOptimisticLocalUserWordStatusHelper(currentList: WordAnalysis[], term: string, newStatus: string): WordAnalysis[] {
    return currentList.map(item => 
        item.term.toLowerCase() === term.toLowerCase()
        ? {...item, status: newStatus} : item
    );
}

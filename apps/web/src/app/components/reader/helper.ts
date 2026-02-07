export interface WordAnalysis {
  term: string;
  lemma: string;
  status: string;
  targetLemma?: string;
}

export function updateOptimisticLocalUserWordStatusHelper(currentList: WordAnalysis[], term: string, newStatus: string): WordAnalysis[] {
    return currentList.map(item => 
        item.term.toLowerCase() === term.toLowerCase()
        ? {...item, status: newStatus} : item
    );
}

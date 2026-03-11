export interface PageResult {
    html: string;
    count: number;
    plainText: string;
}

export class Pagination {
    static checkFit(phrases: string[], container: HTMLElement): number {
        container.innerHTML = '';
        let count = 0;
        for(let i = 0; i < phrases.length; i++) {
            const previousHTML = container.innerHTML;
            const words = phrases[i].split(' ').map(word => `<span>${word}</span>`).join(' ');
            container.innerHTML += words + " "; 
            if(container.scrollHeight > container.clientHeight) {
                container.innerHTML = previousHTML;
                return i;
            }
            count++; 
        }
        return count;
    }

    static generatePageContent(phrases: string[], container: HTMLElement): PageResult {
        const capacity = Pagination.checkFit(phrases, container);

        const visiblePhrases = phrases.slice(0, capacity);

        const contentPage = 
            visiblePhrases
                .map(phrase => phrase.split(' ').map(word => `<span>${word}</span>`).join(' '))
                .join(' ');

        const plainText = visiblePhrases.join(' ');
        
        return {html: contentPage, count: capacity, plainText};
    }
}


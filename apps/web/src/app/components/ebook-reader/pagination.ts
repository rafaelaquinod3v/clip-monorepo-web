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

    static generateContentPage(phrases: string[], container: HTMLElement): string {
        const capacity = Pagination.checkFit(phrases, container);
        const contentPage = 
            phrases
                .slice(0, capacity)
                .map(phrase => phrase.split(' ').map(word => `<span>${word}</span>`).join(' '))
                .join(' ');
        return contentPage;
    }
}
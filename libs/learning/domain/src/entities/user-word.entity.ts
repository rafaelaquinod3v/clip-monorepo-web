import { WordStatus } from "../value-objects/word-status.vo";

export class UserWord {
    constructor(
        public readonly id: string,
        public readonly lemma: string,
        private _lastReview: Date,
        private _grading: WordStatus,
        private _isIgnored: boolean,
    ) {}

    public ignore(): void {
        this._isIgnored = true;
    }

    public stopIgnoring(): void {
        this._isIgnored = false;
    }
    get lastReview() { return this._lastReview; }

    public promote(): void {
        if (this._isIgnored) return; // Business Rule: Cannot promote an ignored word
        this._grading = this._grading.next();
    }

    // Getters for the outside world
    get isIgnored() { return this._isIgnored; }
    get currentGrading() { return this._grading.value; }
}
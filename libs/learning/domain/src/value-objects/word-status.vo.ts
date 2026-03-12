export enum FamiliarityGrading {
  NEW = 1,
  RECOGNIZED = 2,
  FAMILIAR = 3,
  LEARNED = 4,
  KNOWN = 5,
}

export class WordStatus {
  private readonly _value: FamiliarityGrading;

  constructor(value: number | FamiliarityGrading) {
    if (!Object.values(FamiliarityGrading).includes(value as FamiliarityGrading)) {
      throw new Error(`Invalid Word Status value: ${value}`);
    }
    this._value = value as FamiliarityGrading;
  }

  get value(): FamiliarityGrading {
    return this._value;
  }

  // Business Logic: Is the word fully mastered?
  get isMastered(): boolean {
    return this._value === FamiliarityGrading.KNOWN;
  }

  // Business Logic: Should we hide it from study sessions?
  get isExemptFromStudy(): boolean {
    return this._value === FamiliarityGrading.KNOWN;
  }

  // Metadata: Helpful for the UI (Feature Layer)
  get label(): string {
    return FamiliarityGrading[this._value];
  }


  // Logic to move to the next step in the learning ladder
  public next(): WordStatus {
    if (this._value >= FamiliarityGrading.KNOWN) return this;
    return new WordStatus(this._value + 1);
  }
}

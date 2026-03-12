export enum CEFRLevel {
  A1 = 'A1',
  A2 = 'A2',
  B1 = 'B1',
  B2 = 'B2',
  C1 = 'C1',
  C2 = 'C2'
}

export class WordLevel {
  private readonly _value: CEFRLevel;

  // Mapa de pesos para poder comparar niveles (A1 < C2)
  private static readonly LEVEL_WEIGHTS: Record<CEFRLevel, number> = {
    [CEFRLevel.A1]: 1, [CEFRLevel.A2]: 2,
    [CEFRLevel.B1]: 3, [CEFRLevel.B2]: 4,
    [CEFRLevel.C1]: 5, [CEFRLevel.C2]: 6
  };

  constructor(value: string | CEFRLevel) {
    if (!Object.values(CEFRLevel).includes(value as CEFRLevel)) {
      throw new Error(`Nivel MCER inválido: ${value}`);
    }
    this._value = value as CEFRLevel;
  }

  get value(): CEFRLevel {
    return this._value;
  }

  // Lógica de negocio: ¿Es un nivel avanzado?
  public isAdvanced(): boolean {
    return this._value === CEFRLevel.C1 || this._value === CEFRLevel.C2;
  }

  // Comparación: ¿Este nivel es superior a otro?
  public isHigherThan(other: WordLevel): boolean {
    return WordLevel.LEVEL_WEIGHTS[this._value] > WordLevel.LEVEL_WEIGHTS[other.value];
  }
}

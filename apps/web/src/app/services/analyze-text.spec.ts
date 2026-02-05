import { TestBed } from '@angular/core/testing';

import { AnalyzeText } from './analyze-text';

describe('AnalyzeText', () => {
  let service: AnalyzeText;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AnalyzeText);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

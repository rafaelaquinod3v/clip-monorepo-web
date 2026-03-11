import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EbookReaderComponent } from './ebook-reader.component';

describe('EbookReader', () => {
  let component: EbookReaderComponent;
  let fixture: ComponentFixture<EbookReaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EbookReaderComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(Epub);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EbookPageComponent } from './ebook-page.component';

describe('EbookPageComponent', () => {
  let component: EbookPageComponent;
  let fixture: ComponentFixture<EbookPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EbookPageComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(EbookPageComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

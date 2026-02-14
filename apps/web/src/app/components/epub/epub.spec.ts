import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Epub } from './epub';

describe('Epub', () => {
  let component: Epub;
  let fixture: ComponentFixture<Epub>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Epub],
    }).compileComponents();

    fixture = TestBed.createComponent(Epub);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

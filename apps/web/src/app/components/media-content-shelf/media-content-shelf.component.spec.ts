import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MediaContentShelfComponent } from './media-content-shelf.component';

describe('MediaContentShelfComponent', () => {
  let component: MediaContentShelfComponent;
  let fixture: ComponentFixture<MediaContentShelfComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MediaContentShelfComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(MediaContentShelfComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

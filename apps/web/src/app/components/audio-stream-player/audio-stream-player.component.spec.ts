import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AudioStreamPlayerComponent } from './audio-stream-player.component';

describe('AudioStreamPlayerComponent', () => {
  let component: AudioStreamPlayerComponent;
  let fixture: ComponentFixture<AudioStreamPlayerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AudioStreamPlayerComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(AudioStreamPlayerComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

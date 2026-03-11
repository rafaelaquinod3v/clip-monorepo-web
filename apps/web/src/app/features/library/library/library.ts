import { Component } from '@angular/core';
import { MediaContentShelfComponent } from '../../../components/media-content-shelf/media-content-shelf.component';
import { MediaType } from '../../../models/media-content.model';
@Component({
  selector: 'app-library',
  imports: [MediaContentShelfComponent],
  templateUrl: './library.html',
  styleUrl: './library.css',
})
export class Library {
  ebookAllowedTypes: MediaType[] = ['PDF', 'EPUB'];
}

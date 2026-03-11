import { Component } from '@angular/core';

import { MediaType } from '../../../models/media-content.model';
import { MediaContentShelfComponent } from '../components/media-content-shelf/media-content-shelf.component';
@Component({
  selector: 'app-library',
  imports: [MediaContentShelfComponent],
  templateUrl: './library.html',
  styleUrl: './library.css',
})
export class Library {
  ebookAllowedTypes: MediaType[] = ['PDF', 'EPUB'];
}

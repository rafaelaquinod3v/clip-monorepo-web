import { Component } from '@angular/core';
import { EbookReaderComponent } from '../ebook-reader/ebook-reader.component';


@Component({
  selector: 'app-ebook-page',
  imports: [EbookReaderComponent],
  templateUrl: './ebook-page.component.html',
  styleUrl: './ebook-page.component.css',
})
export class EbookPageComponent {}

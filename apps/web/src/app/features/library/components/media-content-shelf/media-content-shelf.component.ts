import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { EpubService } from '../../../../services/epub-service';
import { MediaContentResponse, MediaType } from '../../../../models/media-content.model';
import { RouterLink } from '@angular/router';


@Component({
  selector: 'app-media-content-shelf',
  imports: [RouterLink],
  templateUrl: './media-content-shelf.component.html',
  styleUrl: './media-content-shelf.component.css',
})
export class MediaContentShelfComponent implements OnInit {

  epubService = inject(EpubService);

  @Input({required: true}) mediaTypes!: MediaType[];
  @Input({required: true}) limit = 10;

  private offset = 0;
  private sortOrder = 'DESC';

  mediaContentShelfItems = signal<MediaContentResponse[]>([])

  ngOnInit(): void {
    this.epubService.loadMediaContent(this.offset, this.limit, "uploadedAt", this.sortOrder, this.mediaTypes).subscribe((response) => {
      console.log(response);
      this.mediaContentShelfItems.set(response)
    });
  }

  getDisplayTitle(item: MediaContentResponse): string {
    if (item.metadata?.type === 'EPUB' || item.metadata?.type === 'PDF') {
      const fields = item.metadata.fields as { title?: string };
      if (fields.title) return fields.title;
    }
    return item.originalFilename ?? item.fileName;
  }
}

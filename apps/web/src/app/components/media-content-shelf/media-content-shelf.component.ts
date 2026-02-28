import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { EpubService } from '../../services/epub-service';
import { MediaContentResponse } from '../../models/media-content.model';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-media-content-shelf',
  imports: [RouterLink],
  templateUrl: './media-content-shelf.component.html',
  styleUrl: './media-content-shelf.component.css',
})
export class MediaContentShelfComponent implements OnInit {

  epubService = inject(EpubService);

  @Input({required: true}) contentType!: string;
  @Input({required: true}) limit = 10;

  private offset = 0;
  private sortOrder = 'desc';

  mediaContentShelfItems = signal<MediaContentResponse[]>([])

  ngOnInit(): void {
    this.epubService.loadMediaContent(this.offset, this.limit, this.sortOrder).subscribe((response) => {
      console.log(response);
      this.mediaContentShelfItems.set(response)
    });
  }

  getEpubMetadata(item: MediaContentResponse): { title: string; author: string } | null {
    if (item.metadata?.type === 'EPUB') {
      return item.metadata.fields as { title: string; author: string };
    }
    return null;
  }

  getDisplayTitle(item: MediaContentResponse): string {
    if (item.metadata?.type === 'EPUB' || item.metadata?.type === 'PDF') {
      const fields = item.metadata.fields as { title?: string };
      if (fields.title) return fields.title;
    }
    return item.originalFilename ?? item.fileName;
  }
}

export type MediaContentMetadata =
  | { type: 'EPUB';  fields: { title: string; author: string } }
  | { type: 'PDF';   fields: { title: string; author: string } }
  | { type: 'AUDIO'; fields: { duration: number; bitrate: number } }
  | { type: 'VIDEO'; fields: { duration: number } };

export interface MediaContentResponse {
  id: string;
  fileName: string;
  originalFilename: string;
  fileSize: number;
  mimeType: string;
  mediaType: 'EPUB' | 'PDF' | 'AUDIO' | 'VIDEO';
  uploadedAt: string;
  metadata: MediaContentMetadata | null;
}

export type MediaType = 'PDF' | 'EPUB' | 'VIDEO' | 'AUDIO';
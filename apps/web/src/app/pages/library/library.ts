import { Component } from '@angular/core';
import { Reader } from '../../components/reader/reader';
import { Dictionary } from '../../components/dictionary/dictionary';

@Component({
  selector: 'app-library',
  imports: [Reader, Dictionary],
  templateUrl: './library.html',
  styleUrl: './library.css',
})
export class Library {}

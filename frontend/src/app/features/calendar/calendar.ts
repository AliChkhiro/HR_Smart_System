import { Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-calendar',
  imports: [MatCardModule, MatIconModule],
  templateUrl: './calendar.html',
  styleUrl: './calendar.scss',
})
export class Calendar {}

import { Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-tasks',
  imports: [MatCardModule, MatIconModule],
  templateUrl: './tasks.html',
  styleUrl: './tasks.scss',
})
export class Tasks {}

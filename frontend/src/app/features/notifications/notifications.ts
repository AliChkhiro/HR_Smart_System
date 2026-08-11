import { Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-notifications',
  imports: [MatCardModule, MatIconModule],
  templateUrl: './notifications.html',
  styleUrl: './notifications.scss',
})
export class Notifications {}

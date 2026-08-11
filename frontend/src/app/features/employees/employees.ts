import { Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-employees',
  imports: [MatCardModule, MatIconModule],
  templateUrl: './employees.html',
  styleUrl: './employees.scss',
})
export class Employees {}

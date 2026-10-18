import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule
  ],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly form = this.fb.group({
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  // Signaux : l'application tourne sans zone.js (Angular 22), la détection de changements
  // ne se déclenche donc que sur signal, événement ou markForCheck. Des champs simples
  // laissés sans déclencheur laissaient le spinner actif et rendaient les messages
  // d'échec ou de succès invisibles jusqu'à une interaction de l'utilisateur.
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly successMessage = signal<string | null>(null);

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    const { firstName, lastName, email, password } = this.form.getRawValue();
    this.authService.register({
      firstName: firstName!,
      lastName: lastName!,
      email: email!,
      password: password!
    }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.successMessage.set(
          'Votre demande a été envoyée. Un membre des ressources humaines doit valider votre compte avant la première connexion.'
        );
        this.form.reset();
      },
      error: (err) => {
        this.submitting.set(false);
        if (err?.status === 409) {
          this.errorMessage.set('Un compte avec cet email existe déjà.');
        } else {
          this.errorMessage.set('Une erreur est survenue lors de la création du compte.');
        }
      }
    });
  }
}

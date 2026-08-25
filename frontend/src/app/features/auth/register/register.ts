import { Component, inject } from '@angular/core';
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

  protected submitting = false;
  protected errorMessage: string | null = null;
  protected successMessage: string | null = null;

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.submitting = true;
    this.errorMessage = null;
    this.successMessage = null;
    const { firstName, lastName, email, password } = this.form.getRawValue();
    this.authService.register({
      firstName: firstName!,
      lastName: lastName!,
      email: email!,
      password: password!
    }).subscribe({
      next: () => {
        this.submitting = false;
        this.successMessage = 'Compte créé avec succès. Vous pouvez maintenant vous connecter.';
        this.form.reset();
      },
      error: (err) => {
        this.submitting = false;
        if (err?.status === 409) {
          this.errorMessage = 'Un compte avec cet email existe déjà.';
        } else {
          this.errorMessage = 'Une erreur est survenue lors de la création du compte.';
        }
      }
    });
  }
}

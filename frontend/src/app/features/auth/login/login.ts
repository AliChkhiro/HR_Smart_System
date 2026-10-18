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
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-login',
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
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  protected readonly demoHint = environment.demoAccountsHint;
  protected submitting = false;
  protected errorMessage: string | null = null;

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.submitting = true;
    this.errorMessage = null;
    const { email, password } = this.form.getRawValue();
    this.authService.login({ email: email!, password: password! }).subscribe({
      next: (res) => {
        this.authService.setSession(res);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.submitting = false;
        const code = err?.error?.code;
        if (code === 'AUTH_ACCOUNT_PENDING') {
          this.errorMessage = 'Votre compte est en attente de validation par les ressources humaines.';
        } else if (code === 'AUTH_ACCOUNT_DISABLED') {
          this.errorMessage = 'Ce compte est désactivé. Contactez les ressources humaines.';
        } else {
          this.errorMessage = 'Identifiants invalides. Veuillez réessayer.';
        }
      }
    });
  }
}

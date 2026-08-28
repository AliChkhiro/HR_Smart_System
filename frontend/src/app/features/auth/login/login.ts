import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/services/auth.service';

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
  private readonly route = inject(ActivatedRoute);

  protected readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  protected submitting = false;
  protected errorMessage: string | null = null;

  constructor() {
    this.route.queryParamMap.subscribe((params) => {
      if (params.get('registered') === 'true') {
        this.successMessage = 'Compte créé avec succès. Vous pouvez maintenant vous connecter.';
      }
    });
  }

  protected successMessage: string | null = null;

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.submitting = true;
    this.errorMessage = null;
    this.successMessage = null;
    const { email, password } = this.form.getRawValue();
    this.authService.login({ email: email!, password: password! }).subscribe({
      next: (res) => {
        this.authService.setSession(res);
        this.router.navigate(['/dashboard']);
      },
      error: () => {
        this.submitting = false;
        this.errorMessage = 'Identifiants invalides. Veuillez réessayer.';
      }
    });
  }
}

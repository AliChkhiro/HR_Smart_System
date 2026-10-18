import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../models/user.model';

/**
 * Autorise l'accès uniquement aux rôles indiqués (contrôle côté client).
 * Le backend applique de toute façon le RBAC (`@PreAuthorize`) : cette garde
 * n'est qu'une protection d'interface.
 */
export const roleGuard = (...roles: UserRole[]): CanActivateFn => () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const role = authService.user()?.role;
  if (role && roles.includes(role)) {
    return true;
  }
  return router.createUrlTree(['/dashboard']);
};

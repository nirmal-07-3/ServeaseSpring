import { inject } from '@angular/core';
import {
  CanActivateFn,
  Router,
  ActivatedRouteSnapshot
} from '@angular/router';
import { Auth } from '../services/auth';

function getDashboardPath(role: string | null): string {
  const normalizedRole = (role ?? '').toUpperCase().replace('ROLE_', '');

  if (normalizedRole === 'ADMIN') {
    return '/admin/dashboard';
  }

  if (normalizedRole === 'PROVIDER') {
    return '/provider/dashboard';
  }

  if (normalizedRole === 'CUSTOMER') {
    return '/home';
  }

  return '/login';
}

export const authGuard: CanActivateFn = () => {
  const auth = inject(Auth);
  const router = inject(Router);

  if (auth.isLoggedIn()) {
    return true;
  }

  return router.createUrlTree(['/login']);
};

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth = inject(Auth);
  const router = inject(Router);

  if (!auth.isLoggedIn()) {
    return router.createUrlTree(['/login']);
  }

  const role = (auth.getRole() ?? '').toUpperCase().replace('ROLE_', '');
  const allowedRoles = route.data['roles'] as string[] | undefined;

  if (allowedRoles?.includes(role)) {
    return true;
  }

  return router.parseUrl(getDashboardPath(role));
};

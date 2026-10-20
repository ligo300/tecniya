import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Rol } from './models';

/** Exige sesión; si no hay, manda al login recordando a dónde iba. */
export const autenticadoGuard: CanActivateFn = (_ruta, estado) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.autenticado()
    ? true
    : router.createUrlTree(['/login'], { queryParams: { volver: estado.url } });
};

/** Exige uno de los roles indicados. */
export const rolGuard = (...roles: Rol[]): CanActivateFn => () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.autenticado()) { return router.createUrlTree(['/login']); }
  return auth.tieneRol(...roles) ? true : router.createUrlTree([auth.rutaInicio()]);
};

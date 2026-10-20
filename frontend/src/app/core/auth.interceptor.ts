import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

const PUBLICAS = ['/api/auth/login', '/api/auth/registro'];

/** Agrega el Bearer a las llamadas a /api y cierra la sesión si el token ya no vale. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const esApi = req.url.startsWith('/api/');
  const publica = PUBLICAS.some((p) => req.url.startsWith(p));

  const salida = esApi && token && !publica
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(salida).pipe(
    catchError((e: HttpErrorResponse) => {
      if (e.status === 401 && token && !publica) {
        auth.salir();
      }
      return throwError(() => e);
    }),
  );
};

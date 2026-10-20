import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { autenticadoGuard, rolGuard } from './guards';

function jwt(): string {
  const b64 = (o: object) => btoa(JSON.stringify(o)).replace(/=/g, '');
  return `${b64({ alg: 'HS256' })}.${b64({ exp: Math.floor(Date.now() / 1000) + 3600 })}.f`;
}

const ruta = {} as ActivatedRouteSnapshot;
const estado = { url: '/ordenes/5' } as RouterStateSnapshot;

function ejecutar(guard: ReturnType<typeof rolGuard> | typeof autenticadoGuard) {
  return TestBed.runInInjectionContext(() => guard(ruta, estado));
}

describe('guards', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient()] });
  });
  afterEach(() => localStorage.clear());

  function sesion(rol: string) {
    localStorage.setItem('tecniya.sesion', JSON.stringify({ token: jwt(), usuario: { id: 1, email: 'a@b.pe', rol, activo: true } }));
  }

  it('manda al login recordando la ruta si no hay sesión', () => {
    const r = ejecutar(autenticadoGuard) as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(r)).toBe('/login?volver=%2Fordenes%2F5');
  });

  it('deja pasar a un usuario con sesión', () => {
    sesion('TECNICO');
    expect(ejecutar(autenticadoGuard)).toBe(true);
  });

  it('rolGuard deja pasar solo al rol indicado', () => {
    sesion('TECNICO');
    expect(ejecutar(rolGuard('TECNICO'))).toBe(true);
    const r = ejecutar(rolGuard('ADMIN')) as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(r)).toBe('/panel');
  });

  it('rolGuard sin sesión manda al login', () => {
    const r = ejecutar(rolGuard('ADMIN')) as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(r)).toBe('/login');
  });
});

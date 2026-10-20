import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';

function jwt(exp: number): string {
  const b64 = (o: object) => btoa(JSON.stringify(o)).replace(/=/g, '');
  return `${b64({ alg: 'HS256' })}.${b64({ sub: '1', exp })}.firma`;
}

const usuario = { id: 1, email: 'ana@correo.pe', rol: 'TECNICO' as const, activo: true };

describe('AuthService', () => {
  let http: HttpTestingController;

  function crear(): AuthService {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    return TestBed.inject(AuthService);
  }

  beforeEach(() => localStorage.clear());

  it('guarda token y usuario al iniciar sesión', () => {
    const auth = crear();
    auth.login('ana@correo.pe', 'Secreta123').subscribe();
    const token = jwt(Math.floor(Date.now() / 1000) + 3600);
    http.expectOne('/api/auth/login').flush({ token, tipo: 'Bearer', expiraEnMinutos: 120, usuario });

    expect(auth.autenticado()).toBe(true);
    expect(auth.token()).toBe(token);
    expect(auth.tieneRol('TECNICO')).toBe(true);
    expect(auth.tieneRol('ADMIN')).toBe(false);
    expect(auth.rutaInicio()).toBe('/panel');
    expect(localStorage.getItem('tecniya.sesion')).toContain('ana@correo.pe');
  });

  it('recupera la sesión guardada si el token no venció', () => {
    const token = jwt(Math.floor(Date.now() / 1000) + 3600);
    localStorage.setItem('tecniya.sesion', JSON.stringify({ token, usuario }));
    expect(crear().autenticado()).toBe(true);
  });

  it('descarta una sesión con el token vencido', () => {
    const token = jwt(Math.floor(Date.now() / 1000) - 10);
    localStorage.setItem('tecniya.sesion', JSON.stringify({ token, usuario }));
    expect(crear().autenticado()).toBe(false);
  });

  it('salir borra la sesión', () => {
    const token = jwt(Math.floor(Date.now() / 1000) + 3600);
    localStorage.setItem('tecniya.sesion', JSON.stringify({ token, usuario }));
    const auth = crear();
    auth.salir();
    expect(auth.autenticado()).toBe(false);
    expect(localStorage.getItem('tecniya.sesion')).toBeNull();
  });

  it('el registro envía correo, contraseña y rol', () => {
    const auth = crear();
    auth.registrar('b@c.pe', 'Clave12345', 'CLIENTE').subscribe();
    const req = http.expectOne('/api/auth/registro');
    expect(req.request.body).toEqual({ email: 'b@c.pe', password: 'Clave12345', rol: 'CLIENTE' });
    req.flush({ id: 2, email: 'b@c.pe', rol: 'CLIENTE', activo: true });
  });
});

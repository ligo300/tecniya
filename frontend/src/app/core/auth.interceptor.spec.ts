import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';

function jwt(): string {
  const b64 = (o: object) => btoa(JSON.stringify(o)).replace(/=/g, '');
  return `${b64({ alg: 'HS256' })}.${b64({ exp: Math.floor(Date.now() / 1000) + 3600 })}.f`;
}

describe('authInterceptor', () => {
  let http: HttpClient;
  let ctrl: HttpTestingController;
  let auth: AuthService;
  const token = jwt();

  beforeEach(() => {
    localStorage.setItem('tecniya.sesion', JSON.stringify({
      token, usuario: { id: 1, email: 'a@b.pe', rol: 'TECNICO', activo: true },
    }));
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpClient);
    ctrl = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => localStorage.clear());

  it('agrega el Bearer a las llamadas a /api', () => {
    http.get('/api/ordenes').subscribe();
    const req = ctrl.expectOne('/api/ordenes');
    expect(req.request.headers.get('Authorization')).toBe(`Bearer ${token}`);
    req.flush([]);
  });

  it('no manda el token al login ni al registro', () => {
    http.post('/api/auth/login', {}).subscribe();
    const req = ctrl.expectOne('/api/auth/login');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('cierra la sesión cuando la API responde 401', () => {
    http.get('/api/ordenes').subscribe({ error: () => undefined });
    ctrl.expectOne('/api/ordenes').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.autenticado()).toBe(false);
  });
});

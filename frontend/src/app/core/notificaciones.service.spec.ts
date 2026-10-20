import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NotificacionesService } from './notificaciones.service';

function sesion() {
  const b64 = (o: object) => btoa(JSON.stringify(o)).replace(/=/g, '');
  const token = `${b64({ alg: 'HS256' })}.${b64({ exp: Math.floor(Date.now() / 1000) + 3600 })}.f`;
  localStorage.setItem('tecniya.sesion', JSON.stringify({ token, usuario: { id: 1, email: 'a@b.pe', rol: 'CLIENTE', activo: true } }));
}

describe('NotificacionesService', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => localStorage.clear());

  function crear() {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    return { svc: TestBed.inject(NotificacionesService), http: TestBed.inject(HttpTestingController) };
  }

  it('cuenta las no leídas y resta al marcar una', () => {
    sesion();
    const { svc, http } = crear();
    svc.refrescar();
    const req = http.expectOne((r) => r.url === '/api/notificaciones');
    expect(req.request.params.get('soloNoLeidas')).toBe('true');
    req.flush([{ id: 1 }, { id: 2 }, { id: 3 }]);
    expect(svc.noLeidas()).toBe(3);
    svc.restar();
    expect(svc.noLeidas()).toBe(2);
  });

  it('no consulta nada si no hay sesión', () => {
    const { svc, http } = crear();
    svc.refrescar();
    http.expectNone('/api/notificaciones');
    expect(svc.noLeidas()).toBe(0);
  });
});

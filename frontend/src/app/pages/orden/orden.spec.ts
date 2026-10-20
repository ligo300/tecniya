import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { OrdenPagina } from './orden';

function sesion(rol: string) {
  const b64 = (o: object) => btoa(JSON.stringify(o)).replace(/=/g, '');
  const token = `${b64({ alg: 'HS256' })}.${b64({ exp: Math.floor(Date.now() / 1000) + 3600 })}.f`;
  localStorage.setItem('tecniya.sesion', JSON.stringify({ token, usuario: { id: 1, email: 'a@b.pe', rol, activo: true } }));
}

function detalle(estado: string, extra: object = {}) {
  return {
    orden: { id: 9, codigo: 'TY-0009', clienteId: 1, tecnicoId: 2, origen: 'DIRECTA', tipoEquipo: 'PC',
      descripcion: 'No enciende', precioAcordado: 80, estado, creadoEn: '2026-10-19T10:00:00', actualizadoEn: '2026-10-19T10:00:00' },
    historial: [], fotos: [], pagos: [], calificacion: null, ...extra,
  };
}

async function abrir(rol: string, d: object) {
  localStorage.clear();
  sesion(rol);
  await TestBed.configureTestingModule({
    imports: [OrdenPagina],
    providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
  }).compileComponents();
  const http = TestBed.inject(HttpTestingController);
  const fixture = TestBed.createComponent(OrdenPagina);
  fixture.componentRef.setInput('id', '9');
  await fixture.whenStable();
  http.expectOne('/api/ordenes/9').flush(d);
  await fixture.whenStable();
  return { http, fixture, el: fixture.nativeElement as HTMLElement };
}

describe('OrdenPagina', () => {
  afterEach(() => localStorage.clear());

  it('el cliente ve el formulario de pago cuando el equipo está LISTO', async () => {
    const { el, fixture, http } = await abrir('CLIENTE', detalle('LISTO'));
    expect(el.textContent).toContain('paga para recibirlo');
    expect(fixture.componentInstance.formPago.controls.monto.value).toBe(80);

    fixture.componentInstance.formPago.patchValue({ medio: 'YAPE', nroOperacion: 'OP-123' });
    fixture.componentInstance.pagar(9);
    const req = http.expectOne('/api/ordenes/9/pago');
    expect(req.request.body).toEqual({ monto: 80, medio: 'YAPE', nroOperacion: 'OP-123' });
    req.flush({ id: 1, ordenId: 9, monto: 80, medio: 'YAPE', nroOperacion: 'OP-123', estado: 'PAGADO', fecha: '' });
    http.expectOne('/api/ordenes/9').flush(detalle('LISTO', { pagos: [{ id: 1, estado: 'PAGADO', monto: 80, medio: 'YAPE', nroOperacion: 'OP-123', ordenId: 9, fecha: '' }] }));
    await fixture.whenStable();
    expect(el.textContent).toContain('Pago confirmado');
  });

  it('el cliente puede calificar solo cuando la orden está ENTREGADA', async () => {
    const { el, fixture, http } = await abrir('CLIENTE', detalle('ENTREGADO'));
    expect(el.textContent).toContain('Califica al técnico');
    fixture.componentInstance.formCalif.patchValue({ puntaje: 4, comentario: 'Muy bien' });
    fixture.componentInstance.calificar(9);
    const req = http.expectOne('/api/ordenes/9/calificacion');
    expect(req.request.body).toEqual({ puntaje: 4, comentario: 'Muy bien' });
    req.flush({ id: 1, ordenId: 9, puntaje: 4, comentario: 'Muy bien', fecha: '' });
    http.expectOne('/api/ordenes/9');
  });

  it('el cliente solo cancela en CONTRATADO y el técnico no ve el pago', async () => {
    const a = await abrir('CLIENTE', detalle('RECIBIDO'));
    expect(a.el.textContent).not.toContain('Cancelar orden');
    TestBed.resetTestingModule();
    const b = await abrir('CLIENTE', detalle('CONTRATADO'));
    expect(b.el.textContent).toContain('Cancelar orden');
    TestBed.resetTestingModule();
    const c = await abrir('TECNICO', detalle('LISTO'));
    expect(c.el.textContent).not.toContain('paga para recibirlo');
    expect(c.el.textContent).toContain('Siguiente: Entregado');
  });
});

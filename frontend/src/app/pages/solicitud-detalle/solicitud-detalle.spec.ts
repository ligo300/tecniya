import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { SolicitudDetalle } from './solicitud-detalle';

const solicitud = {
  id: 4, clienteId: 1, tipoEquipo: 'PC', marcaModelo: 'HP 15', descripcionFalla: 'No enciende',
  presupuestoRef: 150, estado: 'CON_OFERTAS', creadoEn: '2026-10-19T10:00:00',
};
const ofertas = [
  { id: 10, solicitudId: 4, tecnicoId: 7, precio: 90, plazoDias: 2, mensaje: 'Lo reviso hoy', estado: 'PENDIENTE', creadoEn: '2026-10-19T11:00:00' },
  { id: 11, solicitudId: 4, tecnicoId: 8, precio: 120, plazoDias: 4, mensaje: null, estado: 'PENDIENTE', creadoEn: '2026-10-19T12:00:00' },
];

describe('SolicitudDetalle', () => {
  it('muestra las ofertas y acepta la elegida', async () => {
    await TestBed.configureTestingModule({
      imports: [SolicitudDetalle],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    const http = TestBed.inject(HttpTestingController);
    const ir = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    const fixture = TestBed.createComponent(SolicitudDetalle);
    fixture.componentRef.setInput('id', '4');
    await fixture.whenStable();
    http.expectOne('/api/solicitudes/4').flush(solicitud);
    http.expectOne('/api/solicitudes/4/ofertas').flush(ofertas);
    const t = (id: number, nombres: string) => ({ id, nombres, apellidos: 'Perez', zona: 'Lima', tarifaBase: 40, estado: 'ACTIVO', califPromedio: 4, califCantidad: 3, especialidades: ['PC'] });
    http.expectOne('/api/tecnicos/7').flush(t(7, 'Ana'));
    http.expectOne('/api/tecnicos/8').flush(t(8, 'Luis'));
    await fixture.whenStable();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Ana Perez');
    expect(el.textContent).toContain('Luis Perez');
    expect(el.querySelectorAll('tbody tr').length).toBe(2);

    (el.querySelector('tbody tr button') as HTMLButtonElement).click();
    const req = http.expectOne('/api/ofertas/10/aceptar');
    expect(req.request.method).toBe('POST');
    req.flush({ ...ofertas[0], estado: 'ACEPTADA' });
    expect(ir).toHaveBeenCalledWith(['/mis-ordenes']);
  });

  it('avisa cuando todavía no hay ofertas', async () => {
    await TestBed.configureTestingModule({
      imports: [SolicitudDetalle],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(SolicitudDetalle);
    fixture.componentRef.setInput('id', '4');
    await fixture.whenStable();
    http.expectOne('/api/solicitudes/4').flush({ ...solicitud, estado: 'ABIERTA' });
    http.expectOne('/api/solicitudes/4/ofertas').flush([]);
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Todavía no hay ofertas');
  });
});

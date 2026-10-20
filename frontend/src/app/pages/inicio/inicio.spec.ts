import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Inicio } from './inicio';

const tecnico = {
  id: 1, nombres: 'Ana', apellidos: 'Quispe', zona: 'Lima Norte', tarifaBase: 40,
  estado: 'ACTIVO', califPromedio: 4.5, califCantidad: 8, especialidades: ['PC', 'TV'],
};

describe('Inicio (catálogo)', () => {
  let ctrl: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Inicio],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    ctrl = TestBed.inject(HttpTestingController);
  });

  it('lista los técnicos que devuelve la API', async () => {
    const fixture = TestBed.createComponent(Inicio);
    await new Promise((r) => setTimeout(r, 300)); // espera el debounce
    ctrl.expectOne((r) => r.url === '/api/tecnicos').flush([tecnico]);
    await fixture.whenStable();
    const html = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(html).toContain('Ana Quispe');
    expect(html).toContain('Lima Norte');
    expect(html).toContain('1 técnico disponible');
  });

  it('manda los filtros como parámetros de la consulta', async () => {
    const fixture = TestBed.createComponent(Inicio);
    await new Promise((r) => setTimeout(r, 300));
    ctrl.expectOne((r) => r.url === '/api/tecnicos').flush([]);

    fixture.componentInstance.filtros.patchValue({ tipo: 'TV', zona: ' Callao ', calificacionMin: 4 });
    await new Promise((r) => setTimeout(r, 300));
    const req = ctrl.expectOne((r) => r.url === '/api/tecnicos');
    expect(req.request.params.get('tipo')).toBe('TV');
    expect(req.request.params.get('zona')).toBe('Callao');
    expect(req.request.params.get('calificacionMin')).toBe('4');
    req.flush([]);
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('No hay técnicos con esos filtros');
  });

  it('muestra el error si el servidor no responde', async () => {
    const fixture = TestBed.createComponent(Inicio);
    await new Promise((r) => setTimeout(r, 300));
    ctrl.expectOne((r) => r.url === '/api/tecnicos').error(new ProgressEvent('error'), { status: 0 });
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('No hay conexión');
  });
});

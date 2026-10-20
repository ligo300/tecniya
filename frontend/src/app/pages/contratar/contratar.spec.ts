import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Contratar } from './contratar';

const tecnico = {
  id: 7, nombres: 'Ana', apellidos: 'Quispe', zona: 'Lima', tarifaBase: 45,
  estado: 'ACTIVO', califPromedio: 0, califCantidad: 0, especialidades: ['CELULAR', 'TV'],
};

describe('Contratar', () => {
  it('contrata directo con el equipo y la descripción elegidos', async () => {
    await TestBed.configureTestingModule({
      imports: [Contratar],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    const http = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    const ir = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    const fixture = TestBed.createComponent(Contratar);
    fixture.componentRef.setInput('id', '7');
    await fixture.whenStable();
    http.expectOne('/api/tecnicos/7').flush(tecnico);
    await fixture.whenStable();

    const c = fixture.componentInstance;
    expect(c.form.controls.tipoEquipo.value).toBe('CELULAR'); // primer equipo que atiende
    c.contratar(tecnico as never); // vacío: no envía
    http.expectNone('/api/ordenes/directa');

    c.form.patchValue({ tipoEquipo: 'TV', descripcion: 'Pantalla sin imagen' });
    c.contratar(tecnico as never);
    const req = http.expectOne('/api/ordenes/directa');
    expect(req.request.body).toEqual({ tecnicoId: 7, tipoEquipo: 'TV', descripcion: 'Pantalla sin imagen' });
    req.flush({ id: 33 });
    expect(ir).toHaveBeenCalledWith(['/ordenes', 33]);
  });
});

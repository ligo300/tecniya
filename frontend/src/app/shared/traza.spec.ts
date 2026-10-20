import { TestBed } from '@angular/core/testing';
import { Traza } from './traza';

describe('Traza', () => {
  it('marca los pasos hechos y el actual', async () => {
    const fixture = TestBed.createComponent(Traza);
    fixture.componentRef.setInput('estado', 'EN_DIAGNOSTICO');
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelectorAll('li.hecho').length).toBe(2);
    expect(el.querySelector('li.actual')?.textContent).toContain('En diagnóstico');
  });

  it('avisa cuando la orden está cancelada', async () => {
    const fixture = TestBed.createComponent(Traza);
    fixture.componentRef.setInput('estado', 'CANCELADO');
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Orden cancelada');
  });
});

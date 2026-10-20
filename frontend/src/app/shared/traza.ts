import { Component, computed, input } from '@angular/core';
import { EstadoOrden, FLUJO, etiquetaEstado } from '../core/models';

/** Línea de avance de la reparación, dibujada como una pista con pads de soldadura. */
@Component({
  selector: 'app-traza',
  template: `
    <ol class="pista" [class.cancelada]="estado() === 'CANCELADO'" aria-label="Avance de la reparación">
      @for (p of pasos(); track p.estado) {
        <li [class.hecho]="p.hecho" [class.actual]="p.actual">
          <span class="pad" aria-hidden="true"></span>
          <span class="nombre">{{ p.etiqueta }}</span>
          @if (p.actual) { <span class="solo-lectores"> (estado actual)</span> }
        </li>
      }
    </ol>
    @if (estado() === 'CANCELADO') {
      <p class="chip cancelado">Orden cancelada</p>
    }
  `,
  styles: `
    .pista { list-style: none; display: flex; margin: 0 0 .5rem; padding: 0; }
    li { flex: 1; position: relative; text-align: center; padding-top: 30px; font-size: .8rem; color: var(--tinta-suave); }
    li::before { content: ''; position: absolute; top: 11px; left: -50%; width: 100%; height: 4px; background: var(--linea); }
    li:first-child::before { display: none; }
    .pad { position: absolute; top: 3px; left: 50%; width: 20px; height: 20px; margin-left: -10px; border-radius: 50%; background: #fff; border: 4px solid var(--linea); z-index: 1; }
    li.hecho::before { background: var(--cobre); }
    li.hecho .pad { background: var(--cobre); border-color: var(--cobre); }
    li.actual .pad { border-color: var(--multimetro); background: var(--placa); box-shadow: 0 0 0 3px var(--placa); }
    li.actual .nombre { color: var(--tinta); font-weight: 700; }
    .cancelada li { opacity: .45; }
    @media (max-width: 640px) { li { font-size: .68rem; } }
  `,
})
export class Traza {
  readonly estado = input.required<EstadoOrden>();
  readonly pasos = computed(() => {
    const idx = FLUJO.indexOf(this.estado());
    return FLUJO.map((e, i) => ({
      estado: e, etiqueta: etiquetaEstado(e), hecho: i < idx, actual: i === idx,
    }));
  });
}

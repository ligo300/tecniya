import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-estrellas',
  template: `
    @if (cantidad() > 0) {
      <span class="valor" [attr.aria-label]="'Calificación ' + promedio() + ' de 5, ' + cantidad() + ' opiniones'">
        <span class="barra" aria-hidden="true"><span class="relleno" [style.width.%]="porcentaje()"></span></span>
        <strong>{{ promedio() }}</strong>
        <span class="suave">({{ cantidad() }})</span>
      </span>
    } @else {
      <span class="suave pequeno">Sin calificaciones aún</span>
    }
  `,
  styles: `
    .valor { display: inline-flex; align-items: center; gap: .4rem; }
    .barra { position: relative; display: inline-block; width: 70px; height: 8px; background: var(--linea); border-radius: 4px; overflow: hidden; }
    .relleno { position: absolute; inset: 0 auto 0 0; background: var(--multimetro); }
  `,
})
export class Estrellas {
  readonly valor = input<number | null>(0);
  readonly cantidad = input<number>(0);
  readonly promedio = computed(() => (this.valor() ?? 0).toFixed(1));
  readonly porcentaje = computed(() => Math.min(100, Math.max(0, ((this.valor() ?? 0) / 5) * 100)));
}

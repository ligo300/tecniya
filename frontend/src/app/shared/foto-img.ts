import { Component, DestroyRef, effect, inject, input, signal } from '@angular/core';
import { ApiService } from '../core/api.service';
import { Foto } from '../core/models';

/** Muestra una foto de la orden: la baja como blob porque la API pide el token. */
@Component({
  selector: 'app-foto-img',
  template: `
    @if (url()) {
      <a [href]="url()" target="_blank" rel="noopener" [attr.aria-label]="'Abrir ' + foto().nombreArchivo">
        <img [src]="url()" [alt]="'Foto del estado ' + foto().estado + ': ' + foto().nombreArchivo" />
      </a>
    } @else if (fallo()) {
      <span class="estado">No se pudo cargar la foto</span>
    } @else {
      <span class="estado">Cargando foto…</span>
    }
  `,
  styles: `
    :host { display: block; }
    img { width: 100%; height: 150px; object-fit: cover; border-radius: 6px; border: 1px solid var(--linea); display: block; }
    .estado { display: grid; place-items: center; height: 150px; background: var(--soldadura); border-radius: 6px; font-size: .85rem; color: var(--tinta-suave); text-align: center; padding: .5rem; }
  `,
})
export class FotoImg {
  private readonly api = inject(ApiService);
  readonly foto = input.required<Foto>();
  readonly url = signal<string | null>(null);
  readonly fallo = signal(false);

  constructor() {
    const limpiar = () => {
      const actual = this.url();
      if (actual) { URL.revokeObjectURL(actual); }
    };
    inject(DestroyRef).onDestroy(limpiar);

    effect((alLimpiar) => {
      const f = this.foto();
      this.fallo.set(false);
      const sub = this.api.descargarFoto(f).subscribe({
        next: (blob) => { limpiar(); this.url.set(URL.createObjectURL(blob)); },
        error: () => this.fallo.set(true),
      });
      alLimpiar(() => sub.unsubscribe());
    });
  }
}

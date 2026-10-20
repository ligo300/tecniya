import { CurrencyPipe } from '@angular/common';
import { Component, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { mensajeError } from '../../core/errores';
import { Tecnico } from '../../core/models';
import { Estrellas } from '../../shared/estrellas';

@Component({
  selector: 'app-tecnico-detalle',
  imports: [RouterLink, CurrencyPipe, Estrellas],
  template: `
    <div class="contenedor pagina">
      <a routerLink="/" class="volver">← Volver al catálogo</a>

      @if (error()) { <div class="aviso error" role="alert">{{ error() }}</div> }
      @if (tecnico(); as t) {
        <header class="cabecera">
          <h1>{{ t.nombres }} {{ t.apellidos }}</h1>
          <p class="suave">Atiende en {{ t.zona }}</p>
        </header>

        <dl class="datos">
          <div><dt>Equipos</dt><dd>@for (e of t.especialidades; track e) { <span class="chip">{{ e }}</span> }</dd></div>
          <div><dt>Calificación</dt><dd><app-estrellas [valor]="t.califPromedio" [cantidad]="t.califCantidad" /></dd></div>
          <div><dt>Tarifa base</dt><dd><strong class="precio">{{ t.tarifaBase | currency: 'PEN' : 'S/ ' }}</strong></dd></div>
        </dl>

        @if (!auth.autenticado()) {
          <p>Para contratar a {{ t.nombres }} necesitas una cuenta de cliente.</p>
          <a class="btn cobre" routerLink="/login" [queryParams]="{ volver: '/tecnicos/' + t.id }">Ingresar para contratar</a>
        } @else if (auth.tieneRol('CLIENTE')) {
          <p>Contrata a {{ t.nombres }} directo por su tarifa base, o publica una solicitud para comparar ofertas.</p>
          <div class="acciones">
            <a class="btn cobre" [routerLink]="['/contratar', t.id]">Contratar a {{ t.nombres }}</a>
            <a class="btn borde" routerLink="/solicitudes/nueva">Publicar una solicitud</a>
          </div>
        }
      }
    </div>
  `,
  styles: `
    .volver { display: inline-block; margin-bottom: 1.2rem; }
    .cabecera h1 { margin-bottom: .2rem; }
    .datos { display: grid; gap: 1rem; margin: 1.5rem 0; padding: 1.2rem; background: #fff; border: 1px solid var(--linea); border-left: 6px solid var(--cobre); border-radius: 6px; max-width: 560px; }
    .datos div { display: grid; grid-template-columns: 9rem 1fr; align-items: center; gap: .5rem; }
    dt { color: var(--tinta-suave); font-weight: 600; }
    dd { margin: 0; display: flex; flex-wrap: wrap; gap: .3rem; }
    .acciones { display: flex; flex-wrap: wrap; gap: .6rem; }
    .precio { font: 800 1.6rem var(--display); }
  `,
})
export class TecnicoPagina {
  private readonly api = inject(ApiService);
  readonly auth = inject(AuthService);
  readonly id = input.required<string>();
  readonly tecnico = signal<Tecnico | null>(null);
  readonly error = signal('');

  constructor() {
    effect(() => {
      const n = Number(this.id());
      this.error.set('');
      this.api.tecnico(n).subscribe({
        next: (t) => this.tecnico.set(t),
        error: (e) => { this.tecnico.set(null); this.error.set(mensajeError(e)); },
      });
    });
  }
}

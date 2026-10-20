import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { mensajeError } from '../../core/errores';
import { Solicitud, etiquetaSolicitud } from '../../core/models';

@Component({
  selector: 'app-solicitudes',
  imports: [RouterLink, DatePipe, CurrencyPipe],
  template: `
    <div class="contenedor pagina">
      <div class="encabezado">
        <div>
          <h1>Mis solicitudes</h1>
          <p class="suave">Publica qué le pasa a tu equipo y recibe ofertas de técnicos. Tú eliges.</p>
        </div>
        <a class="btn cobre" routerLink="/solicitudes/nueva">Nueva solicitud</a>
      </div>

      @if (error()) { <div class="aviso error" role="alert">{{ error() }}</div> }

      @if (cargando()) {
        <p class="suave">Cargando solicitudes…</p>
      } @else if (lista().length === 0 && !error()) {
        <div class="vacio">
          <h3>Aún no has publicado ninguna solicitud</h3>
          <p class="suave">Cuéntanos la falla de tu equipo y tu presupuesto. También puedes <a routerLink="/">elegir un técnico del catálogo</a> y contratarlo directo.</p>
        </div>
      } @else {
        <div class="tabla-caja">
          <table class="tabla">
            <thead><tr><th>Equipo</th><th>Falla</th><th>Presupuesto</th><th>Estado</th><th>Publicada</th><th><span class="solo-lectores">Abrir</span></th></tr></thead>
            <tbody>
              @for (s of lista(); track s.id) {
                <tr>
                  <td><strong>{{ s.tipoEquipo }}</strong><br /><span class="suave pequeno">{{ s.marcaModelo }}</span></td>
                  <td class="falla">{{ s.descripcionFalla }}</td>
                  <td>{{ s.presupuestoRef | currency: 'PEN' : 'S/ ' }}</td>
                  <td><span class="chip" [class.activo]="s.estado === 'ADJUDICADA'" [class.pendiente]="s.estado === 'CON_OFERTAS'" [class.cancelado]="s.estado === 'CANCELADA'">{{ etiqueta(s.estado) }}</span></td>
                  <td>{{ s.creadoEn | date: 'dd/MM/yyyy' }}</td>
                  <td><a class="btn chico" [routerLink]="['/solicitudes', s.id]">Ver</a></td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
  styles: `
    .encabezado { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: flex-start; gap: 1rem; margin-bottom: 1rem; }
    .falla { max-width: 28ch; }
  `,
})
export class Solicitudes {
  private readonly api = inject(ApiService);
  readonly lista = signal<Solicitud[]>([]);
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly etiqueta = etiquetaSolicitud;

  constructor() {
    this.api.solicitudes().subscribe({
      next: (l) => { this.lista.set(l); this.cargando.set(false); },
      error: (e) => { this.cargando.set(false); this.error.set(mensajeError(e)); },
    });
  }
}

import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { mensajeError } from '../../core/errores';
import { Orden, etiquetaEstado } from '../../core/models';

@Component({
  selector: 'app-mis-ordenes',
  imports: [RouterLink, DatePipe, CurrencyPipe],
  template: `
    <div class="contenedor pagina">
      <h1>Mis órdenes</h1>
      <p class="suave">Sigue cada reparación: el técnico sube una foto en cada etapa.</p>

      @if (error()) { <div class="aviso error" role="alert">{{ error() }}</div> }

      @if (cargando()) {
        <p class="suave">Cargando órdenes…</p>
      } @else if (lista().length === 0 && !error()) {
        <div class="vacio">
          <h3>Todavía no tienes órdenes</h3>
          <p class="suave">Acepta una oferta en <a routerLink="/solicitudes">tus solicitudes</a> o contrata a un técnico desde el <a routerLink="/">catálogo</a>.</p>
        </div>
      } @else {
        <div class="tabla-caja">
          <table class="tabla">
            <thead><tr><th>Código</th><th>Equipo</th><th>Precio</th><th>Estado</th><th>Creada</th><th><span class="solo-lectores">Abrir</span></th></tr></thead>
            <tbody>
              @for (o of lista(); track o.id) {
                <tr>
                  <td><strong>{{ o.codigo }}</strong></td>
                  <td>{{ o.tipoEquipo }} <span class="suave pequeno">· {{ o.descripcion }}</span></td>
                  <td>{{ o.precioAcordado | currency: 'PEN' : 'S/ ' }}</td>
                  <td><span class="chip" [class.activo]="o.estado === 'ENTREGADO'" [class.cancelado]="o.estado === 'CANCELADO'">{{ etiqueta(o.estado) }}</span></td>
                  <td>{{ o.creadoEn | date: 'dd/MM/yyyy HH:mm' }}</td>
                  <td><a class="btn chico" [routerLink]="['/ordenes', o.id]">Seguir</a></td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
})
export class MisOrdenes {
  private readonly api = inject(ApiService);
  readonly lista = signal<Orden[]>([]);
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly etiqueta = etiquetaEstado;

  constructor() {
    this.api.ordenes().subscribe({
      next: (l) => { this.lista.set(l); this.cargando.set(false); },
      error: (e) => { this.cargando.set(false); this.error.set(mensajeError(e)); },
    });
  }
}

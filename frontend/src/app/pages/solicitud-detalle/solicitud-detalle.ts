import { CurrencyPipe } from '@angular/common';
import { Component, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { mensajeError } from '../../core/errores';
import { Oferta, Solicitud, Tecnico, etiquetaSolicitud } from '../../core/models';
import { Estrellas } from '../../shared/estrellas';

@Component({
  selector: 'app-solicitud-detalle',
  imports: [RouterLink, CurrencyPipe, Estrellas],
  template: `
    <div class="contenedor pagina">
      <a routerLink="/solicitudes" class="volver">← Mis solicitudes</a>

      @if (error()) { <div class="aviso error" role="alert">{{ error() }}</div> }
      @if (ok()) { <div class="aviso ok" role="status">{{ ok() }}</div> }

      @if (solicitud(); as s) {
        <header>
          <h1>{{ s.tipoEquipo }} · {{ s.marcaModelo }}</h1>
          <p>{{ s.descripcionFalla }}</p>
          <p class="suave">
            Presupuesto de referencia {{ s.presupuestoRef | currency: 'PEN' : 'S/ ' }} ·
            <span class="chip" [class.activo]="s.estado === 'ADJUDICADA'" [class.pendiente]="s.estado === 'CON_OFERTAS'" [class.cancelado]="s.estado === 'CANCELADA'">{{ etiqueta(s.estado) }}</span>
          </p>
          @if (s.estado === 'ABIERTA' || s.estado === 'CON_OFERTAS') {
            <button class="btn peligro chico" type="button" [disabled]="trabajando()" (click)="cancelar(s)">Cancelar solicitud</button>
          }
        </header>

        <h2 style="margin-top:2rem">Ofertas recibidas</h2>
        @if (ofertas().length === 0) {
          <div class="vacio">
            <h3>Todavía no hay ofertas</h3>
            <p class="suave">Los técnicos de {{ s.tipoEquipo }} ya pueden verla. Vuelve en un rato.</p>
          </div>
        } @else {
          <p class="suave pequeno">Ordenadas de menor a mayor precio. Al aceptar una, las demás se rechazan y se crea tu orden.</p>
          <div class="tabla-caja">
            <table class="tabla">
              <thead><tr><th>Técnico</th><th>Precio</th><th>Plazo</th><th>Mensaje</th><th>Estado</th><th><span class="solo-lectores">Acción</span></th></tr></thead>
              <tbody>
                @for (o of ofertas(); track o.id) {
                  <tr>
                    <td>
                      <a [routerLink]="['/tecnicos', o.tecnicoId]">{{ nombre(o.tecnicoId) }}</a>
                      @if (tecnicos()[o.tecnicoId]; as t) { <br /><app-estrellas [valor]="t.califPromedio" [cantidad]="t.califCantidad" /> }
                    </td>
                    <td><strong>{{ o.precio | currency: 'PEN' : 'S/ ' }}</strong></td>
                    <td>{{ o.plazoDias }} {{ o.plazoDias === 1 ? 'día' : 'días' }}</td>
                    <td>{{ o.mensaje }}</td>
                    <td><span class="chip" [class.activo]="o.estado === 'ACEPTADA'" [class.cancelado]="o.estado === 'RECHAZADA'">{{ o.estado === 'PENDIENTE' ? 'Pendiente' : o.estado === 'ACEPTADA' ? 'Aceptada' : 'Rechazada' }}</span></td>
                    <td>
                      @if (o.estado === 'PENDIENTE' && (s.estado === 'ABIERTA' || s.estado === 'CON_OFERTAS')) {
                        <button class="btn cobre chico" type="button" [disabled]="trabajando()" (click)="aceptar(o)">Aceptar</button>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      } @else if (!error()) {
        <p class="suave">Cargando solicitud…</p>
      }
    </div>
  `,
  styles: `.volver { display: inline-block; margin-bottom: 1.2rem; }`,
})
export class SolicitudDetalle {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  readonly id = input.required<string>();
  readonly solicitud = signal<Solicitud | null>(null);
  readonly ofertas = signal<Oferta[]>([]);
  readonly tecnicos = signal<Record<number, Tecnico>>({});
  readonly error = signal('');
  readonly ok = signal('');
  readonly trabajando = signal(false);
  readonly etiqueta = etiquetaSolicitud;

  constructor() {
    effect(() => this.cargar(Number(this.id())));
  }

  private cargar(id: number): void {
    this.api.solicitud(id).subscribe({
      next: (s) => { this.solicitud.set(s); this.error.set(''); },
      error: (e) => this.error.set(mensajeError(e)),
    });
    this.api.ofertas(id).subscribe({
      next: (l) => { this.ofertas.set(l); this.cargarTecnicos(l); },
      error: (e) => this.error.set(mensajeError(e)),
    });
  }

  /** Para comparar ofertas se muestra el nombre y la calificación de cada técnico. */
  private cargarTecnicos(lista: Oferta[]): void {
    for (const id of new Set(lista.map((o) => o.tecnicoId))) {
      if (this.tecnicos()[id]) { continue; }
      this.api.tecnico(id).subscribe({
        next: (t) => this.tecnicos.update((m) => ({ ...m, [id]: t })),
        error: () => undefined, // si no se puede leer, queda "Técnico #id"
      });
    }
  }

  nombre(id: number): string {
    const t = this.tecnicos()[id];
    return t ? `${t.nombres} ${t.apellidos}` : `Técnico #${id}`;
  }

  aceptar(o: Oferta): void {
    if (!confirm(`¿Aceptar la oferta de S/ ${o.precio}? Se creará tu orden con este técnico.`)) { return; }
    this.trabajando.set(true);
    this.error.set('');
    this.api.aceptarOferta(o.id).subscribe({
      next: () => this.router.navigate(['/mis-ordenes']),
      error: (e) => { this.trabajando.set(false); this.error.set(mensajeError(e)); },
    });
  }

  cancelar(s: Solicitud): void {
    if (!confirm('¿Cancelar esta solicitud? Los técnicos dejarán de verla.')) { return; }
    this.trabajando.set(true);
    this.error.set('');
    this.api.cancelarSolicitud(s.id).subscribe({
      next: () => { this.trabajando.set(false); this.ok.set('Solicitud cancelada.'); this.cargar(s.id); },
      error: (e) => { this.trabajando.set(false); this.error.set(mensajeError(e)); },
    });
  }
}

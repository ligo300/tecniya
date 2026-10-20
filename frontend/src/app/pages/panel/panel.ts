import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { mensajeError } from '../../core/errores';
import { Orden, TecnicoDetalle, TecnicoRequest, etiquetaEstado } from '../../core/models';
import { Estrellas } from '../../shared/estrellas';
import { PerfilForm } from '../../shared/perfil-form';

@Component({
  selector: 'app-panel',
  imports: [RouterLink, DatePipe, CurrencyPipe, PerfilForm, Estrellas],
  template: `
    <div class="contenedor pagina">
      <h1>Mi panel</h1>

      @if (error()) { <div class="aviso error" role="alert">{{ error() }}</div> }
      @if (ok()) { <div class="aviso ok" role="status">{{ ok() }}</div> }

      @if (cargando()) {
        <p class="suave">Cargando tu perfil…</p>
      } @else if (!perfil()) {
        <section>
          <h2>Crea tu perfil de técnico</h2>
          <p class="suave">Los clientes verán tu nombre, zona, equipos, tarifa y calificación. Tu documento y teléfono quedan privados.</p>
          <app-perfil-form etiquetaBoton="Crear perfil" [enviando]="guardando()" (guardar)="crear($event)" />
        </section>
      } @else {
        @let p = perfil()!;
        <section class="resumen">
          <div>
            <h2>{{ p.nombres }} {{ p.apellidos }}</h2>
            <p class="suave">{{ p.zona }} · desde {{ p.tarifaBase | currency: 'PEN' : 'S/ ' }}</p>
            <app-estrellas [valor]="p.califPromedio" [cantidad]="p.califCantidad" />
          </div>
          <div>
            <span class="chip" [class.activo]="p.estado === 'ACTIVO'" [class.pendiente]="p.estado === 'PENDIENTE_VALIDACION'" [class.suspendido]="p.estado === 'SUSPENDIDO'">
              {{ etiqueta(p.estado) }}
            </span>
            <button class="btn borde chico" type="button" (click)="editando.set(!editando())">
              {{ editando() ? 'Cerrar edición' : 'Editar perfil' }}
            </button>
          </div>
        </section>

        @if (p.estado === 'PENDIENTE_VALIDACION') {
          <div class="aviso">Tu perfil espera la validación de un administrador. Hasta entonces no aparece en el catálogo.</div>
        } @else if (p.estado === 'SUSPENDIDO') {
          <div class="aviso error">Tu perfil está suspendido y no aparece en el catálogo. Contacta al administrador.</div>
        }

        @if (editando()) {
          <app-perfil-form [inicial]="p" etiquetaBoton="Guardar cambios" [enviando]="guardando()" (guardar)="actualizar(p.id, $event)" />
        }

        <h2 style="margin-top:2rem">Mis órdenes</h2>
        @if (ordenes().length === 0) {
          <div class="vacio">
            <h3>Todavía no tienes órdenes</h3>
            <p class="suave">Cuando un cliente te contrate, la orden aparecerá aquí para que subas las fotos de cada etapa.</p>
          </div>
        } @else {
          <div class="tabla-caja">
            <table class="tabla">
              <thead><tr><th>Código</th><th>Equipo</th><th>Estado</th><th>Creada</th><th><span class="solo-lectores">Abrir</span></th></tr></thead>
              <tbody>
                @for (o of ordenes(); track o.id) {
                  <tr>
                    <td><strong>{{ o.codigo }}</strong></td>
                    <td>{{ o.tipoEquipo }} <span class="suave pequeno">· {{ o.descripcion }}</span></td>
                    <td><span class="chip" [class.cancelado]="o.estado === 'CANCELADO'">{{ etiqueta(o.estado) }}</span></td>
                    <td>{{ o.creadoEn | date: 'dd/MM/yyyy HH:mm' }}</td>
                    <td><a class="btn chico" [routerLink]="['/ordenes', o.id]">Abrir</a></td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      }
    </div>
  `,
  styles: `
    .resumen { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 1rem; align-items: flex-start; padding: 1.2rem; margin-bottom: 1rem; background: #fff; border: 1px solid var(--linea); border-left: 6px solid var(--cobre); border-radius: 6px; }
    .resumen h2 { margin-bottom: .2rem; }
    .resumen > div:last-child { display: flex; gap: .6rem; align-items: center; flex-wrap: wrap; }
  `,
})
export class Panel {
  private readonly api = inject(ApiService);

  readonly perfil = signal<TecnicoDetalle | null>(null);
  readonly ordenes = signal<Orden[]>([]);
  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly editando = signal(false);
  readonly error = signal('');
  readonly ok = signal('');
  readonly etiqueta = etiquetaEstado;

  constructor() { this.cargar(); }

  private cargar(): void {
    this.cargando.set(true);
    this.api.miPerfil().subscribe({
      next: (p) => { this.perfil.set(p); this.cargando.set(false); this.cargarOrdenes(); },
      error: (e: HttpErrorResponse) => {
        this.cargando.set(false);
        // 404 = todavía no creó su perfil: se muestra el formulario, no un error.
        if (e.status !== 404) { this.error.set(mensajeError(e)); }
      },
    });
  }

  private cargarOrdenes(): void {
    this.api.ordenes().subscribe({
      next: (l) => this.ordenes.set(l),
      error: (e) => this.error.set(mensajeError(e)),
    });
  }

  crear(datos: TecnicoRequest): void {
    this.guardando.set(true);
    this.error.set('');
    this.api.crearPerfil(datos).subscribe({
      next: (p) => {
        this.guardando.set(false);
        this.perfil.set(p);
        this.ok.set('Perfil creado. Un administrador lo validará pronto.');
      },
      error: (e) => { this.guardando.set(false); this.error.set(mensajeError(e)); },
    });
  }

  actualizar(id: number, datos: TecnicoRequest): void {
    this.guardando.set(true);
    this.error.set('');
    this.ok.set('');
    this.api.actualizarPerfil(id, datos).subscribe({
      next: (p) => {
        this.guardando.set(false);
        this.perfil.set(p);
        this.editando.set(false);
        this.ok.set('Cambios guardados.');
      },
      error: (e) => { this.guardando.set(false); this.error.set(mensajeError(e)); },
    });
  }
}

import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { mensajeError } from '../../core/errores';
import { EstadoTecnico, TecnicoDetalle, etiquetaEstado } from '../../core/models';

@Component({
  selector: 'app-admin',
  imports: [FormsModule],
  template: `
    <div class="contenedor pagina">
      <h1>Validar técnicos</h1>
      <p class="suave">Un técnico aparece en el catálogo solo cuando está activo.</p>

      @if (error()) { <div class="aviso error" role="alert">{{ error() }}</div> }

      <div class="campo" style="max-width:260px;margin-bottom:1rem">
        <label for="filtro">Mostrar</label>
        <select id="filtro" [ngModel]="filtro()" (ngModelChange)="cambiarFiltro($event)">
          <option value="">Todos</option>
          <option value="PENDIENTE_VALIDACION">Pendientes de validación</option>
          <option value="ACTIVO">Activos</option>
          <option value="SUSPENDIDO">Suspendidos</option>
        </select>
      </div>

      @if (tecnicos().length === 0 && !cargando()) {
        <div class="vacio"><h3>No hay técnicos en esta lista</h3></div>
      } @else {
        <div class="tabla-caja">
          <table class="tabla">
            <thead><tr><th>Técnico</th><th>Documento</th><th>Zona</th><th>Equipos</th><th>Estado</th><th>Acciones</th></tr></thead>
            <tbody>
              @for (t of tecnicos(); track t.id) {
                <tr>
                  <td><strong>{{ t.nombres }} {{ t.apellidos }}</strong><br /><span class="suave pequeno">{{ t.telefono }}</span></td>
                  <td>{{ t.documento }}</td>
                  <td>{{ t.zona }}</td>
                  <td>{{ t.especialidades.join(', ') }}</td>
                  <td><span class="chip" [class.activo]="t.estado === 'ACTIVO'" [class.pendiente]="t.estado === 'PENDIENTE_VALIDACION'" [class.suspendido]="t.estado === 'SUSPENDIDO'">{{ etiqueta(t.estado) }}</span></td>
                  <td class="acciones">
                    @if (t.estado !== 'ACTIVO') {
                      <button class="btn chico" type="button" (click)="cambiar(t, 'ACTIVO')">Activar</button>
                    }
                    @if (t.estado !== 'SUSPENDIDO') {
                      <button class="btn peligro chico" type="button" (click)="cambiar(t, 'SUSPENDIDO')">Suspender</button>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
  styles: `.acciones { display: flex; gap: .4rem; flex-wrap: wrap; }`,
})
export class Admin {
  private readonly api = inject(ApiService);
  readonly tecnicos = signal<TecnicoDetalle[]>([]);
  readonly filtro = signal<EstadoTecnico | ''>('PENDIENTE_VALIDACION');
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly etiqueta = etiquetaEstado;

  constructor() { this.cargar(); }

  cambiarFiltro(v: EstadoTecnico | ''): void { this.filtro.set(v); this.cargar(); }

  private cargar(): void {
    this.cargando.set(true);
    this.api.gestionTecnicos(this.filtro() || undefined).subscribe({
      next: (l) => { this.tecnicos.set(l); this.cargando.set(false); this.error.set(''); },
      error: (e) => { this.cargando.set(false); this.error.set(mensajeError(e)); },
    });
  }

  cambiar(t: TecnicoDetalle, estado: EstadoTecnico): void {
    this.error.set('');
    this.api.cambiarEstadoTecnico(t.id, estado).subscribe({
      next: () => this.cargar(),
      error: (e) => this.error.set(mensajeError(e)),
    });
  }
}

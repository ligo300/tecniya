import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { mensajeError } from '../../core/errores';
import { NotificacionesService } from '../../core/notificaciones.service';
import { Notificacion } from '../../core/models';

@Component({
  selector: 'app-notificaciones',
  imports: [DatePipe],
  template: `
    <div class="contenedor pagina">
      <h1>Notificaciones</h1>

      @if (error()) { <div class="aviso error" role="alert">{{ error() }}</div> }

      @if (cargando()) {
        <p class="suave">Cargando…</p>
      } @else if (lista().length === 0 && !error()) {
        <div class="vacio"><h3>No tienes notificaciones</h3><p class="suave">Aquí aparecerán los avisos de tus solicitudes, órdenes y pagos.</p></div>
      }

      <ul class="lista">
        @for (n of lista(); track n.id) {
          <li [class.nueva]="!n.leida">
            <div>
              <p>{{ n.mensaje }}</p>
              <span class="suave pequeno">{{ n.creadaEn | date: 'dd/MM/yyyy HH:mm' }}</span>
            </div>
            @if (!n.leida) {
              <button class="btn borde chico" type="button" (click)="leer(n)">Marcar como leída</button>
            }
          </li>
        }
      </ul>
    </div>
  `,
  styles: `
    .lista { list-style: none; margin: 0; padding: 0; display: grid; gap: .6rem; }
    li { display: flex; justify-content: space-between; align-items: center; gap: 1rem; flex-wrap: wrap; padding: .9rem 1.1rem; background: #fff; border: 1px solid var(--linea); border-radius: 6px; }
    li.nueva { border-left: 6px solid var(--multimetro); }
    li p { margin: 0 0 .2rem; }
  `,
})
export class Notificaciones {
  private readonly api = inject(ApiService);
  private readonly contador = inject(NotificacionesService);

  readonly lista = signal<Notificacion[]>([]);
  readonly cargando = signal(true);
  readonly error = signal('');

  constructor() {
    this.api.notificaciones().subscribe({
      next: (l) => { this.lista.set(l); this.cargando.set(false); },
      error: (e) => { this.cargando.set(false); this.error.set(mensajeError(e)); },
    });
  }

  leer(n: Notificacion): void {
    this.api.marcarLeida(n.id).subscribe({
      next: () => {
        this.lista.update((l) => l.map((x) => (x.id === n.id ? { ...x, leida: true } : x)));
        this.contador.restar();
      },
      error: (e) => this.error.set(mensajeError(e)),
    });
  }
}

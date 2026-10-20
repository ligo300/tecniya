import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { mensajeError } from '../../core/errores';
import { Solicitud } from '../../core/models';

/** Técnico: solicitudes abiertas de sus especialidades, con formulario de oferta. */
@Component({
  selector: 'app-abiertas',
  imports: [ReactiveFormsModule, CurrencyPipe, DatePipe],
  template: `
    <div class="contenedor pagina">
      <h1>Solicitudes abiertas</h1>
      <p class="suave">Solo ves los equipos que atiendes. Envía tu precio y plazo; el cliente elige la oferta que prefiera.</p>

      @if (error()) { <div class="aviso error" role="alert">{{ error() }}</div> }
      @if (ok()) { <div class="aviso ok" role="status">{{ ok() }}</div> }

      @if (cargando()) {
        <p class="suave">Buscando solicitudes…</p>
      } @else if (lista().length === 0 && !error()) {
        <div class="vacio">
          <h3>No hay solicitudes abiertas para tus equipos</h3>
          <p class="suave">Si tu perfil aún no está activo, no verás solicitudes. Vuelve más tarde.</p>
        </div>
      }

      <ul class="lista">
        @for (s of lista(); track s.id) {
          <li class="solicitud">
            <div>
              <h2>{{ s.tipoEquipo }} · {{ s.marcaModelo }}</h2>
              <p>{{ s.descripcionFalla }}</p>
              <p class="suave pequeno">Presupuesto del cliente {{ s.presupuestoRef | currency: 'PEN' : 'S/ ' }} · publicada {{ s.creadoEn | date: 'dd/MM/yyyy HH:mm' }}</p>
            </div>
            @if (abierta() === s.id) {
              <form class="formulario ancho" [formGroup]="form" (ngSubmit)="enviar(s)" novalidate>
                <div class="campo">
                  <label for="precio-{{ s.id }}">Tu precio (S/)</label>
                  <input id="precio-{{ s.id }}" type="number" step="0.01" min="0.01" formControlName="precio" inputmode="decimal" />
                  @if (form.controls.precio.invalid && form.controls.precio.touched) { <span class="error-campo">El precio debe ser mayor a cero.</span> }
                </div>
                <div class="campo">
                  <label for="plazo-{{ s.id }}">Plazo (días)</label>
                  <input id="plazo-{{ s.id }}" type="number" min="1" max="365" formControlName="plazoDias" />
                  @if (form.controls.plazoDias.invalid && form.controls.plazoDias.touched) { <span class="error-campo">Entre 1 y 365 días.</span> }
                </div>
                <div class="campo completo">
                  <label for="msg-{{ s.id }}">Mensaje para el cliente</label>
                  <input id="msg-{{ s.id }}" formControlName="mensaje" maxlength="300" />
                </div>
                <div class="completo botones">
                  <button class="btn cobre" type="submit" [disabled]="enviando()">{{ enviando() ? 'Enviando…' : 'Enviar oferta' }}</button>
                  <button class="btn borde" type="button" (click)="abierta.set(null)">Cancelar</button>
                </div>
              </form>
            } @else {
              <button class="btn" type="button" (click)="ofertar(s)">Hacer una oferta</button>
            }
          </li>
        }
      </ul>
    </div>
  `,
  styles: `
    .lista { list-style: none; margin: 0; padding: 0; display: grid; gap: 1rem; }
    .solicitud { display: grid; gap: 1rem; padding: 1.2rem; background: #fff; border: 1px solid var(--linea); border-left: 6px solid var(--multimetro); border-radius: 6px; }
    .solicitud h2 { font-size: 1.2rem; margin-bottom: .3rem; }
    .botones { display: flex; gap: .6rem; flex-wrap: wrap; }
  `,
})
export class Abiertas {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  readonly lista = signal<Solicitud[]>([]);
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly ok = signal('');
  readonly abierta = signal<number | null>(null);
  readonly enviando = signal(false);

  readonly form = this.fb.nonNullable.group({
    precio: [0, [Validators.required, Validators.min(0.01)]],
    plazoDias: [3, [Validators.required, Validators.min(1), Validators.max(365)]],
    mensaje: ['', [Validators.maxLength(300)]],
  });

  constructor() {
    this.api.solicitudes().subscribe({
      next: (l) => { this.lista.set(l); this.cargando.set(false); },
      error: (e) => { this.cargando.set(false); this.error.set(mensajeError(e)); },
    });
  }

  ofertar(s: Solicitud): void {
    this.form.reset({ precio: s.presupuestoRef, plazoDias: 3, mensaje: '' });
    this.ok.set('');
    this.error.set('');
    this.abierta.set(s.id);
  }

  enviar(s: Solicitud): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) { return; }
    this.enviando.set(true);
    this.error.set('');
    this.api.ofertar(s.id, this.form.getRawValue()).subscribe({
      next: () => {
        this.enviando.set(false);
        this.abierta.set(null);
        this.ok.set(`Oferta enviada para ${s.tipoEquipo} · ${s.marcaModelo}.`);
      },
      error: (e) => { this.enviando.set(false); this.error.set(mensajeError(e)); },
    });
  }
}

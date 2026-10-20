import { CurrencyPipe } from '@angular/common';
import { Component, effect, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { mensajeError } from '../../core/errores';
import { Tecnico, TipoEquipo } from '../../core/models';

/** Contratación directa: el precio es la tarifa base del técnico. */
@Component({
  selector: 'app-contratar',
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe],
  template: `
    <div class="contenedor pagina">
      <a [routerLink]="['/tecnicos', id()]" class="volver">← Volver al perfil</a>

      @if (error()) { <div class="aviso error" role="alert">{{ error() }}</div> }

      @if (tecnico(); as t) {
        <h1>Contratar a {{ t.nombres }} {{ t.apellidos }}</h1>
        <p class="suave">Contratación directa por su tarifa base de <strong>{{ t.tarifaBase | currency: 'PEN' : 'S/ ' }}</strong>. Si prefieres comparar precios, <a routerLink="/solicitudes/nueva">publica una solicitud</a>.</p>

        <form class="formulario" [formGroup]="form" (ngSubmit)="contratar(t)" novalidate>
          <div class="campo">
            <label for="tipo">Equipo</label>
            <select id="tipo" formControlName="tipoEquipo">
              @for (e of t.especialidades; track e) { <option [value]="e">{{ e }}</option> }
            </select>
          </div>
          <div class="campo">
            <label for="desc">¿Qué le pasa a tu equipo?</label>
            <textarea id="desc" rows="4" formControlName="descripcion" maxlength="700"></textarea>
            @if (form.controls.descripcion.invalid && form.controls.descripcion.touched) {
              <span class="error-campo">Cuéntale al técnico qué falla tiene.</span>
            }
          </div>
          <button class="btn cobre" type="submit" [disabled]="enviando()">{{ enviando() ? 'Contratando…' : 'Contratar técnico' }}</button>
        </form>
      } @else if (!error()) {
        <p class="suave">Cargando técnico…</p>
      }
    </div>
  `,
  styles: `.volver { display: inline-block; margin-bottom: 1.2rem; }`,
})
export class Contratar {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  readonly id = input.required<string>();
  readonly tecnico = signal<Tecnico | null>(null);
  readonly error = signal('');
  readonly enviando = signal(false);

  readonly form = this.fb.nonNullable.group({
    tipoEquipo: ['PC' as TipoEquipo],
    descripcion: ['', [Validators.required, Validators.maxLength(700)]],
  });

  constructor() {
    effect(() => {
      this.api.tecnico(Number(this.id())).subscribe({
        next: (t) => {
          this.tecnico.set(t);
          this.form.controls.tipoEquipo.setValue(t.especialidades[0] ?? 'PC');
        },
        error: (e) => this.error.set(mensajeError(e)),
      });
    });
  }

  contratar(t: Tecnico): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) { return; }
    this.enviando.set(true);
    this.error.set('');
    const { tipoEquipo, descripcion } = this.form.getRawValue();
    this.api.contratarDirecto({ tecnicoId: t.id, tipoEquipo, descripcion }).subscribe({
      next: (o) => this.router.navigate(['/ordenes', o.id]),
      error: (e) => { this.enviando.set(false); this.error.set(mensajeError(e)); },
    });
  }
}

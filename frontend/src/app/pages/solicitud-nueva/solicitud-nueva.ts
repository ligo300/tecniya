import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { mensajeError } from '../../core/errores';
import { TIPOS_EQUIPO, TipoEquipo } from '../../core/models';

@Component({
  selector: 'app-solicitud-nueva',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="contenedor pagina">
      <a routerLink="/solicitudes" class="volver">← Mis solicitudes</a>
      <h1>Nueva solicitud</h1>
      <p class="suave">Los técnicos activos que reparan este tipo de equipo verán tu solicitud y te enviarán ofertas.</p>

      @if (error()) { <div class="aviso error" role="alert">{{ error() }}</div> }

      <form class="formulario" [formGroup]="form" (ngSubmit)="publicar()" novalidate>
        <div class="campo">
          <label for="tipo">Tipo de equipo</label>
          <select id="tipo" formControlName="tipoEquipo">
            @for (t of tipos; track t) { <option [value]="t">{{ t }}</option> }
          </select>
        </div>
        <div class="campo">
          <label for="marca">Marca y modelo</label>
          <input id="marca" formControlName="marcaModelo" placeholder="Ej. Samsung A54" maxlength="100" />
          @if (mal('marcaModelo')) { <span class="error-campo">Indica la marca o el modelo.</span> }
        </div>
        <div class="campo">
          <label for="falla">¿Qué le pasa?</label>
          <textarea id="falla" rows="4" formControlName="descripcionFalla" maxlength="500" placeholder="Ej. No carga y se apaga solo"></textarea>
          @if (mal('descripcionFalla')) { <span class="error-campo">Describe la falla.</span> }
        </div>
        <div class="campo">
          <label for="presupuesto">Presupuesto de referencia (S/)</label>
          <input id="presupuesto" type="number" step="0.01" min="0.01" formControlName="presupuestoRef" inputmode="decimal" />
          @if (mal('presupuestoRef')) { <span class="error-campo">El presupuesto debe ser mayor a cero.</span> }
        </div>
        <button class="btn cobre" type="submit" [disabled]="enviando()">{{ enviando() ? 'Publicando…' : 'Publicar solicitud' }}</button>
      </form>
    </div>
  `,
  styles: `.volver { display: inline-block; margin-bottom: 1.2rem; }`,
})
export class SolicitudNueva {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  readonly tipos = TIPOS_EQUIPO;
  readonly error = signal('');
  readonly enviando = signal(false);

  readonly form = this.fb.nonNullable.group({
    tipoEquipo: ['PC' as TipoEquipo],
    marcaModelo: ['', [Validators.required, Validators.maxLength(100)]],
    descripcionFalla: ['', [Validators.required, Validators.maxLength(500)]],
    presupuestoRef: [0, [Validators.required, Validators.min(0.01)]],
  });

  mal(c: 'marcaModelo' | 'descripcionFalla' | 'presupuestoRef'): boolean {
    const x = this.form.controls[c];
    return x.invalid && x.touched;
  }

  publicar(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) { return; }
    this.enviando.set(true);
    this.error.set('');
    this.api.crearSolicitud(this.form.getRawValue()).subscribe({
      next: (s) => this.router.navigate(['/solicitudes', s.id]),
      error: (e) => { this.enviando.set(false); this.error.set(mensajeError(e)); },
    });
  }
}

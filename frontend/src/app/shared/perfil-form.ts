import { Component, effect, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TIPOS_EQUIPO, TecnicoDetalle, TecnicoRequest, TipoEquipo } from '../core/models';
import { inject } from '@angular/core';

/** Formulario de perfil de técnico (crear y editar). */
@Component({
  selector: 'app-perfil-form',
  imports: [ReactiveFormsModule],
  template: `
    <form class="formulario ancho" [formGroup]="form" (ngSubmit)="enviar()" novalidate>
      <div class="campo">
        <label for="nombres">Nombres</label>
        <input id="nombres" formControlName="nombres" autocomplete="given-name" />
        @if (invalido('nombres')) { <span class="error-campo">Escribe tus nombres.</span> }
      </div>
      <div class="campo">
        <label for="apellidos">Apellidos</label>
        <input id="apellidos" formControlName="apellidos" autocomplete="family-name" />
        @if (invalido('apellidos')) { <span class="error-campo">Escribe tus apellidos.</span> }
      </div>
      <div class="campo">
        <label for="documento">DNI o documento</label>
        <input id="documento" formControlName="documento" inputmode="numeric" />
        @if (invalido('documento')) { <span class="error-campo">Debe tener entre 8 y 12 dígitos.</span> }
      </div>
      <div class="campo">
        <label for="telefono">Teléfono</label>
        <input id="telefono" formControlName="telefono" inputmode="tel" autocomplete="tel" />
        @if (invalido('telefono')) { <span class="error-campo">Escribe un teléfono de contacto.</span> }
      </div>
      <div class="campo">
        <label for="zona">Zona de atención</label>
        <input id="zona" formControlName="zona" placeholder="Ej. Lima Norte" />
        @if (invalido('zona')) { <span class="error-campo">Indica en qué zona atiendes.</span> }
      </div>
      <div class="campo">
        <label for="tarifa">Tarifa base (S/)</label>
        <input id="tarifa" type="number" step="0.01" min="0.01" formControlName="tarifaBase" inputmode="decimal" />
        @if (invalido('tarifaBase')) { <span class="error-campo">La tarifa debe ser mayor a cero.</span> }
      </div>
      <fieldset class="campo completo">
        <legend>Equipos que reparas</legend>
        <div class="opciones">
          @for (t of tipos; track t) {
            <label class="opcion">
              <input type="checkbox" [checked]="seleccion.includes(t)" (change)="alternar(t)" />
              {{ t }}
            </label>
          }
        </div>
        @if (enviado && seleccion.length === 0) { <span class="error-campo">Elige al menos un tipo de equipo.</span> }
      </fieldset>
      <div class="completo">
        <button class="btn cobre" type="submit" [disabled]="enviando()">
          {{ enviando() ? 'Guardando…' : etiquetaBoton() }}
        </button>
      </div>
    </form>
  `,
})
export class PerfilForm {
  private readonly fb = inject(FormBuilder);
  readonly inicial = input<TecnicoDetalle | null>(null);
  readonly enviando = input(false);
  readonly etiquetaBoton = input('Guardar perfil');
  readonly guardar = output<TecnicoRequest>();

  readonly tipos = TIPOS_EQUIPO;
  seleccion: TipoEquipo[] = [];
  enviado = false;

  readonly form = this.fb.nonNullable.group({
    nombres: ['', [Validators.required, Validators.maxLength(80)]],
    apellidos: ['', [Validators.required, Validators.maxLength(80)]],
    documento: ['', [Validators.required, Validators.pattern(/^\d{8,12}$/)]],
    telefono: ['', [Validators.required, Validators.maxLength(20)]],
    zona: ['', [Validators.required, Validators.maxLength(80)]],
    tarifaBase: [0, [Validators.required, Validators.min(0.01)]],
  });

  constructor() {
    effect(() => {
      const t = this.inicial();
      if (t) {
        this.form.patchValue({
          nombres: t.nombres, apellidos: t.apellidos, documento: t.documento,
          telefono: t.telefono, zona: t.zona, tarifaBase: t.tarifaBase,
        });
        this.seleccion = [...t.especialidades];
      }
    });
  }

  invalido(campo: string): boolean {
    const c = this.form.get(campo);
    return !!c && c.invalid && (c.touched || this.enviado);
  }

  alternar(t: TipoEquipo): void {
    this.seleccion = this.seleccion.includes(t)
      ? this.seleccion.filter((x) => x !== t)
      : [...this.seleccion, t];
  }

  enviar(): void {
    this.enviado = true;
    this.form.markAllAsTouched();
    if (this.form.invalid || this.seleccion.length === 0) { return; }
    this.guardar.emit({ ...this.form.getRawValue(), especialidades: this.seleccion });
  }
}

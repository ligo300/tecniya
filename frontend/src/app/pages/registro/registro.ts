import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { mensajeError } from '../../core/errores';

function coinciden(g: AbstractControl): ValidationErrors | null {
  return g.get('password')?.value === g.get('confirmar')?.value ? null : { noCoinciden: true };
}

@Component({
  selector: 'app-registro',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="contenedor pagina">
      <h1>Crear cuenta</h1>
      <p class="suave">Elige cómo vas a usar TecniYa. Los técnicos completan su perfil después de ingresar.</p>

      @if (error()) { <div class="aviso error" role="alert">{{ error() }}</div> }

      <form class="formulario" [formGroup]="form" (ngSubmit)="crear()" novalidate>
        <fieldset class="campo">
          <legend>Quiero</legend>
          <div class="opciones">
            <label class="opcion"><input type="radio" value="CLIENTE" formControlName="rol" /> Contratar un técnico</label>
            <label class="opcion"><input type="radio" value="TECNICO" formControlName="rol" /> Ofrecer mis servicios</label>
          </div>
        </fieldset>
        <div class="campo">
          <label for="email">Correo</label>
          <input id="email" type="email" formControlName="email" autocomplete="email" />
          @if (mal('email')) { <span class="error-campo">Escribe un correo válido.</span> }
        </div>
        <div class="campo">
          <label for="password">Contraseña (8 a 72 caracteres)</label>
          <input id="password" type="password" formControlName="password" autocomplete="new-password" />
          @if (mal('password')) { <span class="error-campo">Usa entre 8 y 72 caracteres.</span> }
        </div>
        <div class="campo">
          <label for="confirmar">Repite la contraseña</label>
          <input id="confirmar" type="password" formControlName="confirmar" autocomplete="new-password" />
          @if (form.touched && form.hasError('noCoinciden')) { <span class="error-campo">Las contraseñas no coinciden.</span> }
        </div>
        <button class="btn cobre" type="submit" [disabled]="enviando()">{{ enviando() ? 'Creando…' : 'Crear cuenta' }}</button>
      </form>
      <p class="suave" style="margin-top:1.2rem">¿Ya tienes cuenta? <a routerLink="/login">Ingresa</a>.</p>
    </div>
  `,
})
export class Registro {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly error = signal('');
  readonly enviando = signal(false);

  readonly form = this.fb.nonNullable.group({
    rol: ['CLIENTE' as 'CLIENTE' | 'TECNICO'],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    confirmar: ['', [Validators.required]],
  }, { validators: coinciden });

  mal(c: 'email' | 'password'): boolean {
    const x = this.form.controls[c];
    return x.invalid && x.touched;
  }

  crear(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) { return; }
    this.enviando.set(true);
    this.error.set('');
    const { email, password, rol } = this.form.getRawValue();
    this.auth.registrar(email, password, rol).subscribe({
      next: () => this.auth.login(email, password).subscribe({
        next: () => this.router.navigateByUrl(this.auth.rutaInicio()),
        error: () => this.router.navigate(['/login']),
      }),
      error: (e) => { this.enviando.set(false); this.error.set(mensajeError(e)); },
    });
  }
}

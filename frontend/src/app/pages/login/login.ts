import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { mensajeError } from '../../core/errores';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="contenedor pagina">
      <h1>Ingresar</h1>
      <p class="suave">Entra con tu correo y contraseña.</p>

      @if (error()) { <div class="aviso error" role="alert">{{ error() }}</div> }

      <form class="formulario" [formGroup]="form" (ngSubmit)="entrar()" novalidate>
        <div class="campo">
          <label for="email">Correo</label>
          <input id="email" type="email" formControlName="email" autocomplete="email" />
          @if (mal('email')) { <span class="error-campo">Escribe un correo válido.</span> }
        </div>
        <div class="campo">
          <label for="password">Contraseña</label>
          <input id="password" type="password" formControlName="password" autocomplete="current-password" />
          @if (mal('password')) { <span class="error-campo">Escribe tu contraseña.</span> }
        </div>
        <button class="btn" type="submit" [disabled]="enviando()">{{ enviando() ? 'Ingresando…' : 'Ingresar' }}</button>
      </form>
      <p class="suave" style="margin-top:1.2rem">¿Aún no tienes cuenta? <a routerLink="/registro">Crea una</a>.</p>
    </div>
  `,
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Llega del query param ?volver= gracias a withComponentInputBinding. */
  readonly volver = input<string>();
  readonly error = signal('');
  readonly enviando = signal(false);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  mal(c: 'email' | 'password'): boolean {
    const x = this.form.controls[c];
    return x.invalid && x.touched;
  }

  entrar(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) { return; }
    this.enviando.set(true);
    this.error.set('');
    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: () => {
        const destino = this.volver();
        this.router.navigateByUrl(destino && destino.startsWith('/') ? destino : this.auth.rutaInicio());
      },
      error: (e) => {
        this.enviando.set(false);
        this.error.set(mensajeError(e));
      },
    });
  }
}

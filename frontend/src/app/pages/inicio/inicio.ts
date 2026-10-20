import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { debounceTime, startWith, switchMap, tap, catchError, of } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { mensajeError } from '../../core/errores';
import { TIPOS_EQUIPO, Tecnico } from '../../core/models';
import { Estrellas } from '../../shared/estrellas';

@Component({
  selector: 'app-inicio',
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, Estrellas],
  template: `
    <section class="portada">
      <div class="contenedor">
        <h1>Repara con alguien que te muestra cada paso.</h1>
        <p>Elige un técnico de PC, celular o TV y sigue tu equipo con fotos desde que lo recibe hasta que te lo devuelve.</p>
      </div>
    </section>

    <div class="contenedor pagina">
      <form class="filtros" [formGroup]="filtros" role="search" aria-label="Filtrar técnicos">
        <div class="campo">
          <label for="tipo">Equipo</label>
          <select id="tipo" formControlName="tipo">
            <option value="">Todos</option>
            @for (t of tipos; track t) { <option [value]="t">{{ t }}</option> }
          </select>
        </div>
        <div class="campo">
          <label for="zona">Zona</label>
          <input id="zona" formControlName="zona" placeholder="Ej. Miraflores" />
        </div>
        <div class="campo">
          <label for="calif">Calificación mínima</label>
          <select id="calif" formControlName="calificacionMin">
            <option [ngValue]="null">Cualquiera</option>
            <option [ngValue]="3">3 o más</option>
            <option [ngValue]="4">4 o más</option>
            <option [ngValue]="4.5">4.5 o más</option>
          </select>
        </div>
      </form>

      @if (error()) { <div class="aviso error" role="alert">{{ error() }}</div> }

      <p class="suave pequeno" aria-live="polite">
        @if (cargando()) { Buscando técnicos… }
        @else { {{ tecnicos().length }} {{ tecnicos().length === 1 ? 'técnico disponible' : 'técnicos disponibles' }} }
      </p>

      <ul class="lista">
        @for (t of tecnicos(); track t.id) {
          <li class="ficha">
            <div class="ficha-nombre">
              <h2><a [routerLink]="['/tecnicos', t.id]">{{ t.nombres }} {{ t.apellidos }}</a></h2>
              <span class="suave">{{ t.zona }}</span>
            </div>
            <div class="ficha-equipos">
              @for (e of t.especialidades; track e) { <span class="chip">{{ e }}</span> }
            </div>
            <div class="ficha-calif"><app-estrellas [valor]="t.califPromedio" [cantidad]="t.califCantidad" /></div>
            <div class="ficha-precio">
              <span class="suave pequeno">Desde</span>
              <strong>{{ t.tarifaBase | currency: 'PEN' : 'S/ ' }}</strong>
            </div>
          </li>
        }
      </ul>

      @if (!cargando() && !error() && tecnicos().length === 0) {
        <div class="vacio">
          <h3>No hay técnicos con esos filtros</h3>
          <p class="suave">Prueba con otra zona o quita la calificación mínima.</p>
        </div>
      }
    </div>
  `,
  styles: `
    .portada { background: var(--placa); color: #fff; padding-block: 3rem 2.5rem; border-bottom: 6px solid var(--cobre); }
    .portada h1 { max-width: 18ch; }
    .portada p { color: #cfe3d8; font-size: 1.1rem; }
    .filtros { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 1rem; margin-bottom: 1rem; }
    .lista { list-style: none; margin: 0; padding: 0; display: grid; gap: .75rem; }
    .ficha { display: grid; grid-template-columns: 2fr 1.4fr 1.2fr auto; gap: 1rem; align-items: center; padding: 1rem 1.2rem; background: #fff; border: 1px solid var(--linea); border-left: 6px solid var(--cobre); border-radius: 6px; }
    .ficha h2 { margin: 0; font-size: 1.25rem; }
    .ficha h2 a { color: var(--tinta); text-decoration: none; }
    .ficha h2 a:hover { text-decoration: underline; }
    .ficha-equipos { display: flex; flex-wrap: wrap; gap: .3rem; }
    .ficha-precio { text-align: right; display: grid; }
    .ficha-precio strong { font: 800 1.3rem var(--display); }
    @media (max-width: 760px) {
      .filtros { grid-template-columns: 1fr; }
      .ficha { grid-template-columns: 1fr; gap: .5rem; }
      .ficha-precio { text-align: left; }
    }
  `,
})
export class Inicio {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  readonly tipos = TIPOS_EQUIPO;
  readonly tecnicos = signal<Tecnico[]>([]);
  readonly cargando = signal(true);
  readonly error = signal('');

  readonly filtros = this.fb.group({
    tipo: [''],
    zona: [''],
    calificacionMin: [null as number | null],
  });

  constructor() {
    this.filtros.valueChanges.pipe(
      startWith(this.filtros.value),
      debounceTime(250),
      tap(() => { this.cargando.set(true); this.error.set(''); }),
      switchMap((f) =>
        this.api.catalogo({
          tipo: (f.tipo ?? '') as never,
          zona: f.zona ?? '',
          calificacionMin: f.calificacionMin,
        }).pipe(
          catchError((e) => { this.error.set(mensajeError(e)); return of([] as Tecnico[]); }),
        ),
      ),
      takeUntilDestroyed(inject(DestroyRef)),
    ).subscribe((lista) => { this.tecnicos.set(lista); this.cargando.set(false); });
  }
}

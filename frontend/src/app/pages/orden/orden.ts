import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { mensajeError } from '../../core/errores';
import { EstadoOrden, FLUJO, MEDIOS_PAGO, MedioPago, OrdenDetalle, etiquetaEstado, siguienteEstado } from '../../core/models';
import { FotoImg } from '../../shared/foto-img';
import { Traza } from '../../shared/traza';

const MAX_BYTES = 5 * 1024 * 1024;

@Component({
  selector: 'app-orden',
  imports: [RouterLink, ReactiveFormsModule, DatePipe, CurrencyPipe, Traza, FotoImg],
  template: `
    <div class="contenedor pagina">
      <a [routerLink]="esTecnico() ? '/panel' : '/mis-ordenes'" class="volver">← Volver</a>

      @if (error()) { <div class="aviso error" role="alert">{{ error() }}</div> }
      @if (ok()) { <div class="aviso ok" role="status">{{ ok() }}</div> }

      @if (detalle(); as d) {
        <header>
          <h1>Orden {{ d.orden.codigo }}</h1>
          <p class="suave">{{ d.orden.tipoEquipo }} · {{ d.orden.descripcion }}
            @if (d.orden.precioAcordado) { · {{ d.orden.precioAcordado | currency: 'PEN' : 'S/ ' }} }
          </p>
        </header>

        <app-traza [estado]="d.orden.estado" />

        @if (esTecnico() && siguiente(); as sig) {
          <section class="accion">
            <h2>Siguiente: {{ etiqueta(sig) }}</h2>
            @if (sig === 'ENTREGADO') {
              @if (pagado()) {
                <p>El cliente ya pagó. Puedes entregar el equipo.</p>
              } @else {
                <p class="suave">Para entregar necesitas que el cliente registre el pago.</p>
              }
            } @else {
              <p class="suave">Sube al menos una foto del equipo en estado «{{ etiqueta(sig) }}» (JPG o PNG, máximo 5 MB) y luego avanza.</p>
              <div class="subida">
                <div class="campo">
                  <label for="archivo">Foto</label>
                  <input id="archivo" type="file" accept="image/jpeg,image/png" (change)="elegir($event)" />
                </div>
                <button class="btn borde" type="button" [disabled]="!archivo() || subiendo()" (click)="subir(sig)">
                  {{ subiendo() ? 'Subiendo…' : 'Subir foto' }}
                </button>
              </div>
            }
            <button class="btn cobre" type="button"
                    [disabled]="avanzando() || (sig === 'ENTREGADO' ? !pagado() : !hayFoto(sig))"
                    (click)="avanzar(sig)">
              {{ avanzando() ? 'Cambiando…' : 'Pasar a ' + etiqueta(sig) }}
            </button>
            @if (sig !== 'ENTREGADO' && !hayFoto(sig)) {
              <span class="suave pequeno"> Falta la foto de este estado.</span>
            }
          </section>
        }

        @if (esCliente()) {
          @if (d.orden.estado === 'LISTO' && !pagado()) {
            <section class="accion">
              <h2>Tu equipo está listo: paga para recibirlo</h2>
              <p class="suave">Pago simulado, no se cobra dinero real. Un número de operación que empiece con RECHAZO simula un pago rechazado.</p>
              <form class="formulario" [formGroup]="formPago" (ngSubmit)="pagar(d.orden.id)" novalidate>
                <div class="campo">
                  <label for="monto">Monto (S/)</label>
                  <input id="monto" type="number" step="0.01" min="0.01" formControlName="monto" inputmode="decimal" />
                  @if (formPago.controls.monto.invalid && formPago.controls.monto.touched) { <span class="error-campo">El monto debe ser mayor a cero.</span> }
                </div>
                <div class="campo">
                  <label for="medio">Medio de pago</label>
                  <select id="medio" formControlName="medio">
                    @for (m of medios; track m) { <option [value]="m">{{ m }}</option> }
                  </select>
                </div>
                <div class="campo">
                  <label for="nro">Número de operación</label>
                  <input id="nro" formControlName="nroOperacion" maxlength="40" />
                  @if (formPago.controls.nroOperacion.invalid && formPago.controls.nroOperacion.touched) { <span class="error-campo">Escribe el número de operación.</span> }
                </div>
                <button class="btn cobre" type="submit" [disabled]="avanzando()">{{ avanzando() ? 'Pagando…' : 'Pagar' }}</button>
              </form>
            </section>
          } @else if (d.orden.estado === 'LISTO' && pagado()) {
            <div class="aviso ok">Pago confirmado. El técnico ya puede entregarte el equipo.</div>
          }
          @if (d.orden.estado === 'ENTREGADO' && !d.calificacion) {
            <section class="accion">
              <h2>Califica al técnico</h2>
              <form class="formulario" [formGroup]="formCalif" (ngSubmit)="calificar(d.orden.id)" novalidate>
                <fieldset class="campo">
                  <legend>Puntaje</legend>
                  <div class="opciones">
                    @for (n of [1, 2, 3, 4, 5]; track n) {
                      <label class="opcion"><input type="radio" [value]="n" formControlName="puntaje" /> {{ n }}</label>
                    }
                  </div>
                </fieldset>
                <div class="campo">
                  <label for="comentario">Comentario (opcional)</label>
                  <textarea id="comentario" rows="3" formControlName="comentario" maxlength="300"></textarea>
                </div>
                <button class="btn cobre" type="submit" [disabled]="avanzando()">{{ avanzando() ? 'Enviando…' : 'Enviar calificación' }}</button>
              </form>
            </section>
          }
        }

        @if (puedeCancelar()) {
          <button class="btn peligro chico" type="button" [disabled]="avanzando()" (click)="cancelar()">Cancelar orden</button>
        }

        <h2 style="margin-top:2rem">Fotos</h2>
        @if (d.fotos.length === 0) {
          <div class="vacio"><h3>Aún no hay fotos</h3><p class="suave">Las fotos de cada etapa aparecerán aquí.</p></div>
        } @else {
          @for (g of fotosPorEstado(); track g.estado) {
            <h3>{{ etiqueta(g.estado) }}</h3>
            <div class="galeria">
              @for (f of g.fotos; track f.id) {
                <figure>
                  <app-foto-img [foto]="f" />
                  <figcaption class="pequeno suave">{{ f.fecha | date: 'dd/MM/yyyy HH:mm' }}</figcaption>
                </figure>
              }
            </div>
          }
        }

        <h2 style="margin-top:2rem">Historial</h2>
        <ol class="historial">
          @for (h of d.historial; track $index) {
            <li>
              <strong>{{ h.estadoAnterior ? etiqueta(h.estadoAnterior) + ' → ' : '' }}{{ etiqueta(h.estadoNuevo) }}</strong>
              <span class="suave pequeno"> · {{ h.fecha | date: 'dd/MM/yyyy HH:mm' }}</span>
            </li>
          }
        </ol>

        @if (d.pagos.length) {
          <h2 style="margin-top:2rem">Pago</h2>
          @for (p of d.pagos; track p.id) {
            <p>{{ p.monto | currency: 'PEN' : 'S/ ' }} por {{ p.medio }} · <span class="chip" [class.activo]="p.estado === 'PAGADO'">{{ p.estado }}</span></p>
          }
        }
        @if (d.calificacion; as c) {
          <h2 style="margin-top:2rem">Calificación del cliente</h2>
          <p><strong>{{ c.puntaje }} / 5</strong>@if (c.comentario) { · «{{ c.comentario }}» }</p>
        }
      } @else if (!error()) {
        <p class="suave">Cargando orden…</p>
      }
    </div>
  `,
  styles: `
    .volver { display: inline-block; margin-bottom: 1.2rem; }
    .accion { margin: 1.5rem 0; padding: 1.2rem; background: #fff; border: 1px solid var(--linea); border-left: 6px solid var(--multimetro); border-radius: 6px; }
    .subida { display: flex; flex-wrap: wrap; gap: 1rem; align-items: end; margin-bottom: 1rem; }
    .galeria { display: grid; grid-template-columns: repeat(auto-fill, minmax(170px, 1fr)); gap: .8rem; margin-bottom: 1rem; }
    figure { margin: 0; }
    .historial { padding-left: 1.2rem; }
  `,
})
export class OrdenPagina {
  private readonly api = inject(ApiService);
  readonly auth = inject(AuthService);

  readonly id = input.required<string>();
  readonly detalle = signal<OrdenDetalle | null>(null);
  readonly error = signal('');
  readonly ok = signal('');
  readonly archivo = signal<File | null>(null);
  readonly subiendo = signal(false);
  readonly avanzando = signal(false);
  readonly etiqueta = etiquetaEstado;

  readonly esTecnico = computed(() => this.auth.tieneRol('TECNICO'));
  readonly esCliente = computed(() => this.auth.tieneRol('CLIENTE'));
  readonly medios = MEDIOS_PAGO;
  private readonly fb = inject(FormBuilder);
  readonly formPago = this.fb.nonNullable.group({
    monto: [0, [Validators.required, Validators.min(0.01)]],
    medio: ['YAPE' as MedioPago],
    nroOperacion: ['', [Validators.required, Validators.maxLength(40)]],
  });
  readonly formCalif = this.fb.nonNullable.group({
    puntaje: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
    comentario: ['', [Validators.maxLength(300)]],
  });
  readonly siguiente = computed(() => {
    const e = this.detalle()?.orden.estado;
    return e ? siguienteEstado(e) : null;
  });
  readonly pagado = computed(() => !!this.detalle()?.pagos.some((p) => p.estado === 'PAGADO'));
  readonly puedeCancelar = computed(() => {
    const e = this.detalle()?.orden.estado;
    if (!e || FLUJO.indexOf(e) < 0) { return false; }
    if (this.esCliente()) { return e === 'CONTRATADO'; }
    return this.esTecnico() && FLUJO.indexOf(e) <= FLUJO.indexOf('EN_REPARACION');
  });
  readonly fotosPorEstado = computed(() => {
    const fotos = this.detalle()?.fotos ?? [];
    return FLUJO.map((e) => ({ estado: e, fotos: fotos.filter((f) => f.estado === e) }))
      .filter((g) => g.fotos.length > 0);
  });

  constructor() {
    effect(() => { this.cargar(Number(this.id())); });
  }

  hayFoto(estado: EstadoOrden): boolean {
    return !!this.detalle()?.fotos.some((f) => f.estado === estado);
  }

  private cargar(id: number): void {
    this.api.orden(id).subscribe({
      next: (d) => {
        this.detalle.set(d);
        this.error.set('');
        if (!this.formPago.controls.monto.dirty) {
          this.formPago.controls.monto.setValue(d.orden.precioAcordado ?? 0);
        }
      },
      error: (e) => this.error.set(mensajeError(e)),
    });
  }

  elegir(ev: Event): void {
    const f = (ev.target as HTMLInputElement).files?.[0] ?? null;
    this.error.set('');
    if (f && !['image/jpeg', 'image/png'].includes(f.type)) {
      this.error.set('La foto debe ser JPG o PNG.');
      this.archivo.set(null);
    } else if (f && f.size > MAX_BYTES) {
      this.error.set('La foto pesa más de 5 MB.');
      this.archivo.set(null);
    } else {
      this.archivo.set(f);
    }
  }

  subir(estado: EstadoOrden): void {
    const f = this.archivo();
    const d = this.detalle();
    if (!f || !d) { return; }
    this.subiendo.set(true);
    this.error.set('');
    this.ok.set('');
    this.api.subirFoto(d.orden.id, f, estado).subscribe({
      next: () => {
        this.subiendo.set(false);
        this.archivo.set(null);
        this.ok.set('Foto subida.');
        const entrada = document.getElementById('archivo') as HTMLInputElement | null;
        if (entrada) { entrada.value = ''; }
        this.cargar(d.orden.id);
      },
      error: (e) => { this.subiendo.set(false); this.error.set(mensajeError(e)); },
    });
  }

  avanzar(estado: EstadoOrden): void {
    const d = this.detalle();
    if (!d) { return; }
    this.avanzando.set(true);
    this.error.set('');
    this.ok.set('');
    this.api.cambiarEstadoOrden(d.orden.id, estado).subscribe({
      next: () => {
        this.avanzando.set(false);
        this.ok.set(`La orden pasó a ${etiquetaEstado(estado)}.`);
        this.cargar(d.orden.id);
      },
      error: (e) => { this.avanzando.set(false); this.error.set(mensajeError(e)); },
    });
  }

  pagar(id: number): void {
    this.formPago.markAllAsTouched();
    if (this.formPago.invalid) { return; }
    this.avanzando.set(true);
    this.error.set('');
    this.ok.set('');
    this.api.pagar(id, this.formPago.getRawValue()).subscribe({
      next: (p) => {
        this.avanzando.set(false);
        if (p.estado === 'PAGADO') {
          this.ok.set('Pago confirmado.');
        } else {
          this.error.set('El pago fue rechazado. Revisa el número de operación e inténtalo de nuevo.');
        }
        this.formPago.controls.nroOperacion.reset('');
        this.cargar(id);
      },
      error: (e) => { this.avanzando.set(false); this.error.set(mensajeError(e)); },
    });
  }

  calificar(id: number): void {
    if (this.formCalif.invalid) { return; }
    this.avanzando.set(true);
    this.error.set('');
    this.ok.set('');
    this.api.calificar(id, this.formCalif.getRawValue()).subscribe({
      next: () => { this.avanzando.set(false); this.ok.set('Gracias por calificar.'); this.cargar(id); },
      error: (e) => { this.avanzando.set(false); this.error.set(mensajeError(e)); },
    });
  }

  cancelar(): void {
    const d = this.detalle();
    if (!d || !confirm('¿Cancelar esta orden? No se puede deshacer.')) { return; }
    this.avanzando.set(true);
    this.error.set('');
    this.api.cancelarOrden(d.orden.id).subscribe({
      next: () => { this.avanzando.set(false); this.ok.set('Orden cancelada.'); this.cargar(d.orden.id); },
      error: (e) => { this.avanzando.set(false); this.error.set(mensajeError(e)); },
    });
  }
}

import { Injectable, computed, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { AuthService } from './auth.service';

/** Mantiene el contador de notificaciones sin leer que muestra la barra superior. */
@Injectable({ providedIn: 'root' })
export class NotificacionesService {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);

  private readonly _noLeidas = signal(0);
  readonly noLeidas = computed(() => (this.auth.autenticado() ? this._noLeidas() : 0));

  refrescar(): void {
    if (!this.auth.autenticado()) { this._noLeidas.set(0); return; }
    this.api.notificaciones(true).subscribe({
      next: (l) => this._noLeidas.set(l.length),
      error: () => undefined, // el contador es un extra: si falla, no molesta
    });
  }

  restar(): void { this._noLeidas.update((n) => Math.max(0, n - 1)); }
}

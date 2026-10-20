import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { AuthResponse, Rol, Usuario } from './models';

const CLAVE = 'tecniya.sesion';

interface Sesion { token: string; usuario: Usuario; }

/** Guarda el JWT y el usuario; el resto de la app solo lee las señales. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly sesion = signal<Sesion | null>(this.leer());

  readonly usuario = computed(() => this.sesion()?.usuario ?? null);
  readonly token = computed(() => this.sesion()?.token ?? null);
  readonly autenticado = computed(() => this.sesion() !== null);

  tieneRol(...roles: Rol[]): boolean {
    const u = this.usuario();
    return !!u && roles.includes(u.rol);
  }

  registrar(email: string, password: string, rol: 'CLIENTE' | 'TECNICO') {
    return this.http.post<Usuario>('/api/auth/registro', { email, password, rol });
  }

  login(email: string, password: string) {
    return this.http.post<AuthResponse>('/api/auth/login', { email, password }).pipe(
      tap((r) => {
        const s: Sesion = { token: r.token, usuario: r.usuario };
        this.sesion.set(s);
        this.guardar(s);
      }),
    );
  }

  salir(): void {
    this.sesion.set(null);
    try { localStorage.removeItem(CLAVE); } catch { /* sin almacenamiento */ }
    this.router.navigate(['/']);
  }

  /** Pantalla de inicio según el rol. */
  rutaInicio(): string {
    switch (this.usuario()?.rol) {
      case 'TECNICO': return '/panel';
      case 'ADMIN': return '/admin';
      default: return '/solicitudes';
    }
  }

  private leer(): Sesion | null {
    try {
      const txt = localStorage.getItem(CLAVE);
      if (!txt) { return null; }
      const s = JSON.parse(txt) as Sesion;
      return s?.token && s?.usuario && !this.vencido(s.token) ? s : null;
    } catch {
      return null;
    }
  }

  private guardar(s: Sesion): void {
    try { localStorage.setItem(CLAVE, JSON.stringify(s)); } catch { /* sin almacenamiento */ }
  }

  private vencido(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
      return typeof payload.exp === 'number' && payload.exp * 1000 < Date.now();
    } catch {
      return true;
    }
  }
}

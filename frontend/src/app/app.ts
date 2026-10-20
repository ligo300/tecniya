import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from './core/auth.service';
import { NotificacionesService } from './core/notificaciones.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <a class="saltar" href="#contenido">Saltar al contenido</a>
    <header class="barra">
      <div class="contenedor barra-interior">
        <a routerLink="/" class="logo">Tecni<span>Ya</span></a>
        <nav aria-label="Principal">
          <a routerLink="/" routerLinkActive="activo" [routerLinkActiveOptions]="{ exact: true }">Técnicos</a>
          @if (auth.tieneRol('CLIENTE')) {
            <a routerLink="/solicitudes" routerLinkActive="activo">Mis solicitudes</a>
            <a routerLink="/mis-ordenes" routerLinkActive="activo">Mis órdenes</a>
          }
          @if (auth.tieneRol('TECNICO')) {
            <a routerLink="/panel" routerLinkActive="activo">Mi panel</a>
            <a routerLink="/abiertas" routerLinkActive="activo">Solicitudes abiertas</a>
          }
          @if (auth.tieneRol('ADMIN')) { <a routerLink="/admin" routerLinkActive="activo">Validar técnicos</a> }
          @if (auth.usuario(); as u) {
            <a routerLink="/notificaciones" routerLinkActive="activo" class="campana">
              Avisos
              @if (avisos.noLeidas() > 0) {
                <span class="contador">{{ avisos.noLeidas() }}<span class="solo-lectores"> sin leer</span></span>
              }
            </a>
            <span class="usuario">{{ u.email }}</span>
            <button type="button" class="salir" (click)="auth.salir()">Salir</button>
          } @else {
            <a routerLink="/login" routerLinkActive="activo">Ingresar</a>
            <a routerLink="/registro" class="btn cobre chico">Crear cuenta</a>
          }
        </nav>
      </div>
    </header>
    <main id="contenido"><router-outlet /></main>
    <footer class="pie"><div class="contenedor">TecniYa · Proyecto Desarrollo de Aplicaciones Web II · Cibertec</div></footer>
  `,
  styles: `
    .saltar { position: absolute; left: -999px; top: 0; background: var(--multimetro); color: var(--tinta); padding: .5rem 1rem; z-index: 10; }
    .saltar:focus { left: 0; }
    .barra { background: var(--placa); color: #fff; }
    .barra-interior { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: .5rem 1.5rem; min-height: 64px; }
    .logo { font: 800 1.5rem var(--display); color: #fff; text-decoration: none; }
    .logo span { color: var(--multimetro); }
    nav { display: flex; align-items: center; flex-wrap: wrap; gap: .3rem 1.2rem; }
    nav a:not(.btn) { color: #dcebe3; text-decoration: none; padding: .4rem 0; border-bottom: 3px solid transparent; }
    nav a.activo { color: #fff; border-bottom-color: var(--multimetro); }
    nav a:not(.btn):hover { color: #fff; }
    .contador { display: inline-block; min-width: 1.4rem; padding: 0 .4rem; margin-left: .25rem; border-radius: 999px; background: var(--multimetro); color: var(--tinta); font-size: .8rem; font-weight: 700; text-align: center; }
    .usuario { color: #cfe3d8; font-size: .9rem; }
    .salir { background: transparent; border: 2px solid #5f8f7c; color: #fff; border-radius: 6px; padding: .3rem .8rem; font: 600 .9rem var(--texto); cursor: pointer; min-height: 36px; }
    .salir:hover { background: var(--placa-2); }
    main { min-height: 70vh; }
    .pie { border-top: 1px solid var(--linea); padding: 1.2rem 0; color: var(--tinta-suave); font-size: .85rem; }
  `,
})
export class App {
  readonly auth = inject(AuthService);
  readonly avisos = inject(NotificacionesService);

  constructor() {
    // El contador de avisos se actualiza en cada cambio de pantalla.
    inject(Router).events.pipe(
      filter((e) => e instanceof NavigationEnd),
      takeUntilDestroyed(inject(DestroyRef)),
    ).subscribe(() => this.avisos.refrescar());
  }
}

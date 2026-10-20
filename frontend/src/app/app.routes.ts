import { Routes } from '@angular/router';
import { autenticadoGuard, rolGuard } from './core/guards';
import { Admin } from './pages/admin/admin';
import { Inicio } from './pages/inicio/inicio';
import { Login } from './pages/login/login';
import { OrdenPagina } from './pages/orden/orden';
import { Panel } from './pages/panel/panel';
import { Registro } from './pages/registro/registro';
import { TecnicoPagina } from './pages/tecnico-detalle/tecnico-detalle';

export const routes: Routes = [
  { path: '', component: Inicio, title: 'TecniYa · Técnicos para tus equipos' },
  { path: 'tecnicos/:id', component: TecnicoPagina, title: 'Perfil del técnico · TecniYa' },
  { path: 'login', component: Login, title: 'Ingresar · TecniYa' },
  { path: 'registro', component: Registro, title: 'Crear cuenta · TecniYa' },
  { path: 'panel', component: Panel, canActivate: [rolGuard('TECNICO')], title: 'Mi panel · TecniYa' },
  { path: 'ordenes/:id', component: OrdenPagina, canActivate: [autenticadoGuard], title: 'Orden · TecniYa' },
  { path: 'admin', component: Admin, canActivate: [rolGuard('ADMIN')], title: 'Validar técnicos · TecniYa' },
  { path: '**', redirectTo: '' },
];

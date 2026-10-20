import { Routes } from '@angular/router';
import { autenticadoGuard, rolGuard } from './core/guards';
import { Abiertas } from './pages/abiertas/abiertas';
import { Admin } from './pages/admin/admin';
import { Contratar } from './pages/contratar/contratar';
import { MisOrdenes } from './pages/mis-ordenes/mis-ordenes';
import { Notificaciones } from './pages/notificaciones/notificaciones';
import { SolicitudDetalle } from './pages/solicitud-detalle/solicitud-detalle';
import { SolicitudNueva } from './pages/solicitud-nueva/solicitud-nueva';
import { Solicitudes } from './pages/solicitudes/solicitudes';
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
  { path: 'solicitudes', component: Solicitudes, canActivate: [rolGuard('CLIENTE')], title: 'Mis solicitudes · TecniYa' },
  { path: 'solicitudes/nueva', component: SolicitudNueva, canActivate: [rolGuard('CLIENTE')], title: 'Nueva solicitud · TecniYa' },
  { path: 'solicitudes/:id', component: SolicitudDetalle, canActivate: [rolGuard('CLIENTE')], title: 'Solicitud · TecniYa' },
  { path: 'mis-ordenes', component: MisOrdenes, canActivate: [rolGuard('CLIENTE')], title: 'Mis órdenes · TecniYa' },
  { path: 'contratar/:id', component: Contratar, canActivate: [rolGuard('CLIENTE')], title: 'Contratar técnico · TecniYa' },
  { path: 'abiertas', component: Abiertas, canActivate: [rolGuard('TECNICO')], title: 'Solicitudes abiertas · TecniYa' },
  { path: 'notificaciones', component: Notificaciones, canActivate: [autenticadoGuard], title: 'Notificaciones · TecniYa' },
  { path: 'admin', component: Admin, canActivate: [rolGuard('ADMIN')], title: 'Validar técnicos · TecniYa' },
  { path: '**', redirectTo: '' },
];

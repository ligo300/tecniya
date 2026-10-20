import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import {
  Calificacion, EstadoOrden, EstadoTecnico, MedioPago, Notificacion, Oferta, OfertaRequest,
  Orden, OrdenDetalle, Foto, Pago, Solicitud, SolicitudRequest, Tecnico, TecnicoDetalle,
  TecnicoRequest, TipoEquipo,
} from './models';

export interface FiltroCatalogo {
  tipo?: TipoEquipo | '';
  zona?: string;
  calificacionMin?: number | null;
}

/** Todas las llamadas REST pasan por el Gateway (proxy /api en desarrollo). */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  // ---- técnicos ----
  catalogo(f: FiltroCatalogo) {
    let params = new HttpParams();
    if (f.tipo) { params = params.set('tipo', f.tipo); }
    if (f.zona?.trim()) { params = params.set('zona', f.zona.trim()); }
    if (f.calificacionMin) { params = params.set('calificacionMin', f.calificacionMin); }
    return this.http.get<Tecnico[]>('/api/tecnicos', { params });
  }
  tecnico(id: number) { return this.http.get<Tecnico>(`/api/tecnicos/${id}`); }
  miPerfil() { return this.http.get<TecnicoDetalle>('/api/tecnicos/mio'); }
  crearPerfil(d: TecnicoRequest) { return this.http.post<TecnicoDetalle>('/api/tecnicos', d); }
  actualizarPerfil(id: number, d: TecnicoRequest) {
    return this.http.put<TecnicoDetalle>(`/api/tecnicos/${id}`, d);
  }
  gestionTecnicos(estado?: EstadoTecnico) {
    const params = estado ? new HttpParams().set('estado', estado) : undefined;
    return this.http.get<TecnicoDetalle[]>('/api/tecnicos/gestion', { params });
  }
  cambiarEstadoTecnico(id: number, estado: EstadoTecnico) {
    return this.http.put<TecnicoDetalle>(`/api/tecnicos/${id}/estado`, { estado });
  }

  // ---- órdenes ----
  ordenes() { return this.http.get<Orden[]>('/api/ordenes'); }
  orden(id: number) { return this.http.get<OrdenDetalle>(`/api/ordenes/${id}`); }
  cambiarEstadoOrden(id: number, estado: EstadoOrden) {
    return this.http.put<Orden>(`/api/ordenes/${id}/estado`, { estado });
  }
  cancelarOrden(id: number) { return this.http.put<Orden>(`/api/ordenes/${id}/cancelar`, {}); }
  subirFoto(id: number, archivo: File, estado: EstadoOrden) {
    const datos = new FormData();
    datos.append('archivo', archivo);
    datos.append('estado', estado);
    return this.http.post<Foto>(`/api/ordenes/${id}/fotos`, datos);
  }
  /** La foto exige el Authorization, por eso se pide como blob y no con <img src>. */
  descargarFoto(f: Pick<Foto, 'ordenId' | 'id'>) {
    return this.http.get(`/api/ordenes/${f.ordenId}/fotos/${f.id}`, { responseType: 'blob' });
  }

  contratarDirecto(d: { tecnicoId: number; tipoEquipo: TipoEquipo; descripcion: string }) {
    return this.http.post<Orden>('/api/ordenes/directa', d);
  }
  pagar(id: number, d: { monto: number; medio: MedioPago; nroOperacion: string }) {
    return this.http.post<Pago>(`/api/ordenes/${id}/pago`, d);
  }
  calificar(id: number, d: { puntaje: number; comentario: string }) {
    return this.http.post<Calificacion>(`/api/ordenes/${id}/calificacion`, d);
  }

  // ---- solicitudes y ofertas ----
  solicitudes() { return this.http.get<Solicitud[]>('/api/solicitudes'); }
  solicitud(id: number) { return this.http.get<Solicitud>(`/api/solicitudes/${id}`); }
  crearSolicitud(d: SolicitudRequest) { return this.http.post<Solicitud>('/api/solicitudes', d); }
  cancelarSolicitud(id: number) { return this.http.delete<void>(`/api/solicitudes/${id}`); }
  ofertas(solicitudId: number) { return this.http.get<Oferta[]>(`/api/solicitudes/${solicitudId}/ofertas`); }
  ofertar(solicitudId: number, d: OfertaRequest) {
    return this.http.post<Oferta>(`/api/solicitudes/${solicitudId}/ofertas`, d);
  }
  aceptarOferta(id: number) { return this.http.post<Oferta>(`/api/ofertas/${id}/aceptar`, {}); }

  // ---- notificaciones ----
  notificaciones(soloNoLeidas = false) {
    const params = new HttpParams().set('soloNoLeidas', soloNoLeidas);
    return this.http.get<Notificacion[]>('/api/notificaciones', { params });
  }
  marcarLeida(id: number) { return this.http.put<Notificacion>(`/api/notificaciones/${id}/leida`, {}); }
}

export type Rol = 'CLIENTE' | 'TECNICO' | 'ADMIN';
export type TipoEquipo = 'PC' | 'CELULAR' | 'TV' | 'OTRO';
export type EstadoTecnico = 'PENDIENTE_VALIDACION' | 'ACTIVO' | 'SUSPENDIDO';
export type EstadoOrden =
  | 'CONTRATADO' | 'RECIBIDO' | 'EN_DIAGNOSTICO' | 'EN_REPARACION'
  | 'LISTO' | 'ENTREGADO' | 'CANCELADO';

export const TIPOS_EQUIPO: TipoEquipo[] = ['PC', 'CELULAR', 'TV', 'OTRO'];

/** Orden del flujo normal (sin CANCELADO). */
export const FLUJO: EstadoOrden[] = [
  'CONTRATADO', 'RECIBIDO', 'EN_DIAGNOSTICO', 'EN_REPARACION', 'LISTO', 'ENTREGADO',
];

export interface Usuario { id: number; email: string; rol: Rol; activo: boolean; }

export interface AuthResponse {
  token: string;
  tipo: string;
  expiraEnMinutos: number;
  usuario: Usuario;
}

export interface Tecnico {
  id: number;
  nombres: string;
  apellidos: string;
  zona: string;
  tarifaBase: number;
  estado: EstadoTecnico;
  califPromedio: number;
  califCantidad: number;
  especialidades: TipoEquipo[];
}

export interface TecnicoDetalle extends Tecnico {
  usuarioId: number;
  documento: string;
  telefono: string;
}

export interface TecnicoRequest {
  nombres: string;
  apellidos: string;
  documento: string;
  telefono: string;
  zona: string;
  tarifaBase: number;
  especialidades: TipoEquipo[];
}

export interface Orden {
  id: number;
  codigo: string;
  clienteId: number;
  tecnicoId: number;
  solicitudId: number | null;
  ofertaId: number | null;
  origen: string;
  tipoEquipo: TipoEquipo;
  descripcion: string;
  precioAcordado: number | null;
  estado: EstadoOrden;
  creadoEn: string;
  actualizadoEn: string;
}

export interface Historial {
  estadoAnterior: EstadoOrden | null;
  estadoNuevo: EstadoOrden;
  usuarioId: number;
  fecha: string;
}

export interface Foto {
  id: number;
  ordenId: number;
  estado: EstadoOrden;
  nombreArchivo: string;
  tamanoBytes: number;
  subidoPor: number;
  fecha: string;
  rutaDescarga: string;
}

export interface Pago {
  id: number;
  ordenId: number;
  monto: number;
  medio: string;
  nroOperacion: string;
  estado: 'PENDIENTE' | 'PAGADO' | 'RECHAZADO';
  fecha: string;
}

export interface Calificacion {
  id: number;
  ordenId: number;
  puntaje: number;
  comentario: string;
  fecha: string;
}

export interface OrdenDetalle {
  orden: Orden;
  historial: Historial[];
  fotos: Foto[];
  pagos: Pago[];
  calificacion: Calificacion | null;
}

/** Siguiente estado del flujo, o null si la orden ya terminó. */
export function siguienteEstado(actual: EstadoOrden): EstadoOrden | null {
  const i = FLUJO.indexOf(actual);
  return i >= 0 && i < FLUJO.length - 1 ? FLUJO[i + 1] : null;
}

const ETIQUETAS: Record<string, string> = {
  CONTRATADO: 'Contratado',
  RECIBIDO: 'Recibido',
  EN_DIAGNOSTICO: 'En diagnóstico',
  EN_REPARACION: 'En reparación',
  LISTO: 'Listo',
  ENTREGADO: 'Entregado',
  CANCELADO: 'Cancelado',
  PENDIENTE_VALIDACION: 'Pendiente de validación',
  ACTIVO: 'Activo',
  SUSPENDIDO: 'Suspendido',
};

export function etiquetaEstado(e: EstadoOrden | EstadoTecnico): string {
  return ETIQUETAS[e] ?? e;
}

// ---- solicitudes y ofertas (paso 13) ----
export type EstadoSolicitud = 'ABIERTA' | 'CON_OFERTAS' | 'ADJUDICADA' | 'CANCELADA';
export type EstadoOferta = 'PENDIENTE' | 'ACEPTADA' | 'RECHAZADA';
export type MedioPago = 'EFECTIVO' | 'YAPE' | 'PLIN' | 'TARJETA';

export const MEDIOS_PAGO: MedioPago[] = ['YAPE', 'PLIN', 'TARJETA', 'EFECTIVO'];

export interface Solicitud {
  id: number;
  clienteId: number;
  tipoEquipo: TipoEquipo;
  marcaModelo: string;
  descripcionFalla: string;
  presupuestoRef: number;
  estado: EstadoSolicitud;
  creadoEn: string;
}

export interface SolicitudRequest {
  tipoEquipo: TipoEquipo;
  marcaModelo: string;
  descripcionFalla: string;
  presupuestoRef: number;
}

export interface Oferta {
  id: number;
  solicitudId: number;
  tecnicoId: number;
  precio: number;
  plazoDias: number;
  mensaje: string | null;
  estado: EstadoOferta;
  creadoEn: string;
}

export interface OfertaRequest {
  precio: number;
  plazoDias: number;
  mensaje: string;
}

export interface Notificacion {
  id: number;
  evento: string;
  mensaje: string;
  leida: boolean;
  creadaEn: string;
}

const ETIQUETAS_SOLICITUD: Record<EstadoSolicitud, string> = {
  ABIERTA: 'Abierta',
  CON_OFERTAS: 'Con ofertas',
  ADJUDICADA: 'Adjudicada',
  CANCELADA: 'Cancelada',
};

export function etiquetaSolicitud(e: EstadoSolicitud): string {
  return ETIQUETAS_SOLICITUD[e] ?? e;
}

import { HttpErrorResponse } from '@angular/common/http';

/** Convierte cualquier error HTTP en un mensaje legible para la pantalla. */
export function mensajeError(e: unknown): string {
  if (e instanceof HttpErrorResponse) {
    if (e.status === 0) {
      return 'No hay conexión con el servidor. Revisa que el Gateway esté levantado.';
    }
    const cuerpo = e.error as { mensaje?: string; detalles?: string[] } | null;
    if (cuerpo?.detalles?.length) { return cuerpo.detalles.join(' · '); }
    if (cuerpo?.mensaje) { return cuerpo.mensaje; }
    if (e.status === 413) { return 'La foto pesa más de 5 MB.'; }
    if (e.status === 403) { return 'No tienes permiso para hacer esto.'; }
    if (e.status === 404) { return 'No se encontró lo que buscabas.'; }
  }
  return 'Ocurrió un error inesperado. Inténtalo de nuevo.';
}

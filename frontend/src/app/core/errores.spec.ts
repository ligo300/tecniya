import { HttpErrorResponse } from '@angular/common/http';
import { mensajeError } from './errores';

describe('mensajeError', () => {
  it('usa el mensaje que envía la API', () => {
    const e = new HttpErrorResponse({ status: 409, error: { mensaje: 'El correo ya está registrado', detalles: [] } });
    expect(mensajeError(e)).toBe('El correo ya está registrado');
  });

  it('une los detalles de validación', () => {
    const e = new HttpErrorResponse({ status: 400, error: { mensaje: 'Datos inválidos', detalles: ['email: inválido', 'password: muy corta'] } });
    expect(mensajeError(e)).toBe('email: inválido · password: muy corta');
  });

  it('explica cuando no hay conexión', () => {
    expect(mensajeError(new HttpErrorResponse({ status: 0 }))).toContain('No hay conexión');
  });

  it('cubre 413 y errores desconocidos', () => {
    expect(mensajeError(new HttpErrorResponse({ status: 413 }))).toContain('5 MB');
    expect(mensajeError(new Error('x'))).toContain('inesperado');
  });
});

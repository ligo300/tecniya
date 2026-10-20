import { etiquetaEstado, siguienteEstado } from './models';

describe('models', () => {
  it('sigue el flujo CONTRATADO → ENTREGADO', () => {
    expect(siguienteEstado('CONTRATADO')).toBe('RECIBIDO');
    expect(siguienteEstado('EN_REPARACION')).toBe('LISTO');
    expect(siguienteEstado('LISTO')).toBe('ENTREGADO');
  });

  it('no hay siguiente después de ENTREGADO ni en CANCELADO', () => {
    expect(siguienteEstado('ENTREGADO')).toBeNull();
    expect(siguienteEstado('CANCELADO')).toBeNull();
  });

  it('da formato legible a los estados', () => {
    expect(etiquetaEstado('EN_DIAGNOSTICO')).toBe('En diagnóstico');
    expect(etiquetaEstado('PENDIENTE_VALIDACION')).toBe('Pendiente de validación');
  });
});

package pe.tecniya.ordenes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import pe.tecniya.ordenes.domain.EstadoOrden;

/** RN-11: el flujo normal avanza de a un estado; ENTREGADO y CANCELADO son finales. */
class OrdenFlujoTests {

    @Test
    void cadaEstadoTieneUnSoloSiguiente() {
        assertEquals(EstadoOrden.RECIBIDO, OrdenService.siguiente(EstadoOrden.CONTRATADO));
        assertEquals(EstadoOrden.EN_DIAGNOSTICO, OrdenService.siguiente(EstadoOrden.RECIBIDO));
        assertEquals(EstadoOrden.EN_REPARACION, OrdenService.siguiente(EstadoOrden.EN_DIAGNOSTICO));
        assertEquals(EstadoOrden.LISTO, OrdenService.siguiente(EstadoOrden.EN_REPARACION));
        assertEquals(EstadoOrden.ENTREGADO, OrdenService.siguiente(EstadoOrden.LISTO));
    }

    @Test
    void losEstadosFinalesNoTienenSiguiente() {
        assertNull(OrdenService.siguiente(EstadoOrden.ENTREGADO));
        assertNull(OrdenService.siguiente(EstadoOrden.CANCELADO));
    }
}

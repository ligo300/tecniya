package pe.tecniya.ordenes.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import pe.tecniya.ordenes.domain.EstadoPago;
import pe.tecniya.ordenes.domain.MedioPago;
import pe.tecniya.ordenes.domain.Pago;

public record PagoResponse(
        Long id,
        Long ordenId,
        BigDecimal monto,
        MedioPago medio,
        String nroOperacion,
        EstadoPago estado,
        LocalDateTime fecha) {

    public static PagoResponse de(Pago p) {
        return new PagoResponse(p.getId(), p.getOrdenId(), p.getMonto(), p.getMedio(),
                p.getNroOperacion(), p.getEstado(), p.getFecha());
    }
}

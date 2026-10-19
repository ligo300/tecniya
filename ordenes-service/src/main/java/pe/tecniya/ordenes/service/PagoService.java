package pe.tecniya.ordenes.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.tecniya.ordenes.domain.EstadoOrden;
import pe.tecniya.ordenes.domain.EstadoPago;
import pe.tecniya.ordenes.domain.Pago;
import pe.tecniya.ordenes.dto.PagoRequest;
import pe.tecniya.ordenes.dto.PagoResponse;
import pe.tecniya.ordenes.eventos.EventoPublisher;
import pe.tecniya.ordenes.exception.ConflictoException;
import pe.tecniya.ordenes.exception.ReglaNegocioException;
import pe.tecniya.ordenes.repository.PagoRepository;
import pe.tecniya.ordenes.security.JwtUser;
import reactor.core.publisher.Mono;

/** Pago simulado (RN-17..19): no hay pasarela real; el número de operación decide el resultado. */
@Service
public class PagoService {

    private final PagoRepository pagos;
    private final AccesoOrden acceso;
    private final EventoPublisher eventos;

    public PagoService(PagoRepository pagos, AccesoOrden acceso, EventoPublisher eventos) {
        this.pagos = pagos;
        this.acceso = acceso;
        this.eventos = eventos;
    }

    @Transactional
    public Mono<PagoResponse> pagar(Long ordenId, PagoRequest req, JwtUser usuario) {
        return acceso.comoCliente(ordenId, usuario).flatMap(o -> {
            if (o.getEstado() != EstadoOrden.LISTO) {
                return Mono.<Pago>error(new ReglaNegocioException(
                        "Solo se puede pagar cuando el equipo está LISTO"));
            }
            if (req.monto().compareTo(o.getPrecioAcordado()) > 0) {
                return Mono.<Pago>error(new ReglaNegocioException(
                        "El monto no puede superar el precio acordado ("
                                + o.getPrecioAcordado() + ")"));
            }
            return pagos.existsByOrdenIdAndEstado(ordenId, EstadoPago.PAGADO)
                    .flatMap(yaPagada -> {
                        if (yaPagada) {
                            return Mono.<Pago>error(new ConflictoException(
                                    "La orden ya tiene un pago confirmado"));
                        }
                        return pagos.existsByNroOperacion(req.nroOperacion().trim())
                                .flatMap(repetido -> {
                                    if (repetido) {
                                        return Mono.<Pago>error(new ConflictoException(
                                                "Ese número de operación ya fue usado"));
                                    }
                                    Pago p = new Pago();
                                    p.setOrdenId(ordenId);
                                    p.setMonto(req.monto());
                                    p.setMedio(req.medio());
                                    p.setNroOperacion(req.nroOperacion().trim());
                                    p.setEstado(p.getNroOperacion().toUpperCase()
                                            .startsWith("RECHAZO")
                                            ? EstadoPago.RECHAZADO : EstadoPago.PAGADO);
                                    p.setFecha(LocalDateTime.now());
                                    return pagos.save(p).flatMap(g -> eventos
                                            .publicar("pago.registrado", "PagoRegistrado", Map.of(
                                                    "ordenId", o.getId(),
                                                    "codigo", o.getCodigo(),
                                                    "clienteId", o.getClienteId(),
                                                    "tecnicoId", o.getTecnicoId(),
                                                    "monto", g.getMonto(),
                                                    "medio", g.getMedio().name(),
                                                    "estado", g.getEstado().name()))
                                            .thenReturn(g));
                                });
                    });
        }).map(PagoResponse::de);
    }
}

package pe.tecniya.auditoria.exception;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

import pe.tecniya.auditoria.dto.ApiError;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiError> validacion(WebExchangeBindException ex) {
        List<String> detalles = ex.getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "Datos inválidos", detalles);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ApiError> entradaInvalida(ServerWebInputException ex) {
        return responder(HttpStatus.BAD_REQUEST,
                "La petición no es válida", List.of());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ApiError> regla(ReglaNegocioException ex) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ApiError> accesoDenegado(AccesoDenegadoException ex) {
        return responder(HttpStatus.FORBIDDEN, ex.getMessage(), List.of());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> noEncontrado(RecursoNoEncontradoException ex) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<ApiError> servicioNoDisponible(ServicioNoDisponibleException ex) {
        return responder(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), List.of());
    }

    @ExceptionHandler({ConflictoException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ApiError> conflicto(Exception ex) {
        String mensaje = ex instanceof ConflictoException
                ? ex.getMessage() : "Ya existe un registro con esos datos";
        return responder(HttpStatus.CONFLICT, mensaje, List.of());
    }

    private ResponseEntity<ApiError> responder(HttpStatus estado, String mensaje,
                                               List<String> detalles) {
        ApiError error = new ApiError(LocalDateTime.now(), estado.value(),
                mensaje, detalles);
        return ResponseEntity.status(estado).body(error);
    }
}

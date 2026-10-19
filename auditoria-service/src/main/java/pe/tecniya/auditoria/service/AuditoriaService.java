package pe.tecniya.auditoria.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pe.tecniya.auditoria.domain.Auditoria;
import pe.tecniya.auditoria.dto.AuditoriaResponse;
import pe.tecniya.auditoria.eventos.EventoTecniya;
import pe.tecniya.auditoria.repository.AuditoriaRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AuditoriaService {

    private static final int MAX_DATOS = 2000;
    private static final int MAX_RESULTADOS = 200;

    private final AuditoriaRepository auditorias;
    private final ObjectMapper json;

    public AuditoriaService(AuditoriaRepository auditorias, ObjectMapper json) {
        this.auditorias = auditorias;
        this.json = json;
    }

    /** Guarda el evento tal como llegó, sin interpretarlo (solo se agrega, nunca se modifica). */
    public Mono<Void> registrar(EventoTecniya evento) {
        String tipo = evento.tipo() == null ? "Desconocido" : evento.tipo();
        Map<String, Object> datos = evento.datos() == null ? Map.of() : evento.datos();
        String entidad = entidadDe(tipo);

        Auditoria a = new Auditoria();
        a.setTipo(tipo);
        a.setEntidad(entidad);
        a.setEntidadId(idDe(entidad, datos));
        a.setDatos(texto(datos));
        a.setOcurridoEn(fecha(evento.fecha()));
        a.setRegistradoEn(LocalDateTime.now());
        return auditorias.save(a).then();
    }

    public Flux<AuditoriaResponse> listar(String tipo) {
        Flux<Auditoria> origen = tipo == null || tipo.isBlank()
                ? auditorias.findAllByOrderByIdDesc()
                : auditorias.findByTipoOrderByIdDesc(tipo.trim());
        return origen.take(MAX_RESULTADOS).map(AuditoriaResponse::de);
    }

    // ---------- Apoyo ----------

    private String entidadDe(String tipo) {
        if (tipo.startsWith("Tecnico")) {
            return "TECNICO";
        }
        if (tipo.startsWith("Oferta")) {
            return "SOLICITUD";
        }
        if (tipo.startsWith("Orden") || tipo.startsWith("Pago")) {
            return "ORDEN";
        }
        return "OTRO";
    }

    private Long idDe(String entidad, Map<String, Object> datos) {
        String clave = switch (entidad) {
            case "TECNICO" -> "tecnicoId";
            case "SOLICITUD" -> "solicitudId";
            case "ORDEN" -> "ordenId";
            default -> null;
        };
        Object valor = clave == null ? null : datos.get(clave);
        return valor instanceof Number n ? n.longValue() : null;
    }

    private String texto(Map<String, Object> datos) {
        String s;
        try {
            s = json.writeValueAsString(datos);
        } catch (JsonProcessingException e) {
            s = "{}";
        }
        return s.length() > MAX_DATOS ? s.substring(0, MAX_DATOS) : s;
    }

    private LocalDateTime fecha(String texto) {
        try {
            return LocalDateTime.parse(texto);
        } catch (DateTimeParseException | NullPointerException e) {
            return LocalDateTime.now();
        }
    }
}

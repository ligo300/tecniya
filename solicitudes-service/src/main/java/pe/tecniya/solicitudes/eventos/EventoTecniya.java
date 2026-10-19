package pe.tecniya.solicitudes.eventos;

import java.util.Map;

/** Sobre común de todos los eventos: qué pasó, cuándo y los datos. */
public record EventoTecniya(String tipo, String fecha, Map<String, Object> datos) {
}

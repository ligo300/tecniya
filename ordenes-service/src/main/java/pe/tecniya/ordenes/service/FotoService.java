package pe.tecniya.ordenes.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;

import pe.tecniya.ordenes.domain.EstadoOrden;
import pe.tecniya.ordenes.domain.Orden;
import pe.tecniya.ordenes.domain.OrdenFoto;
import pe.tecniya.ordenes.dto.FotoResponse;
import pe.tecniya.ordenes.exception.RecursoNoEncontradoException;
import pe.tecniya.ordenes.exception.ReglaNegocioException;
import pe.tecniya.ordenes.repository.FotoRepository;
import pe.tecniya.ordenes.security.JwtUser;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Evidencia fotográfica (RN-12/13): JPG o PNG de hasta 5 MB, ligada al estado de destino. */
@Service
public class FotoService {

    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final List<EstadoOrden> CON_FOTO = List.of(EstadoOrden.RECIBIDO,
            EstadoOrden.EN_DIAGNOSTICO, EstadoOrden.EN_REPARACION, EstadoOrden.LISTO);

    /** Contenido de una foto ya leído del disco. */
    public record Archivo(byte[] bytes, String tipo) {
    }

    private final FotoRepository fotos;
    private final AccesoOrden acceso;
    private final Path carpeta;

    public FotoService(FotoRepository fotos, AccesoOrden acceso,
                       @Value("${app.uploads.dir}") String carpeta) {
        this.fotos = fotos;
        this.acceso = acceso;
        this.carpeta = Paths.get(carpeta).toAbsolutePath().normalize();
    }

    public Mono<FotoResponse> subir(Long ordenId, FilePart archivo, String estadoTexto,
                                    JwtUser usuario, String autorizacion) {
        return Mono.fromCallable(() -> interpretar(estadoTexto)).flatMap(estado ->
                acceso.comoTecnicoAsignado(ordenId, autorizacion)
                .flatMap(o -> validarEstado(o, estado))
                .flatMap(o -> leer(archivo).flatMap(bytes -> {
                    String extension = extension(bytes);
                    if (extension == null) {
                        return Mono.<OrdenFoto>error(new ReglaNegocioException(
                                "Solo se aceptan imágenes JPG o PNG"));
                    }
                    String ruta = "ordenes/" + ordenId + "/" + estado + "/"
                            + UUID.randomUUID() + "." + extension;
                    return guardarEnDisco(ruta, bytes).then(Mono.defer(() -> {
                        OrdenFoto f = new OrdenFoto();
                        f.setOrdenId(ordenId);
                        f.setEstado(estado);
                        f.setUrl(ruta);
                        f.setNombreArchivo(nombreSeguro(archivo.filename()));
                        f.setTamanoBytes(bytes.length);
                        f.setSubidoPor(usuario.id());
                        f.setFecha(LocalDateTime.now());
                        return fotos.save(f);
                    }));
                })))
                .map(FotoResponse::de);
    }

    public Mono<Archivo> descargar(Long ordenId, Long fotoId, JwtUser usuario,
                                   String autorizacion) {
        return acceso.comoParticipante(ordenId, usuario, autorizacion)
                .then(fotos.findById(fotoId))
                .filter(f -> f.getOrdenId().equals(ordenId))
                .switchIfEmpty(Mono.error(
                        new RecursoNoEncontradoException("Foto no encontrada")))
                .flatMap(f -> Mono.fromCallable(() -> new Archivo(
                                Files.readAllBytes(carpeta.resolve(f.getUrl())),
                                f.getUrl().endsWith(".png") ? "image/png" : "image/jpeg"))
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorMap(IOException.class, e -> new RecursoNoEncontradoException(
                                "El archivo de la foto ya no está disponible")));
    }

    // ---------- Apoyo ----------

    private EstadoOrden interpretar(String texto) {
        try {
            EstadoOrden e = EstadoOrden.valueOf(texto.trim().toUpperCase());
            if (CON_FOTO.contains(e)) {
                return e;
            }
        } catch (IllegalArgumentException | NullPointerException ignorada) {
            // se informa abajo
        }
        throw new ReglaNegocioException(
                "El estado debe ser RECIBIDO, EN_DIAGNOSTICO, EN_REPARACION o LISTO");
    }

    private Mono<Orden> validarEstado(Orden o, EstadoOrden estado) {
        if (estado != OrdenService.siguiente(o.getEstado())) {
            return Mono.error(new ReglaNegocioException(
                    "La foto debe corresponder al siguiente estado (" + o.getEstado()
                            + " → " + estado + " no es válido)"));
        }
        return Mono.just(o);
    }

    /** Lee todo el archivo en memoria; si pasa de 5 MB lanza DataBufferLimitException (413). */
    private Mono<byte[]> leer(FilePart archivo) {
        return DataBufferUtils.join(archivo.content(), MAX_BYTES).map(buffer -> {
            try {
                byte[] bytes = new byte[buffer.readableByteCount()];
                buffer.read(bytes);
                return bytes;
            } finally {
                DataBufferUtils.release(buffer);
            }
        }).switchIfEmpty(Mono.error(new ReglaNegocioException("El archivo está vacío")));
    }

    /** Se confía en los primeros bytes del archivo, no en la extensión que declara el cliente. */
    private String extension(byte[] b) {
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 0x50 && b[2] == 0x4E
                && b[3] == 0x47 && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A
                && b[7] == 0x0A) {
            return "png";
        }
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8
                && (b[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        return null;
    }

    private Mono<Void> guardarEnDisco(String ruta, byte[] bytes) {
        return Mono.fromCallable(() -> {
            Path destino = carpeta.resolve(ruta).normalize();
            if (!destino.startsWith(carpeta)) {
                throw new IOException("Ruta no permitida");
            }
            Files.createDirectories(destino.getParent());
            Files.write(destino, bytes);
            return destino;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private String nombreSeguro(String original) {
        String limpio = original == null ? "foto" : original.replaceAll("[^A-Za-z0-9._-]", "_");
        return limpio.length() > 120 ? limpio.substring(limpio.length() - 120) : limpio;
    }
}

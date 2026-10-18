package pe.tecniya.tecnicos.repository;

import java.util.Collection;
import java.util.LinkedHashSet;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import pe.tecniya.tecnicos.domain.TipoEquipo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * La tabla tecnico_especialidad tiene llave compuesta (tecnico_id, tipo_equipo),
 * que Spring Data R2DBC no maneja con un repositorio; se usa DatabaseClient.
 */
@Repository
public class EspecialidadRepository {

    private final DatabaseClient db;

    public EspecialidadRepository(DatabaseClient db) {
        this.db = db;
    }

    public Flux<TipoEquipo> listar(Long tecnicoId) {
        return db.sql("SELECT tipo_equipo FROM tecnico_especialidad "
                        + "WHERE tecnico_id = :id ORDER BY tipo_equipo")
                .bind("id", tecnicoId)
                .map((fila, meta) -> TipoEquipo.valueOf(fila.get("tipo_equipo", String.class)))
                .all();
    }

    public Mono<Void> reemplazar(Long tecnicoId, Collection<TipoEquipo> tipos) {
        Mono<Void> borrar = db.sql("DELETE FROM tecnico_especialidad WHERE tecnico_id = :id")
                .bind("id", tecnicoId)
                .then();
        Flux<Void> insertar = Flux.fromIterable(new LinkedHashSet<>(tipos))
                .concatMap(tipo -> db.sql("INSERT INTO tecnico_especialidad "
                                + "(tecnico_id, tipo_equipo) VALUES (:id, :tipo)")
                        .bind("id", tecnicoId)
                        .bind("tipo", tipo.name())
                        .then());
        return borrar.thenMany(insertar).then();
    }
}

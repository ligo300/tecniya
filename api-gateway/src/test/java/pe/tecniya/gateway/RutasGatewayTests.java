package pe.tecniya.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

/** Cada servicio de negocio debe tener su ruta en el Gateway, apuntando a su nombre en Eureka. */
@SpringBootTest(properties = "eureka.client.enabled=false")
class RutasGatewayTests {

    @Autowired
    private RouteLocator rutas;

    @Test
    void declaraUnaRutaPorServicioHaciaSuNombreEnEureka() {
        Map<String, String> destinos = rutas.getRoutes().collectList().block().stream()
                .collect(Collectors.toMap(Route::getId, r -> r.getUri().toString()));

        for (String servicio : new String[] {"auth-service", "tecnicos-service",
                "solicitudes-service", "ordenes-service", "notificaciones-service",
                "auditoria-service"}) {
            assertEquals("lb://" + servicio, destinos.get(servicio),
                    "falta la ruta de " + servicio);
        }
    }
}

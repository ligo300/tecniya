# TecniYa

Plataforma web tipo inDrive para técnicos de equipos (PC, celulares, televisores y otros). El visitante explora el catálogo de técnicos; el cliente publica solicitudes con su presupuesto o contrata directamente; el técnico sube **fotos en cada etapa** y el cliente sigue la **trazabilidad** de su equipo desde la web.

Proyecto del curso **Desarrollo de Aplicaciones Web II (4697) — IES Cibertec**.

> Documentación de diseño (reglas de negocio, diagnóstico SEPTE, arquitectura y modelo de datos): [`docs/TecniYa_Plan_de_Proyecto_Pasos_1-3.docx`](docs/TecniYa_Plan_de_Proyecto_Pasos_1-3.docx)

## Arquitectura

| Módulo | Puerto | Tecnología | Responsabilidad |
|---|---|---|---|
| `eureka-server` | 8761 | Spring Cloud Eureka | Registro y descubrimiento de servicios |
| `api-gateway` | 8080 | Spring Cloud Gateway | Punto de entrada único, valida JWT |
| `auth-service` | 8081 | Spring MVC + JPA + Security (BCrypt) | Registro, login y roles |
| `tecnicos-service` | 8082 | WebFlux + R2DBC | Perfiles, especialidades, tarifa, calificación |
| `solicitudes-service` | 8083 | WebFlux + R2DBC | Solicitudes y ofertas |
| `ordenes-service` | 8084 | WebFlux + R2DBC | Órdenes, fotos, pago simulado, calificación |
| `notificaciones-service` | 8085 | WebFlux + R2DBC | Consume eventos (notificaciones simuladas) |
| `auditoria-service` | 8086 | WebFlux + R2DBC | Consume eventos (auditoría) |
| `frontend/` | 4200 | Angular | Interfaz web (se crea en el paso 12) |

Infraestructura local (`docker-compose.yml`): PostgreSQL 16 (una base por servicio, ver `docker/postgres/init.sql`) y RabbitMQ con consola de administración.

## Requisitos

- JDK 21
- Docker y Docker Compose
- Node.js 20 o superior (solo para el frontend)

## Cómo ejecutar

```bash
# 1. Variables de entorno (opcional: hay valores por defecto)
cp .env.example .env

# 2. Infraestructura: PostgreSQL y RabbitMQ
docker compose up -d

# 3. Compilar y probar todo
./mvnw verify            # en Windows: mvnw.cmd verify

# 4. Levantar los servicios (en este orden, cada uno en su terminal)
./mvnw -pl eureka-server spring-boot:run
./mvnw -pl api-gateway spring-boot:run
./mvnw -pl auth-service spring-boot:run
```

- Eureka: <http://localhost:8761>
- Consola de RabbitMQ: <http://localhost:15672> (usuario y clave de `.env`)

## Flujo de trabajo con Git

- `main`: versión estable. `develop`: integración. Una rama por tarea: `feature/<paso>-<tema>` (por ejemplo, `feature/06-auth-service`).
- Cada `push` ejecuta el workflow de CI (`.github/workflows/ci.yml`), que corre `./mvnw -B verify` con Java 21.
- Mensajes de commit en español, en imperativo: `Agrega login con BCrypt`.

## Versiones

Spring Boot 3.5.16 y Spring Cloud 2025.0.3 (compatibles entre sí), Java 21, Maven 3.9.11 mediante Maven Wrapper.

## Avance por pasos

- [x] 1. Alcance y reglas de negocio
- [x] 2. Diagnóstico SEPTE y objetivos SMART
- [x] 3. Arquitectura y modelo de datos
- [x] 4. Repositorio y CI inicial
- [ ] 5. Eureka + API Gateway
- [ ] 6. `auth-service`
- [ ] 7. `tecnicos-service`
- [ ] 8. `solicitudes-service`
- [ ] 9. `ordenes-service`
- [ ] 10. RabbitMQ (eventos, notificaciones y auditoría)
- [ ] 11. Pruebas automatizadas
- [ ] 12. Angular: parte pública y técnicos
- [ ] 13. Angular: parte del cliente
- [ ] 14. CI completo y evidencias
- [ ] 15. Documentación y sustentación

## Pruebas

```bash
./mvnw -B verify
```

Ejecuta todas las pruebas con H2 en memoria (no necesitan Docker) y genera el reporte de cobertura de JaCoCo en `<servicio>/target/site/jacoco/index.html`. El CI de GitHub Actions hace lo mismo y guarda los reportes como artefactos.

| Tipo | Qué comprueba | Ejemplos |
|---|---|---|
| Capa de datos | Insertar, actualizar, eliminar y listar en los repositorios, más restricciones únicas y cascadas | `UsuarioRepositoryTests`, `OrdenRepositoryTests` |
| Unitarias (Mockito) | Reglas de negocio sin Spring ni base de datos | `PagoServiceTests`, `OrdenServiceTests`, `JwtServiceTests` |
| API | Cada endpoint con roles, validaciones y errores | `OrdenApiTests`, `SolicitudApiTests` |
| Eventos | Publicación y consumo de eventos sin RabbitMQ | `EventoConsumidorTests`, `EventosTecnicoTests` |
| Gateway | Una ruta por servicio hacia su nombre en Eureka | `RutasGatewayTests` |

### Prueba de punta a punta

Con `docker compose up -d` y los 8 servicios arriba:

```bash
bash scripts/e2e.sh
```

Recorre el flujo completo por el Gateway: registro, validación del técnico, solicitud, oferta, orden con fotos, pago, entrega, calificación, notificaciones y auditoría.

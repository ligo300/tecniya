# TecniYa Web (Angular)

Frontend de TecniYa. Consume la API a través del API Gateway (`http://localhost:8080`).

## Requisitos
- Node.js 20.19+ (o 22+) y npm 10+

## Comandos
```bash
npm ci               # instala dependencias (usa el package-lock.json)
npm start            # http://localhost:4200  (proxy de /api hacia el Gateway en :8080)
npm test -- --watch=false   # pruebas unitarias (Vitest)
npm run build        # compila a dist/tecniya-web
```

En desarrollo el archivo `proxy.conf.json` redirige `/api/*` al Gateway, así no hay problemas de CORS.

## Pantallas (pasos 12 y 13)
| Ruta | Quién | Qué hace |
|---|---|---|
| `/` | Público | Catálogo de técnicos con filtros (equipo, zona, calificación) |
| `/tecnicos/:id` | Público | Perfil público del técnico |
| `/registro`, `/login` | Público | Crear cuenta (CLIENTE o TECNICO) e ingresar |
| `/panel` | TECNICO | Crear/editar perfil y ver sus órdenes |
| `/ordenes/:id` | Con sesión | Línea de avance, fotos por etapa, subir foto y cambiar estado |
| `/admin` | ADMIN | Activar o suspender técnicos |
| `/solicitudes`, `/solicitudes/nueva` | CLIENTE | Publicar una solicitud y ver las propias |
| `/solicitudes/:id` | CLIENTE | Comparar ofertas por precio, aceptar una o cancelar |
| `/contratar/:id` | CLIENTE | Contratación directa por la tarifa base del técnico |
| `/mis-ordenes` | CLIENTE | Lista de órdenes; en `/ordenes/:id` paga y califica |
| `/abiertas` | TECNICO | Solicitudes abiertas de sus equipos y envío de ofertas |
| `/notificaciones` | Con sesión | Avisos de solicitudes, órdenes y pagos |

## Estructura
- `src/app/core`: modelos, `AuthService` (JWT), interceptor, guards, `ApiService`
- `src/app/shared`: foto con token (blob), estrellas, línea de avance, formulario de perfil
- `src/app/pages`: una carpeta por pantalla

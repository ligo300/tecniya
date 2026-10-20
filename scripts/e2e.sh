#!/bin/bash
# Prueba de punta a punta de TecniYa: recorre todo el flujo a traves del Gateway.
#
# Requisitos: Docker con PostgreSQL y RabbitMQ (docker compose up -d) y los 8 servicios arriba.
# Uso:        bash scripts/e2e.sh
# Variables opcionales: GATEWAY_URL, ADMIN_EMAIL, ADMIN_PASSWORD

GW="${GATEWAY_URL:-http://localhost:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@tecniya.pe}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin12345}"
SELLO="$(date +%s)"
TMP=".e2e-tmp-$SELLO"          # carpeta relativa: evita problemas de rutas en Git Bash
mkdir -p "$TMP"
trap 'rm -rf "$TMP"' EXIT

OK=0
FALLOS=0
CODIGO=""
CUERPO=""

# peticion METODO RUTA TOKEN [JSON]: deja el codigo HTTP en $CODIGO y la respuesta en $CUERPO
peticion() {
  local metodo="$1" ruta="$2" token="$3" datos="$4"
  local args=(-s -o "$TMP/cuerpo" -w '%{http_code}' -X "$metodo" "$GW$ruta")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$datos" ] && args+=(-H 'Content-Type: application/json' -d "$datos")
  CODIGO=$(curl "${args[@]}")
  CUERPO=$(cat "$TMP/cuerpo")
}

# subir_foto ORDEN_ID ESTADO TOKEN: sube la foto de prueba (multipart)
subir_foto() {
  CODIGO=$(curl -s -o "$TMP/cuerpo" -w '%{http_code}' -X POST "$GW/api/ordenes/$1/fotos" \
    -H "Authorization: Bearer $3" \
    -F "archivo=@$TMP/foto.png;type=image/png" -F "estado=$2")
  CUERPO=$(cat "$TMP/cuerpo")
}

verificar() {   # descripcion codigo_esperado
  if [ "$CODIGO" = "$2" ]; then
    echo "  OK    $1 ($CODIGO)"; OK=$((OK + 1))
  else
    echo "  FALLA $1: se esperaba $2 y llego $CODIGO"
    echo "        $CUERPO"; FALLOS=$((FALLOS + 1))
  fi
}

contiene() {    # descripcion texto_esperado_en_la_respuesta
  if echo "$CUERPO" | grep -q "$2"; then
    echo "  OK    $1"; OK=$((OK + 1))
  else
    echo "  FALLA $1: no aparece '$2'"
    echo "        $CUERPO"; FALLOS=$((FALLOS + 1))
  fi
}

campo() {       # nombre: primer valor de ese campo en la respuesta
  echo "$CUERPO" | grep -o "\"$1\":\"\?[^\",}]*" | head -1 | sed -E 's/^"[^"]*":"?//'
}

echo "== Comprobando el Gateway en $GW"
peticion GET /actuator/health ""
if [ "$CODIGO" != "200" ]; then
  echo "El Gateway no responde (codigo $CODIGO). Levanta Docker y los 8 servicios primero."
  exit 1
fi

# ---------------------------------------------------------------------------
echo "== 1. Registro e inicio de sesion"
CLI_EMAIL="e2e-cliente-$SELLO@correo.com"
TEC_EMAIL="e2e-tecnico-$SELLO@correo.com"

peticion POST /api/auth/registro "" "{\"email\":\"$CLI_EMAIL\",\"password\":\"Clave12345\",\"rol\":\"CLIENTE\"}"
verificar "Registrar cliente" 201
peticion POST /api/auth/registro "" "{\"email\":\"$TEC_EMAIL\",\"password\":\"Clave12345\",\"rol\":\"TECNICO\"}"
verificar "Registrar tecnico" 201
peticion POST /api/auth/registro "" "{\"email\":\"$CLI_EMAIL\",\"password\":\"Clave12345\",\"rol\":\"CLIENTE\"}"
verificar "Correo repetido es rechazado" 409

peticion POST /api/auth/login "" "{\"email\":\"$CLI_EMAIL\",\"password\":\"Clave12345\"}"
verificar "Login del cliente" 200
TOK_CLI=$(campo token)
peticion POST /api/auth/login "" "{\"email\":\"$TEC_EMAIL\",\"password\":\"Clave12345\"}"
verificar "Login del tecnico" 200
TOK_TEC=$(campo token)
peticion POST /api/auth/login "" "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}"
verificar "Login del administrador" 200
TOK_ADM=$(campo token)
peticion POST /api/auth/login "" "{\"email\":\"$CLI_EMAIL\",\"password\":\"incorrecta\"}"
verificar "Clave incorrecta da 401" 401

# ---------------------------------------------------------------------------
echo "== 2. Perfil del tecnico y validacion del administrador"
DOC=$(printf '%08d' $(( (RANDOM * 32768 + RANDOM) % 100000000 )))
peticion POST /api/tecnicos "$TOK_TEC" \
  "{\"nombres\":\"Luis\",\"apellidos\":\"Prueba\",\"documento\":\"$DOC\",\"telefono\":\"999111222\",\"zona\":\"Lima\",\"tarifaBase\":50,\"especialidades\":[\"PC\"]}"
verificar "El tecnico crea su perfil" 201
TEC_ID=$(campo id)
peticion GET "/api/tecnicos/$TEC_ID" ""
verificar "Pendiente de validacion: no aparece en el catalogo" 404
peticion PUT "/api/tecnicos/$TEC_ID/estado" "$TOK_CLI" '{"estado":"ACTIVO"}'
verificar "Un cliente no puede activar tecnicos" 403
peticion PUT "/api/tecnicos/$TEC_ID/estado" "$TOK_ADM" '{"estado":"ACTIVO"}'
verificar "El administrador activa al tecnico" 200
peticion GET "/api/tecnicos/$TEC_ID" ""
verificar "Ya aparece en el catalogo publico" 200

# ---------------------------------------------------------------------------
echo "== 3. Solicitud, oferta y aceptacion (estilo inDrive)"
peticion POST /api/solicitudes "$TOK_CLI" \
  '{"tipoEquipo":"PC","marcaModelo":"HP Pavilion","descripcionFalla":"No enciende","presupuestoRef":100}'
verificar "El cliente publica su solicitud" 201
SOL_ID=$(campo id)
peticion POST "/api/solicitudes/$SOL_ID/ofertas" "$TOK_TEC" \
  '{"precio":80,"plazoDias":2,"mensaje":"Lo reviso manana"}'
verificar "El tecnico envia su oferta" 201
OFE_ID=$(campo id)
peticion GET "/api/solicitudes/$SOL_ID/ofertas" "$TOK_CLI"
verificar "El cliente ve las ofertas" 200
contiene "La oferta de 80 aparece" '"precio":80'
peticion POST "/api/ofertas/$OFE_ID/aceptar" "$TOK_CLI"
verificar "El cliente acepta la oferta" 200

# ---------------------------------------------------------------------------
echo "== 4. Orden con trazabilidad por fotos"
peticion GET /api/ordenes "$TOK_CLI"
verificar "El cliente lista sus ordenes" 200
ORD_ID=$(campo id)
contiene "La orden nace CONTRATADO" '"CONTRATADO"'

printf '\211PNG\r\n\032\n' > "$TMP/foto.png"
head -c 2048 /dev/zero >> "$TMP/foto.png"

peticion PUT "/api/ordenes/$ORD_ID/estado" "$TOK_TEC" '{"estado":"RECIBIDO"}'
verificar "Sin foto no se puede pasar a RECIBIDO" 400
subir_foto "$ORD_ID" "RECIBIDO" "$TOK_CLI"
verificar "El cliente no puede subir fotos" 403

for ESTADO in RECIBIDO EN_DIAGNOSTICO EN_REPARACION LISTO; do
  subir_foto "$ORD_ID" "$ESTADO" "$TOK_TEC"
  verificar "Foto de $ESTADO" 201
  peticion PUT "/api/ordenes/$ORD_ID/estado" "$TOK_TEC" "{\"estado\":\"$ESTADO\"}"
  verificar "Estado $ESTADO" 200
done

# ---------------------------------------------------------------------------
echo "== 5. Pago simulado, entrega y calificacion"
peticion PUT "/api/ordenes/$ORD_ID/estado" "$TOK_TEC" '{"estado":"ENTREGADO"}'
verificar "No se entrega sin pago" 400
peticion POST "/api/ordenes/$ORD_ID/pago" "$TOK_CLI" \
  "{\"monto\":80,\"medio\":\"YAPE\",\"nroOperacion\":\"E2E-$SELLO\"}"
verificar "El cliente paga" 201
contiene "El pago queda PAGADO" '"PAGADO"'
peticion PUT "/api/ordenes/$ORD_ID/estado" "$TOK_TEC" '{"estado":"ENTREGADO"}'
verificar "El tecnico entrega" 200
peticion POST "/api/ordenes/$ORD_ID/calificacion" "$TOK_CLI" \
  '{"puntaje":5,"comentario":"Excelente trabajo"}'
verificar "El cliente califica" 201
peticion POST "/api/ordenes/$ORD_ID/calificacion" "$TOK_CLI" '{"puntaje":4}'
verificar "No se califica dos veces" 409

peticion GET "/api/ordenes/$ORD_ID" "$TOK_CLI"
verificar "Detalle de la orden" 200
contiene "Historial con ENTREGADO" '"estadoNuevo":"ENTREGADO"'
contiene "Fotos registradas" '"rutaDescarga"'

# ---------------------------------------------------------------------------
echo "== 6. Eventos asincronos (RabbitMQ)"
echo "  Esperando 5 segundos a que los consumidores procesen los eventos..."
sleep 5
peticion GET "/api/tecnicos/$TEC_ID" ""
verificar "Perfil publico del tecnico" 200
contiene "RN-20: una calificacion registrada" '"califCantidad":1'
contiene "RN-20: promedio 5" '"califPromedio":5'

peticion GET /api/notificaciones "$TOK_CLI"
verificar "Notificaciones del cliente" 200
contiene "Avisos de la orden" 'ORD-'
peticion GET /api/notificaciones "$TOK_TEC"
verificar "Notificaciones del tecnico" 200
contiene "Aviso de la calificacion" '5/5'

peticion GET "/api/auditoria?tipo=OrdenCalificada" "$TOK_ADM"
verificar "Auditoria para el administrador" 200
contiene "Evento OrdenCalificada auditado" 'OrdenCalificada'
peticion GET /api/auditoria "$TOK_CLI"
verificar "Un cliente no ve la auditoria" 403

# ---------------------------------------------------------------------------
echo
echo "Resultado: $OK correctas, $FALLOS fallidas."
[ "$FALLOS" -eq 0 ]

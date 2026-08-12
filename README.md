# TienditaB — backend del punto de venta

API del sistema de la tienda de abarrotes y desechables. El plan completo del
proyecto (cómo funciona, el modelo de datos, las fases) vive en `~/tiendita/`.

## Stack

- Spring Boot 3.5, Java 17, Maven (wrapper en `api/`)
- MariaDB 10.11 + Flyway
- Destino final: Raspberry Pi 4 B en el mostrador

## Levantar en desarrollo

La base corre en Docker, aislada del MySQL de RestFood:

```bash
cd ~/tiendita/entorno && docker compose up -d     # MariaDB en el 3307
cd ~/TienditaB/api
TIENDITA_DB_USER=tiendita_app TIENDITA_DB_PASSWORD=tiendita_dev ./mvnw spring-boot:run
```

La API queda en `http://localhost:8090`.

```bash
./mvnw clean test     # 27 tests contra la base real. SIEMPRE antes de abrir PR.
```

## Lo que hay hasta ahora (Fase 1)

| Área | Endpoints |
|---|---|
| Catálogo | `GET /productos?q=`, `GET /productos/codigo/{codigo}`, `POST /productos` (alta rápida), `PUT /productos/{id}`, `DELETE /productos/{id}` |
| Inventario | `POST /productos/{id}/merma`, `/autoconsumo`, `/conteo`, `GET /productos/stock-bajo` |
| Venta | `POST /ventas`, `GET /ventas/{id}`, `POST /ventas/{id}/cancelar`, `GET /ventas/{id}/ticket`, `GET /ventas/resumen` |
| Caja | `GET /caja`, `POST /caja/abrir`, `/gasto`, `/retiro`, `/deposito`, `/cerrar`, `GET /caja/cortes` |

## Las tres reglas del modelo

1. **Toda cantidad es `DECIMAL(10,3)`** — el granel se vende por kilo real (1.250 kg).
2. **El costo se copia dentro de la venta** — el margen de una venta de marzo no
   puede cambiar porque el costo subió en abril.
3. **Nada se edita ni se borra** — cancelar agrega el movimiento contrario.

Las tres están fijadas con tests: `CobroTest`, `CancelacionTest`, `KardexCuadraTest`.

## Lo que todavía no está

- **Login.** Todos los movimientos se firman como `mostrador`. Es el siguiente PR.
- **Impresión física.** `TicketService` arma el texto de 48 columnas y está
  probado; falta la impresora y mandarlo a la cola de CUPS.
- **Compras y costeo** (Fase 2): las tablas ya existen en la `V1`, sin código.

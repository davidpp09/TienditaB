# CLAUDE.md — Tiendita Backend

Guía para Claude Code en este repo. El contexto del proyecto está en `~/tiendita/`
(`PLAN.md` y `modelo/ESQUEMA.md`) — leerlo antes de tocar el modelo de datos.

## Stack
- Spring Boot 3.5, Java 17, Maven (wrapper en `api/`)
- MariaDB 10.11 + Flyway. **No es MySQL**: la Raspberry corre MariaDB y el
  desarrollo la iguala a propósito.

## Comandos (desde `api/`)
```bash
./mvnw clean test        # SIEMPRE con clean, y siempre antes de abrir PR
./mvnw spring-boot:run   # con TIENDITA_DB_USER y TIENDITA_DB_PASSWORD
```

## Flujo de trabajo (obligatorio)
1. Nunca commitear directo a `main`.
2. Rama `feat/<descripcion>` o `fix/<descripcion>`.
3. Commits pequeños, en español, formato conventional.
4. `./mvnw clean test` antes de abrir PR.
5. PR a `main` → revisión de David → merge.

## Reglas del proyecto

- **Este sistema cobra dinero en tiempo real.** En RestFood, diez minutos caído
  significa esperar; aquí significa no vender. Ante la duda, la venta gana.
- **Nada puede impedir cobrar.** Producto sin código, sin costo, existencia en
  negativo, caja sin abrir: todo eso se resuelve solo y se corrige después. Si un
  trámite bloquea el mostrador, el sistema se abandona y se vuelve al cuaderno.
- **Las tres reglas del modelo** (cantidades decimales, costo copiado en la venta,
  nada se borra) están fijadas con tests. Si un cambio las rompe, el cambio está mal.
- **El precio lo pone el servidor**, nunca el navegador.
- **Todo cambio de esquema va en una migración de Flyway**, nunca a mano en la
  base. Con `ddl-auto=validate`, si agregas un campo y olvidas la migración, el
  CI falla.
- **Variables de entorno con prefijo `TIENDITA_`.** Los nombres genéricos ya
  están tomados: el `.bashrc` de la máquina de desarrollo exporta `DB_PASSWORD`
  para RestFood y se coló a los tests el primer día.
- **No tocar nada de RestFood**: ni el MySQL del sistema (:3306), ni su backend
  (:8080), ni el Caddyfile de producción. Puertos de Tiendita: 8090 y 3307.
- Explicarle a David el porqué de cada práctica nueva, no solo el resultado.

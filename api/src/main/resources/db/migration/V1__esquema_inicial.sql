-- Esquema inicial de la tiendita.
--
-- Tres reglas gobiernan este esquema (ver ~/tiendita/modelo/ESQUEMA.md):
--   1. Toda CANTIDAD es DECIMAL(10,3)  -> el granel se vende por kilo real.
--   2. Todo DINERO es DECIMAL          -> nunca DOUBLE, o el corte no cuadra.
--   3. Nada se edita ni se borra       -> cancelar crea el movimiento contrario.

CREATE TABLE categoria (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre  VARCHAR(60)  NOT NULL,
    activo  BOOLEAN      NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE proveedor (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(120) NOT NULL,
    telefono      VARCHAR(30),
    dias_entrega  INT          NOT NULL DEFAULT 7,
    notas         VARCHAR(300),
    activo        BOOLEAN      NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE producto (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    -- NULL a propósito: el granel y lo que no trae código no tienen código de
    -- barras. UNIQUE permite varios NULL en MariaDB, que es justo lo que se quiere.
    codigo_barras   VARCHAR(20),
    nombre          VARCHAR(120)  NOT NULL,
    categoria_id    BIGINT,
    unidad          VARCHAR(10)   NOT NULL,          -- PIEZA | KILO
    precio_venta    DECIMAL(10,2) NOT NULL,
    precio_mayoreo  DECIMAL(10,2),
    -- 4 decimales: el costo promedio es una división y se arrastra en cada compra.
    costo_promedio  DECIMAL(10,4) NOT NULL DEFAULT 0,
    existencia      DECIMAL(10,3) NOT NULL DEFAULT 0,
    stock_minimo    DECIMAL(10,3) NOT NULL DEFAULT 0,
    dias_proveedor  INT           NOT NULL DEFAULT 7,
    proveedor_id    BIGINT,
    activo          BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en       DATETIME      NOT NULL,
    CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id) REFERENCES categoria(id),
    CONSTRAINT fk_producto_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor(id)
) ENGINE=InnoDB;

CREATE UNIQUE INDEX ux_producto_codigo ON producto (codigo_barras);
CREATE INDEX ix_producto_nombre       ON producto (nombre);

-- Sin columna `folio`: el folio del ticket se deriva del id (V000123). Una
-- columna NOT NULL UNIQUE con un valor derivado del id solo agrega una escritura
-- extra y una carrera posible al cobrar, sin aportar nada.
CREATE TABLE venta (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha_hora  DATETIME      NOT NULL,
    total       DECIMAL(10,2) NOT NULL,
    -- El costo de lo vendido se guarda en la venta para no recalcularlo en cada
    -- reporte. Suma de los costo_unitario de sus renglones.
    costo_total DECIMAL(10,2) NOT NULL,
    forma_pago  VARCHAR(20)   NOT NULL,             -- EFECTIVO | TARJETA | TRANSFERENCIA
    recibido    DECIMAL(10,2),
    cambio      DECIMAL(10,2),
    cancelada   BOOLEAN       NOT NULL DEFAULT FALSE,
    cancelada_en DATETIME,
    motivo_cancelacion VARCHAR(200),
    usuario     VARCHAR(40)   NOT NULL
) ENGINE=InnoDB;

CREATE INDEX ix_venta_fecha ON venta (fecha_hora);

CREATE TABLE venta_detalle (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    venta_id        BIGINT        NOT NULL,
    producto_id     BIGINT        NOT NULL,
    -- Copia del nombre: si el producto se renombra, el ticket viejo no cambia.
    descripcion     VARCHAR(120)  NOT NULL,
    cantidad        DECIMAL(10,3) NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    -- EL RENGLÓN MÁS IMPORTANTE DEL ESQUEMA: el costo del momento en que se
    -- vendió. Sin esto, el margen de una venta de marzo calculado con el costo
    -- de hoy es ficción, y todos los reportes históricos mienten.
    costo_unitario  DECIMAL(10,4) NOT NULL,
    importe         DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_detalle_venta    FOREIGN KEY (venta_id)    REFERENCES venta(id),
    CONSTRAINT fk_detalle_producto FOREIGN KEY (producto_id) REFERENCES producto(id)
) ENGINE=InnoDB;

CREATE INDEX ix_venta_detalle_producto ON venta_detalle (producto_id);

-- El kardex. Es un libro, no un pizarrón: aquí solo se agregan renglones.
-- producto.existencia es un caché de esta tabla y debe poder reconstruirse
-- recorriéndola (ver KardexCuadraTest).
CREATE TABLE movimiento_inventario (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id           BIGINT        NOT NULL,
    fecha_hora            DATETIME      NOT NULL,
    tipo                  VARCHAR(25)   NOT NULL,
    -- Con signo: entradas positivas, salidas negativas.
    cantidad              DECIMAL(10,3) NOT NULL,
    costo_unitario        DECIMAL(10,4) NOT NULL,
    -- La foto de la existencia justo después del movimiento. Cuesta un campo y
    -- ahorra horas cuando algo no cuadra: se ve en qué renglón se torció.
    existencia_resultante DECIMAL(10,3) NOT NULL,
    referencia_tipo       VARCHAR(20),
    referencia_id         BIGINT,
    motivo                VARCHAR(200),
    usuario               VARCHAR(40)   NOT NULL,
    CONSTRAINT fk_kardex_producto FOREIGN KEY (producto_id) REFERENCES producto(id)
) ENGINE=InnoDB;

CREATE INDEX ix_kardex_producto_fecha ON movimiento_inventario (producto_id, fecha_hora);

CREATE TABLE corte_caja (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha          DATE          NOT NULL,
    fondo_inicial  DECIMAL(10,2) NOT NULL,
    total_ventas   DECIMAL(10,2) NOT NULL,
    total_gastos   DECIMAL(10,2) NOT NULL,
    total_retiros  DECIMAL(10,2) NOT NULL,
    esperado       DECIMAL(10,2) NOT NULL,
    contado        DECIMAL(10,2) NOT NULL,
    -- Se guarda aunque sea negativa. Un sistema que no deja registrar faltantes
    -- es un sistema en el que se dejan de registrar.
    diferencia     DECIMAL(10,2) NOT NULL,
    abierto_en     DATETIME      NOT NULL,
    cerrado_en     DATETIME,
    usuario        VARCHAR(40)   NOT NULL,
    notas          VARCHAR(300)
) ENGINE=InnoDB;

CREATE INDEX ix_corte_fecha ON corte_caja (fecha);

CREATE TABLE movimiento_caja (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha_hora      DATETIME      NOT NULL,
    tipo            VARCHAR(20)   NOT NULL,   -- FONDO|VENTA|GASTO|COMPRA|RETIRO|DEPOSITO
    -- Con signo. Una venta cancelada entra como VENTA con monto negativo.
    monto           DECIMAL(10,2) NOT NULL,
    concepto        VARCHAR(200)  NOT NULL,
    categoria_gasto VARCHAR(50),
    referencia_tipo VARCHAR(20),
    referencia_id   BIGINT,
    corte_id        BIGINT,
    usuario         VARCHAR(40)   NOT NULL,
    CONSTRAINT fk_caja_corte FOREIGN KEY (corte_id) REFERENCES corte_caja(id)
) ENGINE=InnoDB;

CREATE INDEX ix_caja_fecha ON movimiento_caja (fecha_hora);
CREATE INDEX ix_caja_corte ON movimiento_caja (corte_id);

-- Tablas de la Fase 2 (compras). Se crean desde ya para que el esquema del
-- repositorio sea el esquema completo documentado, y para que la migración que
-- traiga el costeo no tenga que tocar estructura.
CREATE TABLE compra (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    proveedor_id BIGINT        NOT NULL,
    fecha        DATE          NOT NULL,
    folio        VARCHAR(40),
    total        DECIMAL(10,2) NOT NULL,
    pagada       BOOLEAN       NOT NULL DEFAULT TRUE,
    usuario      VARCHAR(40)   NOT NULL,
    CONSTRAINT fk_compra_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor(id)
) ENGINE=InnoDB;

CREATE TABLE compra_detalle (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    compra_id      BIGINT        NOT NULL,
    producto_id    BIGINT        NOT NULL,
    cantidad       DECIMAL(10,3) NOT NULL,
    costo_unitario DECIMAL(10,4) NOT NULL,
    importe        DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_compra_detalle_compra   FOREIGN KEY (compra_id)   REFERENCES compra(id),
    CONSTRAINT fk_compra_detalle_producto FOREIGN KEY (producto_id) REFERENCES producto(id)
) ENGINE=InnoDB;

INSERT INTO categoria (nombre) VALUES
    ('Abarrotes'), ('Desechables'), ('Bebidas'), ('Botanas'), ('Limpieza'), ('Otros');

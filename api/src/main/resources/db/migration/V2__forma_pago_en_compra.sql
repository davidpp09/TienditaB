-- La compra necesita saber CÓMO se pagó, no solo si se pagó.
--
-- La V1 dejó `pagada`, que contesta "¿ya se le pagó al proveedor?". Eso no
-- alcanza para el corte de caja: si el pago fue por transferencia, el dinero
-- nunca estuvo en el cajón, y descontarlo del corte inventaría un faltante de
-- cientos de pesos que nadie va a poder explicar en la noche.
--
-- Mismos valores que `venta.forma_pago`, y la misma regla: solo EFECTIVO mueve
-- el cajón. EFECTIVO por omisión porque es como se le paga a casi todos los
-- proveedores de la tienda.
ALTER TABLE compra
    ADD COLUMN forma_pago VARCHAR(20) NOT NULL DEFAULT 'EFECTIVO' AFTER pagada;

-- El historial de precios de compra ("¿quién me vende más barato?") entra por
-- producto y sale ordenado por fecha. Sin este índice, esa consulta recorre
-- todos los renglones de compra que existan.
CREATE INDEX ix_compra_detalle_producto ON compra_detalle (producto_id);
CREATE INDEX ix_compra_fecha            ON compra (fecha);

ALTER TABLE restaurantes
ADD COLUMN raio_entrega_km DECIMAL(5,2) DEFAULT 10.00;

UPDATE restaurantes
SET raio_entrega_km = 10.00
WHERE raio_entrega_km IS NULL;


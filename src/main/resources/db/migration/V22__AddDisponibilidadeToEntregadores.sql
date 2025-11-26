ALTER TABLE entregadores
ADD COLUMN disponibilidade VARCHAR(50) NOT NULL DEFAULT 'UNAVAILABLE';

CREATE INDEX idx_entregadores_disponibilidade ON entregadores(disponibilidade);


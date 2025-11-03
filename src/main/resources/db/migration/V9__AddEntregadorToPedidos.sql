-- Migração V9: Adicionar campos de entregador e tempo estimado na tabela pedidos
ALTER TABLE pedidos 
ADD COLUMN entregador_id BIGINT,
ADD COLUMN tempo_estimado_entrega TIMESTAMP NULL,
ADD CONSTRAINT fk_pedidos_entregador FOREIGN KEY (entregador_id) REFERENCES entregadores(id);

CREATE INDEX idx_pedidos_entregador ON pedidos(entregador_id);
CREATE INDEX idx_pedidos_status_entregador ON pedidos(status, entregador_id);


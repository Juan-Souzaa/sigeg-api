-- Migração V17: Criar tabela de rotas de entrega para armazenar waypoints
CREATE TABLE IF NOT EXISTS rota_entrega (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id BIGINT NOT NULL UNIQUE,
    waypoints TEXT,
    indice_atual INT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    CONSTRAINT fk_rota_entrega_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE
);

CREATE INDEX idx_rota_entrega_pedido ON rota_entrega(pedido_id);


CREATE TABLE avaliacoes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    restaurante_id BIGINT NOT NULL,
    entregador_id BIGINT NULL,
    nota_restaurante INT NOT NULL CHECK (nota_restaurante >= 1 AND nota_restaurante <= 5),
    nota_entregador INT NULL CHECK (nota_entregador IS NULL OR (nota_entregador >= 1 AND nota_entregador <= 5)),
    nota_pedido INT NOT NULL CHECK (nota_pedido >= 1 AND nota_pedido <= 5),
    comentario_restaurante TEXT,
    comentario_entregador TEXT,
    comentario_pedido TEXT,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NULL,
    CONSTRAINT fk_avaliacao_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
    CONSTRAINT fk_avaliacao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_avaliacao_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurantes(id),
    CONSTRAINT fk_avaliacao_entregador FOREIGN KEY (entregador_id) REFERENCES entregadores(id),
    CONSTRAINT uk_avaliacao_cliente_pedido UNIQUE (cliente_id, pedido_id)
);

CREATE INDEX idx_avaliacoes_restaurante ON avaliacoes(restaurante_id);
CREATE INDEX idx_avaliacoes_entregador ON avaliacoes(entregador_id);
CREATE INDEX idx_avaliacoes_pedido ON avaliacoes(pedido_id);
CREATE INDEX idx_avaliacoes_cliente ON avaliacoes(cliente_id);
CREATE INDEX idx_avaliacoes_criado_em ON avaliacoes(criado_em DESC);


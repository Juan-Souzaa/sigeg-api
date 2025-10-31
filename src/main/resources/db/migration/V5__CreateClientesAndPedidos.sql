-- Migração V5: Tabelas de clientes, pedidos e itens
CREATE TABLE clientes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    telefone VARCHAR(20) NOT NULL,
    endereco VARCHAR(500) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pedidos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cliente_id BIGINT NOT NULL,
    restaurante_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    metodo_pagamento VARCHAR(10) NOT NULL,
    troco DECIMAL(10,2),
    observacoes TEXT,
    endereco_entrega VARCHAR(500) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    taxa_entrega DECIMAL(10,2) NOT NULL,
    total DECIMAL(10,2) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pedidos_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_pedidos_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurantes(id)
);

CREATE TABLE pedido_itens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pedido_id BIGINT NOT NULL,
    prato_id BIGINT NOT NULL,
    quantidade INT NOT NULL,
    preco_unitario DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_pedido_itens_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
    CONSTRAINT fk_pedido_itens_prato FOREIGN KEY (prato_id) REFERENCES pratos(id)
);

CREATE INDEX idx_clientes_email ON clientes(email);
CREATE INDEX idx_pedidos_cliente ON pedidos(cliente_id);
CREATE INDEX idx_pedidos_restaurante ON pedidos(restaurante_id);
CREATE INDEX idx_pedidos_status ON pedidos(status);
CREATE INDEX idx_pedido_itens_pedido ON pedido_itens(pedido_id);
CREATE INDEX idx_pedido_itens_prato ON pedido_itens(prato_id);

-- Migração V4: Tabelas de restaurantes e pratos
CREATE TABLE restaurantes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(255) NOT NULL,
    endereco VARCHAR(500) NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    email VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_APPROVAL',
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pratos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(255) NOT NULL,
    descricao TEXT,
    preco DECIMAL(10,2) NOT NULL,
    categoria VARCHAR(20) NOT NULL,
    disponivel BOOLEAN NOT NULL DEFAULT TRUE,
    foto_url VARCHAR(500),
    restaurante_id BIGINT NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pratos_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurantes(id)
);

CREATE INDEX idx_restaurantes_status ON restaurantes(status);
CREATE INDEX idx_pratos_restaurante ON pratos(restaurante_id);
CREATE INDEX idx_pratos_categoria ON pratos(categoria);
CREATE INDEX idx_pratos_disponivel ON pratos(disponivel);

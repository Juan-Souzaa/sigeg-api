CREATE TABLE entregadores (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    nome VARCHAR(255) NOT NULL,
    cpf VARCHAR(14) NOT NULL UNIQUE,
    telefone VARCHAR(20) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    foto_cnh_url TEXT,
    tipo_veiculo VARCHAR(50) NOT NULL,
    placa_veiculo VARCHAR(10) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_APPROVAL',
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    CONSTRAINT fk_entregadores_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_entregadores_user ON entregadores(user_id);
CREATE INDEX idx_entregadores_status ON entregadores(status);
CREATE INDEX idx_entregadores_localizacao ON entregadores(latitude, longitude);


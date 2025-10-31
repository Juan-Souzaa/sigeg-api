-- Schema para testes do SIGEG

-- Tabela de roles
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);

-- Tabela de usuários
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(120) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de relacionamento user_roles
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Tabela de restaurantes
CREATE TABLE IF NOT EXISTS restaurantes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    telefone VARCHAR(20) NOT NULL,
    endereco VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING_APPROVAL','APPROVED','REJECTED')),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Tabela de pratos
CREATE TABLE IF NOT EXISTS pratos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(500),
    preco DECIMAL(10,2) NOT NULL,
    categoria VARCHAR(20) NOT NULL CHECK (categoria IN ('APPETIZER','MAIN','DESSERT','BEVERAGE')),
    disponivel BOOLEAN NOT NULL DEFAULT TRUE,
    foto_url VARCHAR(255),
    restaurante_id BIGINT NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (restaurante_id) REFERENCES restaurantes(id)
);

-- Tabela de clientes
CREATE TABLE IF NOT EXISTS clientes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    telefone VARCHAR(20) NOT NULL,
    endereco VARCHAR(200) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de pedidos
CREATE TABLE IF NOT EXISTS pedidos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    restaurante_id BIGINT NOT NULL,
    metodo_pagamento VARCHAR(20) NOT NULL CHECK (metodo_pagamento IN ('PIX','CASH','CREDIT_CARD')),
    troco DECIMAL(10,2),
    observacoes VARCHAR(500),
    endereco_entrega VARCHAR(200) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    taxa_entrega DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','CONFIRMED','PREPARING','READY','DELIVERED','CANCELLED')),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    FOREIGN KEY (restaurante_id) REFERENCES restaurantes(id)
);

-- Tabela de itens do pedido
CREATE TABLE IF NOT EXISTS pedido_itens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    prato_id BIGINT NOT NULL,
    quantidade INTEGER NOT NULL,
    preco_unitario DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
    FOREIGN KEY (prato_id) REFERENCES pratos(id)
);

-- Tabela de pagamentos
CREATE TABLE IF NOT EXISTS pagamentos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    valor DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','PAID','FAILED','CANCELLED')),
    asaas_customer_id VARCHAR(100),
    asaas_payment_id VARCHAR(100),
    pix_qr_code TEXT,
    pix_qr_code_image TEXT,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (pedido_id) REFERENCES pedidos(id)
);

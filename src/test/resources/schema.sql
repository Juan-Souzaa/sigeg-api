-- Schema para testes do SIGEG

-- Tabela de roles
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE
);

-- Tabela de usuários
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

-- Tabela de relacionamento user_roles
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Inserir roles padrão (ignorar se já existir - compatível com H2)
INSERT INTO roles(role_name) 
SELECT 'ROLE_USER' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'ROLE_USER');
INSERT INTO roles(role_name) 
SELECT 'ROLE_ADMIN' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'ROLE_ADMIN');
INSERT INTO roles(role_name) 
SELECT 'ROLE_RESTAURANTE' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'ROLE_RESTAURANTE');
INSERT INTO roles(role_name) 
SELECT 'ROLE_ENTREGADOR' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'ROLE_ENTREGADOR');
INSERT INTO roles(role_name) 
SELECT 'ROLE_CLIENTE' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'ROLE_CLIENTE');

-- Tabela de restaurantes
CREATE TABLE IF NOT EXISTS restaurantes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    telefone VARCHAR(20) NOT NULL,
    endereco VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING_APPROVAL','APPROVED','REJECTED')),
    user_id BIGINT,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Tabela de pratos
CREATE TABLE IF NOT EXISTS pratos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    preco DECIMAL(10,2) NOT NULL,
    categoria VARCHAR(20) NOT NULL CHECK (categoria IN ('APPETIZER','MAIN','DESSERT','BEVERAGE')),
    disponivel BOOLEAN NOT NULL DEFAULT TRUE,
    foto_url VARCHAR(255),
    restaurante_id BIGINT NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (restaurante_id) REFERENCES restaurantes(id)
);

-- Tabela de clientes
CREATE TABLE IF NOT EXISTS clientes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    telefone VARCHAR(20) NOT NULL,
    endereco VARCHAR(200) NOT NULL,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Tabela de entregadores
CREATE TABLE IF NOT EXISTS entregadores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Tabela de pedidos
CREATE TABLE IF NOT EXISTS pedidos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    restaurante_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('CREATED','CONFIRMED','PREPARING','OUT_FOR_DELIVERY','DELIVERED','CANCELED')),
    metodo_pagamento VARCHAR(20) NOT NULL CHECK (metodo_pagamento IN ('PIX','CASH','CREDIT_CARD')),
    troco DECIMAL(10,2),
    observacoes TEXT,
    endereco_entrega VARCHAR(200) NOT NULL,
    latitude_entrega DECIMAL(10, 8),
    longitude_entrega DECIMAL(11, 8),
    subtotal DECIMAL(10,2) NOT NULL,
    taxa_entrega DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total DECIMAL(10,2) NOT NULL,
    entregador_id BIGINT,
    tempo_estimado_entrega TIMESTAMP,
    taxa_plataforma_restaurante DECIMAL(10, 2),
    taxa_plataforma_entregador DECIMAL(10, 2),
    valor_liquido_restaurante DECIMAL(10, 2),
    valor_liquido_entregador DECIMAL(10, 2),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    FOREIGN KEY (restaurante_id) REFERENCES restaurantes(id),
    FOREIGN KEY (entregador_id) REFERENCES entregadores(id)
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
    metodo VARCHAR(20) NOT NULL CHECK (metodo IN ('PIX','CASH','CREDIT_CARD')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','PAID','FAILED','CANCELLED')),
    valor DECIMAL(10,2) NOT NULL,
    troco DECIMAL(10,2),
    qr_code VARCHAR(255),
    qr_code_image_url LONGTEXT,
    asaas_payment_id VARCHAR(100),
    asaas_customer_id VARCHAR(100),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    FOREIGN KEY (pedido_id) REFERENCES pedidos(id)
);

-- Tabela de avaliações
CREATE TABLE IF NOT EXISTS avaliacoes (
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
    FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
    FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    FOREIGN KEY (restaurante_id) REFERENCES restaurantes(id),
    FOREIGN KEY (entregador_id) REFERENCES entregadores(id),
    CONSTRAINT uk_avaliacao_cliente_pedido UNIQUE (cliente_id, pedido_id)
);

-- Tabela de cupons
CREATE TABLE IF NOT EXISTS cupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    tipo_desconto VARCHAR(20) NOT NULL,
    valor_desconto DECIMAL(10,2) NOT NULL,
    valor_minimo DECIMAL(10,2) NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    usos_maximos INT NOT NULL,
    usos_atuais INT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_tipo_desconto CHECK (tipo_desconto IN ('PERCENTUAL', 'VALOR_FIXO')),
    CONSTRAINT chk_valor_desconto CHECK (valor_desconto >= 0),
    CONSTRAINT chk_valor_minimo CHECK (valor_minimo >= 0),
    CONSTRAINT chk_usos_maximos CHECK (usos_maximos > 0),
    CONSTRAINT chk_usos_atuais CHECK (usos_atuais >= 0),
    CONSTRAINT chk_data_validade CHECK (data_fim >= data_inicio)
);

-- Tabela de carrinhos
CREATE TABLE IF NOT EXISTS carrinhos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    cupom_id BIGINT NULL,
    subtotal DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    desconto DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE,
    FOREIGN KEY (cupom_id) REFERENCES cupons(id) ON DELETE SET NULL
);

-- Tabela de itens do carrinho
CREATE TABLE IF NOT EXISTS carrinho_itens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    carrinho_id BIGINT NOT NULL,
    prato_id BIGINT NOT NULL,
    quantidade INT NOT NULL,
    preco_unitario DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (carrinho_id) REFERENCES carrinhos(id) ON DELETE CASCADE,
    FOREIGN KEY (prato_id) REFERENCES pratos(id) ON DELETE CASCADE,
    CONSTRAINT chk_quantidade CHECK (quantidade > 0),
    CONSTRAINT chk_preco_unitario CHECK (preco_unitario >= 0),
    CONSTRAINT chk_subtotal CHECK (subtotal >= 0)
);

-- Tabela de configurações de taxa
CREATE TABLE IF NOT EXISTS configuracoes_taxa (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo_taxa VARCHAR(50) NOT NULL,
    percentual DECIMAL(5,2) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_tipo_taxa CHECK (tipo_taxa IN ('TAXA_RESTAURANTE', 'TAXA_ENTREGADOR')),
    CONSTRAINT chk_percentual CHECK (percentual >= 0 AND percentual <= 100)
);

-- Inserir taxas padrão
INSERT INTO configuracoes_taxa (tipo_taxa, percentual, ativo, criado_em, atualizado_em)
SELECT 'TAXA_RESTAURANTE', 10.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM configuracoes_taxa WHERE tipo_taxa = 'TAXA_RESTAURANTE');

INSERT INTO configuracoes_taxa (tipo_taxa, percentual, ativo, criado_em, atualizado_em)
SELECT 'TAXA_ENTREGADOR', 15.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM configuracoes_taxa WHERE tipo_taxa = 'TAXA_ENTREGADOR');

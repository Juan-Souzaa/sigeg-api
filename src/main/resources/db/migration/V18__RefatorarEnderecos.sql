-- Migração V18: Refatorar estrutura de endereços para Entity separada

-- Criar tabela enderecos
CREATE TABLE enderecos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cliente_id BIGINT,
    restaurante_id BIGINT,
    logradouro VARCHAR(200) NOT NULL,
    numero VARCHAR(10) NOT NULL,
    complemento VARCHAR(50),
    bairro VARCHAR(100) NOT NULL,
    cidade VARCHAR(100) NOT NULL,
    estado VARCHAR(2) NOT NULL,
    cep VARCHAR(8) NOT NULL,
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    tipo VARCHAR(20) NOT NULL DEFAULT 'OUTRO',
    principal BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_enderecos_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE,
    CONSTRAINT fk_enderecos_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurantes(id) ON DELETE CASCADE
);

-- Migrar dados de clientes.endereco para enderecos
INSERT INTO enderecos (cliente_id, logradouro, numero, complemento, bairro, cidade, estado, cep, latitude, longitude, tipo, principal, criado_em)
SELECT 
    id as cliente_id,
    SUBSTRING_INDEX(SUBSTRING_INDEX(endereco, ',', 1), ',', -1) as logradouro,
    COALESCE(SUBSTRING_INDEX(SUBSTRING_INDEX(endereco, ',', 2), ',', -1), '0') as numero,
    NULL as complemento,
    COALESCE(SUBSTRING_INDEX(SUBSTRING_INDEX(endereco, ',', 3), ',', -1), 'Centro') as bairro,
    COALESCE(SUBSTRING_INDEX(SUBSTRING_INDEX(endereco, ',', 4), ',', -1), 'São Paulo') as cidade,
    COALESCE(SUBSTRING_INDEX(SUBSTRING_INDEX(endereco, ',', 5), ',', -1), 'SP') as estado,
    COALESCE(REGEXP_SUBSTR(endereco, '[0-9]{5}-?[0-9]{3}'), '00000000') as cep,
    latitude,
    longitude,
    'OUTRO' as tipo,
    TRUE as principal,
    criado_em
FROM clientes
WHERE endereco IS NOT NULL AND endereco != '';

-- Migrar dados de restaurantes.endereco para enderecos
INSERT INTO enderecos (restaurante_id, logradouro, numero, complemento, bairro, cidade, estado, cep, latitude, longitude, tipo, principal, criado_em)
SELECT 
    id as restaurante_id,
    SUBSTRING_INDEX(SUBSTRING_INDEX(endereco, ',', 1), ',', -1) as logradouro,
    COALESCE(SUBSTRING_INDEX(SUBSTRING_INDEX(endereco, ',', 2), ',', -1), '0') as numero,
    NULL as complemento,
    COALESCE(SUBSTRING_INDEX(SUBSTRING_INDEX(endereco, ',', 3), ',', -1), 'Centro') as bairro,
    COALESCE(SUBSTRING_INDEX(SUBSTRING_INDEX(endereco, ',', 4), ',', -1), 'São Paulo') as cidade,
    COALESCE(SUBSTRING_INDEX(SUBSTRING_INDEX(endereco, ',', 5), ',', -1), 'SP') as estado,
    COALESCE(REGEXP_SUBSTR(endereco, '[0-9]{5}-?[0-9]{3}'), '00000000') as cep,
    latitude,
    longitude,
    'OUTRO' as tipo,
    TRUE as principal,
    criado_em
FROM restaurantes
WHERE endereco IS NOT NULL AND endereco != '';

-- Adicionar coluna endereco_entrega_id em pedidos
ALTER TABLE pedidos ADD COLUMN endereco_entrega_id BIGINT;

-- Migrar dados de pedidos.endereco_entrega para enderecos (sem vincular a cliente/restaurante)
INSERT INTO enderecos (cliente_id, restaurante_id, logradouro, numero, complemento, bairro, cidade, estado, cep, latitude, longitude, tipo, principal, criado_em)
SELECT 
    NULL as cliente_id,
    NULL as restaurante_id,
    SUBSTRING_INDEX(SUBSTRING_INDEX(endereco_entrega, ',', 1), ',', -1) as logradouro,
    COALESCE(SUBSTRING_INDEX(SUBSTRING_INDEX(endereco_entrega, ',', 2), ',', -1), '0') as numero,
    NULL as complemento,
    COALESCE(SUBSTRING_INDEX(SUBSTRING_INDEX(endereco_entrega, ',', 3), ',', -1), 'Centro') as bairro,
    COALESCE(SUBSTRING_INDEX(SUBSTRING_INDEX(endereco_entrega, ',', 4), ',', -1), 'São Paulo') as cidade,
    COALESCE(SUBSTRING_INDEX(SUBSTRING_INDEX(endereco_entrega, ',', 5), ',', -1), 'SP') as estado,
    COALESCE(REGEXP_SUBSTR(endereco_entrega, '[0-9]{5}-?[0-9]{3}'), '00000000') as cep,
    latitude_entrega as latitude,
    longitude_entrega as longitude,
    'OUTRO' as tipo,
    FALSE as principal,
    criado_em
FROM pedidos
WHERE endereco_entrega IS NOT NULL AND endereco_entrega != '';

-- Atualizar pedidos com endereco_entrega_id
UPDATE pedidos p
INNER JOIN enderecos e ON (
    e.cliente_id IS NULL 
    AND e.restaurante_id IS NULL
    AND e.logradouro = SUBSTRING_INDEX(SUBSTRING_INDEX(p.endereco_entrega, ',', 1), ',', -1)
    AND p.endereco_entrega IS NOT NULL
)
SET p.endereco_entrega_id = e.id
WHERE p.endereco_entrega IS NOT NULL;

-- Adicionar foreign key para endereco_entrega_id
ALTER TABLE pedidos 
ADD CONSTRAINT fk_pedidos_endereco_entrega FOREIGN KEY (endereco_entrega_id) REFERENCES enderecos(id);

-- Remover colunas antigas de clientes
ALTER TABLE clientes DROP COLUMN endereco;
ALTER TABLE clientes DROP COLUMN latitude;
ALTER TABLE clientes DROP COLUMN longitude;

-- Remover colunas antigas de restaurantes
ALTER TABLE restaurantes DROP COLUMN endereco;
ALTER TABLE restaurantes DROP COLUMN latitude;
ALTER TABLE restaurantes DROP COLUMN longitude;

-- Remover colunas antigas de pedidos
ALTER TABLE pedidos DROP COLUMN endereco_entrega;
ALTER TABLE pedidos DROP COLUMN latitude_entrega;
ALTER TABLE pedidos DROP COLUMN longitude_entrega;

-- Criar índices para melhorar performance
CREATE INDEX idx_enderecos_cliente ON enderecos(cliente_id);
CREATE INDEX idx_enderecos_restaurante ON enderecos(restaurante_id);
CREATE INDEX idx_enderecos_principal ON enderecos(principal);
CREATE INDEX idx_pedidos_endereco_entrega ON pedidos(endereco_entrega_id);


-- Migração V10: Adicionar campos de latitude/longitude para cálculo de distância e rastreamento

-- Adicionar coordenadas aos restaurantes
ALTER TABLE restaurantes
ADD COLUMN latitude DECIMAL(10, 8),
ADD COLUMN longitude DECIMAL(11, 8);

-- Adicionar coordenadas aos clientes
ALTER TABLE clientes
ADD COLUMN latitude DECIMAL(10, 8),
ADD COLUMN longitude DECIMAL(11, 8);

-- Adicionar coordenadas do endereço de entrega aos pedidos
ALTER TABLE pedidos
ADD COLUMN latitude_entrega DECIMAL(10, 8),
ADD COLUMN longitude_entrega DECIMAL(11, 8);

-- Criar índices para otimizar consultas de proximidade
CREATE INDEX idx_restaurantes_localizacao ON restaurantes(latitude, longitude);
CREATE INDEX idx_clientes_localizacao ON clientes(latitude, longitude);
CREATE INDEX idx_pedidos_localizacao_entrega ON pedidos(latitude_entrega, longitude_entrega);


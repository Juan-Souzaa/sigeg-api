ALTER TABLE pedidos
ADD COLUMN taxa_plataforma_restaurante DECIMAL(10,2) NULL,
ADD COLUMN taxa_plataforma_entregador DECIMAL(10,2) NULL,
ADD COLUMN valor_liquido_restaurante DECIMAL(10,2) NULL,
ADD COLUMN valor_liquido_entregador DECIMAL(10,2) NULL;


-- Migração V19: Adicionar coluna ativo para soft delete em clientes e restaurantes

-- Adicionar coluna ativo em clientes
ALTER TABLE clientes ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;

-- Adicionar coluna ativo em restaurantes
ALTER TABLE restaurantes ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;


-- Migração V7: Adicionar coluna user_id na tabela restaurantes para controle de ownership
ALTER TABLE restaurantes 
ADD COLUMN user_id BIGINT,
ADD CONSTRAINT fk_restaurantes_user FOREIGN KEY (user_id) REFERENCES users(id);

CREATE INDEX idx_restaurantes_user ON restaurantes(user_id);


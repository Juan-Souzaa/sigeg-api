CREATE TABLE configuracoes_taxa (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo_taxa VARCHAR(50) NOT NULL,
    percentual DECIMAL(5,2) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_tipo_taxa CHECK (tipo_taxa IN ('TAXA_RESTAURANTE', 'TAXA_ENTREGADOR')),
    CONSTRAINT chk_percentual CHECK (percentual >= 0 AND percentual <= 100)
);

CREATE INDEX idx_configuracoes_taxa_tipo_ativo ON configuracoes_taxa(tipo_taxa, ativo);
CREATE INDEX idx_configuracoes_taxa_criado_em ON configuracoes_taxa(criado_em DESC);

-- Inserir taxas padrão
INSERT INTO configuracoes_taxa (tipo_taxa, percentual, ativo, criado_em, atualizado_em)
VALUES ('TAXA_RESTAURANTE', 10.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO configuracoes_taxa (tipo_taxa, percentual, ativo, criado_em, atualizado_em)
VALUES ('TAXA_ENTREGADOR', 15.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


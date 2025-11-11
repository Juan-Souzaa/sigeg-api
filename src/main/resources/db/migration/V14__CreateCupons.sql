CREATE TABLE cupons (
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

CREATE INDEX idx_cupons_codigo ON cupons(codigo);
CREATE INDEX idx_cupons_ativo ON cupons(ativo);
CREATE INDEX idx_cupons_data_validade ON cupons(data_inicio, data_fim);


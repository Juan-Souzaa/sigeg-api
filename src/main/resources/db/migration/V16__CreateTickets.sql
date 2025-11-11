CREATE TABLE tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(200) NOT NULL,
    descricao TEXT NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    prioridade VARCHAR(50) NOT NULL,
    criado_por_id BIGINT NOT NULL,
    atribuido_a_id BIGINT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NULL,
    resolvido_em TIMESTAMP NULL,
    CONSTRAINT fk_ticket_criado_por FOREIGN KEY (criado_por_id) REFERENCES users(id),
    CONSTRAINT fk_ticket_atribuido_a FOREIGN KEY (atribuido_a_id) REFERENCES users(id),
    CONSTRAINT chk_tipo_ticket CHECK (tipo IN ('RECLAMACAO', 'SUPORTE_TECNICO', 'SUGESTAO')),
    CONSTRAINT chk_status_ticket CHECK (status IN ('ABERTO', 'EM_ANDAMENTO', 'RESOLVIDO', 'FECHADO')),
    CONSTRAINT chk_prioridade_ticket CHECK (prioridade IN ('BAIXA', 'MEDIA', 'ALTA', 'URGENTE'))
);

CREATE TABLE ticket_comentarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    autor_id BIGINT NOT NULL,
    comentario TEXT NOT NULL,
    interno BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comentario_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_comentario_autor FOREIGN KEY (autor_id) REFERENCES users(id)
);

CREATE INDEX idx_tickets_criado_por ON tickets(criado_por_id);
CREATE INDEX idx_tickets_atribuido_a ON tickets(atribuido_a_id);
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_tipo ON tickets(tipo);
CREATE INDEX idx_tickets_prioridade ON tickets(prioridade);
CREATE INDEX idx_tickets_criado_em ON tickets(criado_em DESC);

CREATE INDEX idx_ticket_comentarios_ticket ON ticket_comentarios(ticket_id);
CREATE INDEX idx_ticket_comentarios_autor ON ticket_comentarios(autor_id);
CREATE INDEX idx_ticket_comentarios_criado_em ON ticket_comentarios(criado_em ASC);


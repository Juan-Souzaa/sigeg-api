-- Migração V20: Adicionar campos de reembolso na tabela pagamentos
ALTER TABLE pagamentos
    ADD COLUMN valor_reembolsado DECIMAL(10,2) NULL,
    ADD COLUMN data_reembolso TIMESTAMP NULL,
    ADD COLUMN asaas_refund_id VARCHAR(255) NULL;


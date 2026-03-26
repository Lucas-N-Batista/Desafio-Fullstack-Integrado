-- ============================================================
-- Schema do banco de dados - Desafio Fullstack Integrado
-- Compatível com PostgreSQL 15+
-- ============================================================

-- Remove tabela existente para re-criação limpa (útil em dev)
DROP TABLE IF EXISTS BENEFICIO;

-- Tabela principal de benefícios
CREATE TABLE BENEFICIO (
    ID          BIGSERIAL PRIMARY KEY,
    NOME        VARCHAR(100)   NOT NULL,
    DESCRICAO   VARCHAR(255),
    VALOR       DECIMAL(15, 2) NOT NULL CHECK (VALOR >= 0),
    ATIVO       BOOLEAN        NOT NULL DEFAULT TRUE,
    VERSION     BIGINT         NOT NULL DEFAULT 0,  -- campo para Optimistic Locking (EJB @Version)
    CREATED_AT  TIMESTAMP      NOT NULL DEFAULT NOW(),
    UPDATED_AT  TIMESTAMP      NOT NULL DEFAULT NOW()
);

-- Índice para buscas por nome
CREATE INDEX idx_beneficio_nome ON BENEFICIO (NOME);

-- Índice para filtros por ativo
CREATE INDEX idx_beneficio_ativo ON BENEFICIO (ATIVO);

-- Comentários na tabela
COMMENT ON TABLE  BENEFICIO           IS 'Entidade de Benefício para transferência de saldo';
COMMENT ON COLUMN BENEFICIO.VERSION   IS 'Versão para controle de Optimistic Locking (JPA @Version)';
COMMENT ON COLUMN BENEFICIO.VALOR     IS 'Saldo atual do benefício (nunca negativo)';

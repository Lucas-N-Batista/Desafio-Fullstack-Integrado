-- ============================================================
-- Dados iniciais (seed) - Desafio Fullstack Integrado
-- Execute APÓS db/schema.sql
-- ============================================================

-- Limpa dados existentes (seguro para re-seed em dev)
TRUNCATE TABLE BENEFICIO RESTART IDENTITY CASCADE;

-- Benefícios de exemplo com saldos variados para testar transferências
INSERT INTO BENEFICIO (NOME, DESCRICAO, VALOR, ATIVO, VERSION) VALUES
    ('Vale Alimentação',   'Benefício de alimentação mensal',          1500.00, TRUE,  0),
    ('Vale Refeição',      'Benefício de refeição diária',             800.00,  TRUE,  0),
    ('Vale Transporte',    'Benefício de transporte público',          350.00,  TRUE,  0),
    ('Plano de Saúde',     'Cobertura médica e hospitalar completa',   2500.00, TRUE,  0),
    ('Auxílio Creche',     'Benefício de auxílio para filhos(as)',     600.00,  TRUE,  0),
    ('Seguro de Vida',     'Apólice de seguro de vida corporativo',    0.00,    TRUE,  0),
    ('Gympass',            'Acesso a academias parceiras',             200.00,  FALSE, 0),
    ('PLR',                'Participação nos lucros e resultados',     5000.00, TRUE,  0);

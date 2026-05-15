-- V6__Integridade_projeto.sql
-- Reforça integridade referencial: NOT NULL nos FKs obrigatórios e índices nas colunas FK.

-- Backfill: projetos legados (criados antes do Sprint 8) sem equipe recebem a primeira equipe disponível.
UPDATE projeto
SET equipe_id = (SELECT id FROM equipe ORDER BY nome LIMIT 1)
WHERE equipe_id IS NULL;

-- Backfill: projetos sem gerente recebem o primeiro gerente disponível (defensivo).
UPDATE projeto
SET gerente_id = (SELECT id FROM usuario WHERE perfil = 'GERENTE' ORDER BY nome LIMIT 1)
WHERE gerente_id IS NULL;

ALTER TABLE projeto ALTER COLUMN gerente_id SET NOT NULL;
ALTER TABLE projeto ALTER COLUMN equipe_id SET NOT NULL;

CREATE INDEX idx_projeto_equipe_id     ON projeto(equipe_id);
CREATE INDEX idx_projeto_gerente_id    ON projeto(gerente_id);
CREATE INDEX idx_tarefa_projeto_id     ON tarefa(projeto_id);
CREATE INDEX idx_tarefa_responsavel_id ON tarefa(responsavel_id);

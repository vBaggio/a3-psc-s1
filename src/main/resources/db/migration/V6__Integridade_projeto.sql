-- V6__Integridade_projeto.sql
-- Reforça integridade referencial: NOT NULL nos FKs obrigatórios e índices nas colunas FK.

ALTER TABLE projeto ALTER COLUMN gerente_id SET NOT NULL;
ALTER TABLE projeto ALTER COLUMN equipe_id SET NOT NULL;

CREATE INDEX idx_projeto_equipe_id    ON projeto(equipe_id);
CREATE INDEX idx_projeto_gerente_id   ON projeto(gerente_id);
CREATE INDEX idx_tarefa_projeto_id    ON tarefa(projeto_id);
CREATE INDEX idx_tarefa_responsavel_id ON tarefa(responsavel_id);

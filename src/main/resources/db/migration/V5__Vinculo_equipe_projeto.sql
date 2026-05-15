-- V5__Vinculo_equipe_projeto.sql
-- Corrige o modelo ManyToMany (equipe_projeto) para ManyToOne (equipe_id em projeto).

ALTER TABLE projeto ADD COLUMN equipe_id UUID REFERENCES equipe(id);

DROP TABLE equipe_projeto;

-- Sprint 4: adiciona tabela de tarefas do projeto
CREATE TABLE tarefa (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome           VARCHAR(150) NOT NULL,
    descricao      TEXT,
    prazo          DATE,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    projeto_id     UUID NOT NULL REFERENCES projeto(id),
    responsavel_id UUID REFERENCES usuario(id)
);

-- ============================================================
-- V1__Create_schema.sql
-- Schema inicial do Sistema de Gerenciamento de Projetos
-- ============================================================

CREATE TABLE cargo (
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE usuario (
    id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome     VARCHAR(150) NOT NULL,
    cpf      VARCHAR(11)  NOT NULL UNIQUE,
    email    VARCHAR(150) NOT NULL UNIQUE,
    login    VARCHAR(50)  NOT NULL UNIQUE,
    senha    VARCHAR(255) NOT NULL,
    perfil   VARCHAR(20)  NOT NULL,
    cargo_id UUID REFERENCES cargo(id)
);

CREATE TABLE projeto (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome          VARCHAR(150) NOT NULL,
    descricao     TEXT,
    data_inicio   DATE,
    data_previsao DATE,
    data_fim      DATE,
    status        VARCHAR(20) NOT NULL,
    gerente_id    UUID REFERENCES usuario(id)
);

CREATE TABLE equipe (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome      VARCHAR(150) NOT NULL,
    descricao TEXT
);

CREATE TABLE equipe_membro (
    equipe_id  UUID NOT NULL REFERENCES equipe(id),
    usuario_id UUID NOT NULL REFERENCES usuario(id),
    PRIMARY KEY (equipe_id, usuario_id)
);

CREATE TABLE equipe_projeto (
    equipe_id  UUID NOT NULL REFERENCES equipe(id),
    projeto_id UUID NOT NULL REFERENCES projeto(id),
    PRIMARY KEY (equipe_id, projeto_id)
);

-- ============================================================
-- V3__Seed_admin.sql
-- Cargos pré-cadastrados e usuário administrador padrão
-- login: admin | senha: 123
-- ============================================================

-- Cargos pré-cadastrados
INSERT INTO cargo (id, nome) VALUES
    ('cc000001-0000-0000-0000-000000000000', 'Sysadmin'),
    ('cc000002-0000-0000-0000-000000000000', 'Coordenador de Projetos'),
    ('cc000003-0000-0000-0000-000000000000', 'Desenvolvedor'),
    ('cc000004-0000-0000-0000-000000000000', 'Líder Técnico');

-- Usuário administrador padrão para primeiro acesso
INSERT INTO usuario (id, nome, cpf, email, login, senha, perfil, cargo_id)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Administrador',
    '00000000000',
    'admin@sistema.local',
    'admin',
    '$2a$10$hj2UvKMFYHSEg07bMcn2JuZkT/xKXXkvRtVBPeXcFTICqIT7sRNxi',
    'ADMINISTRADOR',
    'cc000001-0000-0000-0000-000000000000'
);

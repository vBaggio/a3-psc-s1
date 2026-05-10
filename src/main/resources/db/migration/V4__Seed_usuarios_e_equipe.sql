-- ============================================================
-- V4__Seed_usuarios_e_equipe.sql
-- Usuários padrão (um por perfil) e equipe de demonstração
-- login: gerente | senha: 123
-- login: usuario | senha: 123
-- ============================================================

INSERT INTO usuario (id, nome, cpf, email, login, senha, perfil, cargo_id)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Gerente Padrão',
    '11111111111',
    'gerente@sistema.local',
    'gerente',
    '$2a$10$hj2UvKMFYHSEg07bMcn2JuZkT/xKXXkvRtVBPeXcFTICqIT7sRNxi',
    'GERENTE',
    NULL
);

INSERT INTO usuario (id, nome, cpf, email, login, senha, perfil, cargo_id)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    'Usuário Padrão',
    '22222222222',
    'usuario@sistema.local',
    'usuario',
    '$2a$10$hj2UvKMFYHSEg07bMcn2JuZkT/xKXXkvRtVBPeXcFTICqIT7sRNxi',
    'COLABORADOR',
    NULL
);

INSERT INTO equipe (id, nome, descricao)
VALUES (
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
    'Equipe Padrão',
    'Equipe criada automaticamente com os usuários padrão do sistema.'
);

INSERT INTO equipe_membro (equipe_id, usuario_id)
VALUES (
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
    (SELECT id FROM usuario WHERE login = 'admin')
);

INSERT INTO equipe_membro (equipe_id, usuario_id)
VALUES (
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
    '11111111-1111-1111-1111-111111111111'
);

INSERT INTO equipe_membro (equipe_id, usuario_id)
VALUES (
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
    '22222222-2222-2222-2222-222222222222'
);

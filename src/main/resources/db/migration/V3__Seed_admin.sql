-- Usuário administrador padrão para primeiro acesso
-- login: admin | senha: 123
INSERT INTO usuario (id, nome, cpf, email, login, senha, perfil, cargo_id)
VALUES (
    gen_random_uuid(),
    'Administrador',
    '00000000000',
    'admin@sistema.local',
    'admin',
    '$2a$10$hj2UvKMFYHSEg07bMcn2JuZkT/xKXXkvRtVBPeXcFTICqIT7sRNxi',
    'ADMINISTRADOR',
    NULL
);

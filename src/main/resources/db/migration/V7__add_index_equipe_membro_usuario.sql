-- ============================================================
-- V7__add_index_equipe_membro_usuario.sql
-- Índice de performance para consultas de projetos visíveis por usuário
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_equipe_membro_usuario
    ON equipe_membro(usuario_id);

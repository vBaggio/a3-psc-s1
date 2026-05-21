package com.vbaggio.projectapp.repository;

import com.vbaggio.projectapp.repository.JpaUtil;
import com.vbaggio.projectapp.model.entity.Projeto;
import com.vbaggio.projectapp.model.enums.StatusProjeto;
import jakarta.persistence.EntityManager;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository responsável pelas operações de persistência da entidade {@link Projeto}.
 */
public class ProjetoRepository {

    /**
     * Persiste um novo Projeto no banco de dados.
     *
     * @param projeto entidade a ser salva
     */
    public void salvar(Projeto projeto) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(projeto);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Busca um Projeto pelo seu identificador único.
     *
     * @param id UUID do projeto
     * @return Optional com o projeto, ou vazio se não encontrado
     */
    public Optional<Projeto> buscarPorId(UUID id) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            List<Projeto> result = em.createQuery(
                    "SELECT p FROM Projeto p LEFT JOIN FETCH p.gerente LEFT JOIN FETCH p.equipe WHERE p.id = :id",
                    Projeto.class)
                    .setParameter("id", id)
                    .getResultList();
            return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
        } finally {
            em.close();
        }
    }

    /**
     * Retorna todos os projetos cadastrados, ordenados pelo nome.
     *
     * @return lista de projetos
     */
    public List<Projeto> listarTodos() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT p FROM Projeto p LEFT JOIN FETCH p.gerente LEFT JOIN FETCH p.equipe ORDER BY p.nome",
                            Projeto.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Retorna todos os projetos visíveis para um dado usuário:
     * aqueles em que ele é o gerente ou membro da equipe associada.
     *
     * @param usuarioId UUID do usuário
     * @return lista de projetos visíveis, ordenada pelo nome
     */
    public List<Projeto> listarPorMembroOuGerente(UUID usuarioId) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                "SELECT DISTINCT p FROM Projeto p " +
                "LEFT JOIN FETCH p.gerente " +
                "LEFT JOIN FETCH p.equipe eq " +
                "LEFT JOIN eq.membros m " +
                "WHERE p.gerente.id = :usuarioId OR m.id = :usuarioId " +
                "ORDER BY p.nome",
                Projeto.class)
                .setParameter("usuarioId", usuarioId)
                .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Verifica se existe pelo menos um projeto ativo (PLANEJADO ou EM_ANDAMENTO)
     * com o gerente especificado. Usado para impedir a remoção de gerentes vinculados.
     *
     * @param gerenteId UUID do gerente
     * @return true se houver projetos ativos gerenciados por ele
     */
    public boolean existeProjetoAtivoComGerente(UUID gerenteId) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(p) FROM Projeto p " +
                "WHERE p.gerente.id = :gerenteId " +
                "AND p.status IN (:s1, :s2)",
                Long.class)
                .setParameter("gerenteId", gerenteId)
                .setParameter("s1", StatusProjeto.PLANEJADO)
                .setParameter("s2", StatusProjeto.EM_ANDAMENTO)
                .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }

    /**
     * Filtra projetos por status.
     *
     * @param status status desejado (PLANEJADO, EM_ANDAMENTO, CONCLUIDO, CANCELADO)
     * @return lista de projetos com o status informado
     */
    public List<Projeto> listarPorStatus(StatusProjeto status) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT p FROM Projeto p LEFT JOIN FETCH p.gerente LEFT JOIN FETCH p.equipe WHERE p.status = :status ORDER BY p.nome",
                            Projeto.class)
                    .setParameter("status", status)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Retorna todos os projetos gerenciados por um determinado usuário.
     *
     * @param gerenteId UUID do gerente responsável
     * @return lista de projetos do gerente
     */
    public List<Projeto> listarPorGerente(UUID gerenteId) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT p FROM Projeto p LEFT JOIN FETCH p.gerente LEFT JOIN FETCH p.equipe " +
                    "WHERE p.gerente.id = :gerenteId ORDER BY p.nome",
                    Projeto.class)
                    .setParameter("gerenteId", gerenteId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Atualiza os dados de um Projeto já existente.
     *
     * @param projeto entidade com dados atualizados (id obrigatório)
     * @return entidade gerenciada após o merge
     */
    public Projeto atualizar(Projeto projeto) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Projeto atualizado = em.merge(projeto);
            em.getTransaction().commit();
            return atualizado;
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Remove um Projeto pelo seu id.
     *
     * @param id UUID do projeto a ser removido
     */
    public void deletar(UUID id) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Projeto projeto = em.find(Projeto.class, id);
            if (projeto != null) {
                em.remove(projeto);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Retorna a contagem de projetos agrupada por status sem hidratar entidades completas.
     */
    @SuppressWarnings("unchecked")
    public Map<StatusProjeto, Long> contarPorStatus() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            List<Object[]> linhas = em.createQuery(
                            "SELECT p.status, COUNT(p) FROM Projeto p GROUP BY p.status")
                    .getResultList();
            Map<StatusProjeto, Long> resultado = new EnumMap<>(StatusProjeto.class);
            for (StatusProjeto s : StatusProjeto.values()) resultado.put(s, 0L);
            for (Object[] linha : linhas) {
                resultado.put((StatusProjeto) linha[0], (Long) linha[1]);
            }
            return resultado;
        } finally {
            em.close();
        }
    }
}

package com.vbaggio.projectapp.repository;

import com.vbaggio.projectapp.infra.JpaUtil;
import com.vbaggio.projectapp.model.entity.Tarefa;
import com.vbaggio.projectapp.model.enums.StatusTarefa;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository responsável pelas operações de persistência da entidade {@link Tarefa}.
 */
public class TarefaRepository {

    /**
     * Persiste uma nova Tarefa no banco de dados.
     *
     * @param tarefa entidade a ser salva
     */
    public void salvar(Tarefa tarefa) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(tarefa);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Busca uma Tarefa pelo seu identificador único.
     *
     * @param id UUID da tarefa
     * @return Optional com a tarefa, ou vazio se não encontrada
     */
    public Optional<Tarefa> buscarPorId(UUID id) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return Optional.ofNullable(em.find(Tarefa.class, id));
        } finally {
            em.close();
        }
    }

    /**
     * Retorna todas as tarefas cadastradas, ordenadas pelo nome.
     *
     * @return lista de tarefas
     */
    public List<Tarefa> listarTodos() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT t FROM Tarefa t ORDER BY t.nome", Tarefa.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Retorna todas as tarefas vinculadas a um projeto, ordenadas por prazo (nulos por último) e nome.
     *
     * @param projetoId UUID do projeto
     * @return lista de tarefas do projeto
     */
    public List<Tarefa> listarPorProjeto(UUID projetoId) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT t FROM Tarefa t WHERE t.projeto.id = :projetoId ORDER BY t.prazo ASC NULLS LAST, t.nome",
                            Tarefa.class)
                    .setParameter("projetoId", projetoId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Retorna todas as tarefas atribuídas a um usuário responsável, ordenadas por prazo (nulos por último).
     *
     * @param usuarioId UUID do usuário responsável
     * @return lista de tarefas do responsável
     */
    public List<Tarefa> listarPorResponsavel(UUID usuarioId) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT t FROM Tarefa t WHERE t.responsavel.id = :usuarioId ORDER BY t.prazo ASC NULLS LAST",
                            Tarefa.class)
                    .setParameter("usuarioId", usuarioId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Filtra tarefas por status, ordenadas por prazo (nulos por último).
     *
     * @param status status desejado (PENDENTE, EM_ANDAMENTO, CONCLUIDA, CANCELADA)
     * @return lista de tarefas com o status informado
     */
    public List<Tarefa> listarPorStatus(StatusTarefa status) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT t FROM Tarefa t WHERE t.status = :status ORDER BY t.prazo ASC NULLS LAST",
                            Tarefa.class)
                    .setParameter("status", status)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Atualiza os dados de uma Tarefa já existente.
     *
     * @param tarefa entidade com dados atualizados (id obrigatório)
     * @return entidade gerenciada após o merge
     */
    public Tarefa atualizar(Tarefa tarefa) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Tarefa atualizada = em.merge(tarefa);
            em.getTransaction().commit();
            return atualizada;
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Remove uma Tarefa pelo seu id.
     *
     * @param id UUID da tarefa a ser removida
     */
    public void deletar(UUID id) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Tarefa tarefa = em.find(Tarefa.class, id);
            if (tarefa != null) {
                em.remove(tarefa);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}

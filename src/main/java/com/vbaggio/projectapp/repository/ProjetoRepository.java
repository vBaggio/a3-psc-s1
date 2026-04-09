package com.vbaggio.projectapp.repository;

import com.vbaggio.projectapp.infra.JpaUtil;
import com.vbaggio.projectapp.model.entity.Projeto;
import com.vbaggio.projectapp.model.enums.StatusProjeto;
import jakarta.persistence.EntityManager;

import java.util.List;
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
            return Optional.ofNullable(em.find(Projeto.class, id));
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
                            "SELECT p FROM Projeto p ORDER BY p.nome", Projeto.class)
                    .getResultList();
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
                            "SELECT p FROM Projeto p WHERE p.status = :status ORDER BY p.nome",
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
                            "SELECT p FROM Projeto p WHERE p.gerente.id = :gerenteId ORDER BY p.nome",
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
}

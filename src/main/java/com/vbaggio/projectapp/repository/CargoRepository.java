package com.vbaggio.projectapp.repository;

import com.vbaggio.projectapp.infra.JpaUtil;
import com.vbaggio.projectapp.model.entity.Cargo;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository responsável pelas operações de persistência da entidade {@link Cargo}.
 */
public class CargoRepository {

    /**
     * Persiste um novo Cargo no banco de dados.
     *
     * @param cargo entidade a ser salva (id gerado automaticamente pelo JPA)
     */
    public void salvar(Cargo cargo) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(cargo);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Busca um Cargo pelo seu identificador único.
     *
     * @param id UUID do cargo
     * @return Optional contendo o Cargo, ou vazio se não encontrado
     */
    public Optional<Cargo> buscarPorId(UUID id) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return Optional.ofNullable(em.find(Cargo.class, id));
        } finally {
            em.close();
        }
    }

    /**
     * Busca um Cargo pelo nome (case-insensitive).
     *
     * @param nome nome do cargo
     * @return Optional com o Cargo encontrado, ou vazio
     */
    public Optional<Cargo> buscarPorNome(String nome) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT c FROM Cargo c WHERE LOWER(c.nome) = LOWER(:nome)", Cargo.class)
                    .setParameter("nome", nome)
                    .getResultStream()
                    .findFirst();
        } finally {
            em.close();
        }
    }

    /**
     * Retorna todos os cargos cadastrados, ordenados pelo nome.
     *
     * @return lista de cargos
     */
    public List<Cargo> listarTodos() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery("SELECT c FROM Cargo c ORDER BY c.nome", Cargo.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Atualiza os dados de um Cargo já existente.
     *
     * @param cargo entidade com dados atualizados (deve ter id preenchido)
     * @return entidade gerenciada após o merge
     */
    public Cargo atualizar(Cargo cargo) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Cargo atualizado = em.merge(cargo);
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
     * Remove um Cargo pelo seu id.
     *
     * @param id UUID do cargo a ser removido
     */
    public void deletar(UUID id) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Cargo cargo = em.find(Cargo.class, id);
            if (cargo != null) {
                em.remove(cargo);
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

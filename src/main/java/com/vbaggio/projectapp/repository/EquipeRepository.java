package com.vbaggio.projectapp.repository;

import com.vbaggio.projectapp.infra.JpaUtil;
import com.vbaggio.projectapp.model.entity.Equipe;
import com.vbaggio.projectapp.model.entity.Projeto;
import com.vbaggio.projectapp.model.entity.Usuario;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository responsável pelas operações de persistência da entidade {@link Equipe},
 * incluindo o gerenciamento de membros e projetos associados.
 */
public class EquipeRepository {

    /**
     * Persiste uma nova Equipe no banco de dados.
     *
     * @param equipe entidade a ser salva
     */
    public void salvar(Equipe equipe) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(equipe);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Busca uma Equipe pelo seu identificador único.
     *
     * @param id UUID da equipe
     * @return Optional com a equipe, ou vazio se não encontrada
     */
    public Optional<Equipe> buscarPorId(UUID id) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return Optional.ofNullable(em.find(Equipe.class, id));
        } finally {
            em.close();
        }
    }

    /**
     * Retorna todas as equipes cadastradas, ordenadas pelo nome.
     *
     * @return lista de equipes
     */
    public List<Equipe> listarTodos() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery("SELECT e FROM Equipe e ORDER BY e.nome", Equipe.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Adiciona um {@link Usuario} como membro de uma {@link Equipe}.
     * Garante que a relação @ManyToMany seja persistida na tabela {@code equipe_membro}.
     *
     * @param equipeId  UUID da equipe
     * @param usuarioId UUID do usuário a ser adicionado
     */
    public void adicionarMembro(UUID equipeId, UUID usuarioId) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Equipe equipe   = em.find(Equipe.class, equipeId);
            Usuario usuario = em.find(Usuario.class, usuarioId);
            if (equipe != null && usuario != null && !equipe.getMembros().contains(usuario)) {
                equipe.addMembro(usuario);
                em.merge(equipe);
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
     * Remove um {@link Usuario} da lista de membros de uma {@link Equipe}.
     *
     * @param equipeId  UUID da equipe
     * @param usuarioId UUID do usuário a ser removido
     */
    public void removerMembro(UUID equipeId, UUID usuarioId) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Equipe equipe = em.find(Equipe.class, equipeId);
            if (equipe != null) {
                equipe.getMembros().removeIf(u -> u.getId().equals(usuarioId));
                em.merge(equipe);
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
     * Vincula uma {@link Equipe} a um {@link Projeto}.
     * Persiste o relacionamento na tabela {@code equipe_projeto}.
     *
     * @param equipeId  UUID da equipe
     * @param projetoId UUID do projeto
     */
    public void adicionarProjeto(UUID equipeId, UUID projetoId) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Equipe equipe   = em.find(Equipe.class, equipeId);
            Projeto projeto = em.find(Projeto.class, projetoId);
            if (equipe != null && projeto != null && !equipe.getProjetos().contains(projeto)) {
                equipe.addProjeto(projeto);
                em.merge(equipe);
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
     * Retorna a lista de membros de uma equipe.
     *
     * @param equipeId UUID da equipe
     * @return lista de usuários membros, ou lista vazia se a equipe não existir
     */
    public List<Usuario> listarMembrosDaEquipe(UUID equipeId) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT u FROM Equipe e JOIN e.membros u WHERE e.id = :equipeId",
                            Usuario.class)
                    .setParameter("equipeId", equipeId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Atualiza os dados de uma Equipe já existente (nome e descrição).
     *
     * @param equipe entidade com dados atualizados (id obrigatório)
     * @return entidade gerenciada após o merge
     */
    public Equipe atualizar(Equipe equipe) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Equipe atualizada = em.merge(equipe);
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
     * Remove uma Equipe pelo seu id.
     *
     * @param id UUID da equipe a ser removida
     */
    public void deletar(UUID id) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Equipe equipe = em.find(Equipe.class, id);
            if (equipe != null) {
                em.remove(equipe);
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

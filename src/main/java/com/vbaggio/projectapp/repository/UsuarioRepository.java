package com.vbaggio.projectapp.repository;

import com.vbaggio.projectapp.infra.JpaUtil;
import com.vbaggio.projectapp.model.entity.Usuario;
import com.vbaggio.projectapp.model.enums.Perfil;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository responsável pelas operações de persistência da entidade {@link Usuario}.
 */
public class UsuarioRepository {

    /**
     * Persiste um novo Usuário no banco de dados.
     *
     * @param usuario entidade a ser salva
     */
    public void salvar(Usuario usuario) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(usuario);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Busca um Usuário pelo seu identificador único.
     *
     * @param id UUID do usuário
     * @return Optional com o usuário, ou vazio se não encontrado
     */
    public Optional<Usuario> buscarPorId(UUID id) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return Optional.ofNullable(em.find(Usuario.class, id));
        } finally {
            em.close();
        }
    }

    /**
     * Busca um Usuário pelo login — usado no fluxo de autenticação.
     *
     * @param login login do usuário
     * @return Optional com o usuário, ou vazio se não encontrado
     */
    public Optional<Usuario> buscarPorLogin(String login) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT u FROM Usuario u WHERE u.login = :login", Usuario.class)
                    .setParameter("login", login)
                    .getResultStream()
                    .findFirst();
        } finally {
            em.close();
        }
    }

    /**
     * Busca um Usuário pelo e-mail — usado para validação de unicidade.
     *
     * @param email e-mail do usuário
     * @return Optional com o usuário, ou vazio se não encontrado
     */
    public Optional<Usuario> buscarPorEmail(String email) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT u FROM Usuario u WHERE LOWER(u.email) = LOWER(:email)", Usuario.class)
                    .setParameter("email", email)
                    .getResultStream()
                    .findFirst();
        } finally {
            em.close();
        }
    }

    /**
     * Busca um Usuário pelo CPF — usado para validação de unicidade.
     *
     * @param cpf CPF sem formatação (11 dígitos)
     * @return Optional com o usuário, ou vazio se não encontrado
     */
    public Optional<Usuario> buscarPorCpf(String cpf) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT u FROM Usuario u WHERE u.cpf = :cpf", Usuario.class)
                    .setParameter("cpf", cpf)
                    .getResultStream()
                    .findFirst();
        } finally {
            em.close();
        }
    }

    /**
     * Retorna todos os usuários cadastrados, ordenados pelo nome.
     *
     * @return lista de usuários
     */
    public List<Usuario> listarTodos() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT u FROM Usuario u LEFT JOIN FETCH u.cargo ORDER BY u.nome",
                            Usuario.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Filtra usuários por perfil de acesso.
     *
     * @param perfil perfil desejado (ADMINISTRADOR, GERENTE ou COLABORADOR)
     * @return lista de usuários com o perfil informado
     */
    public List<Usuario> listarPorPerfil(Perfil perfil) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT u FROM Usuario u LEFT JOIN FETCH u.cargo WHERE u.perfil = :perfil ORDER BY u.nome",
                            Usuario.class)
                    .setParameter("perfil", perfil)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Atualiza os dados de um Usuário já existente.
     *
     * @param usuario entidade com dados atualizados (id obrigatório)
     * @return entidade gerenciada após o merge
     */
    public Usuario atualizar(Usuario usuario) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Usuario atualizado = em.merge(usuario);
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
     * Remove um Usuário pelo seu id.
     *
     * @param id UUID do usuário a ser removido
     */
    public void deletar(UUID id) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Usuario usuario = em.find(Usuario.class, id);
            if (usuario != null) {
                em.remove(usuario);
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

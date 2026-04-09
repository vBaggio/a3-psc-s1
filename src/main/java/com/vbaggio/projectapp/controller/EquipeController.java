package com.vbaggio.projectapp.controller;

import com.vbaggio.projectapp.model.entity.Equipe;
import com.vbaggio.projectapp.model.entity.Usuario;
import com.vbaggio.projectapp.repository.EquipeRepository;
import com.vbaggio.projectapp.repository.UsuarioRepository;

import java.util.List;
import java.util.UUID;

/**
 * Controller responsável pelas regras de negócio da entidade {@link Equipe}.
 *
 * <p>Gerencia o ciclo de vida das equipes, incluindo composição de membros
 * e alocação em projetos com as devidas validações de negócio.</p>
 */
public class EquipeController {

    private final EquipeRepository  equipeRepo;
    private final UsuarioRepository usuarioRepo;

    public EquipeController() {
        this.equipeRepo  = new EquipeRepository();
        this.usuarioRepo = new UsuarioRepository();
    }

    /**
     * Cria uma nova equipe.
     *
     * <p>Regra: nome obrigatório e não pode ser duplicado.</p>
     *
     * @param nome      nome da equipe
     * @param descricao descrição (pode ser null)
     * @return entidade Equipe persistida
     */
    public Equipe criarEquipe(String nome, String descricao) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome da equipe é obrigatório.");
        }

        boolean nomeEmUso = equipeRepo.listarTodos().stream()
                .anyMatch(e -> e.getNome().equalsIgnoreCase(nome.trim()));
        if (nomeEmUso) {
            throw new IllegalArgumentException("Já existe uma equipe com o nome '" + nome + "'.");
        }

        Equipe equipe = new Equipe();
        equipe.setNome(nome.trim());
        equipe.setDescricao(descricao);
        equipeRepo.salvar(equipe);
        return equipe;
    }

    /**
     * Adiciona um usuário como membro de uma equipe.
     *
     * <p>Regra: o usuário deve existir; duplicatas são silenciosamente ignoradas pelo Repository.</p>
     *
     * @param equipeId  UUID da equipe
     * @param usuarioId UUID do usuário
     */
    public void adicionarMembro(UUID equipeId, UUID usuarioId) {
        buscarPorId(equipeId); // valida existência da equipe

        usuarioRepo.buscarPorId(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + usuarioId));

        equipeRepo.adicionarMembro(equipeId, usuarioId);
    }

    /**
     * Remove um membro de uma equipe.
     *
     * <p>Regra: não é possível remover o último membro de uma equipe
     * que já está alocada em algum projeto ativo.</p>
     *
     * @param equipeId  UUID da equipe
     * @param usuarioId UUID do usuário a ser removido
     */
    public void removerMembro(UUID equipeId, UUID usuarioId) {
        buscarPorId(equipeId);

        List<Usuario> membrosAtuais = equipeRepo.listarMembrosDaEquipe(equipeId);
        boolean ultimoMembro = membrosAtuais.size() == 1
                && membrosAtuais.get(0).getId().equals(usuarioId);

        if (ultimoMembro) {
            Equipe equipe = buscarPorId(equipeId);
            if (!equipe.getProjetos().isEmpty()) {
                throw new IllegalStateException(
                        "Não é possível remover o último membro de uma equipe alocada em projeto(s)."
                );
            }
        }

        equipeRepo.removerMembro(equipeId, usuarioId);
    }

    /**
     * Retorna todos os membros de uma equipe.
     *
     * @param equipeId UUID da equipe
     * @return lista de usuários membros
     */
    public List<Usuario> listarMembros(UUID equipeId) {
        buscarPorId(equipeId);
        return equipeRepo.listarMembrosDaEquipe(equipeId);
    }

    /**
     * Retorna todas as equipes cadastradas.
     *
     * @return lista de equipes ordenada por nome
     */
    public List<Equipe> listarEquipes() {
        return equipeRepo.listarTodos();
    }

    /**
     * Busca uma equipe pelo id.
     *
     * @param id UUID da equipe
     * @return entidade Equipe encontrada
     * @throws IllegalArgumentException se não encontrada
     */
    public Equipe buscarPorId(UUID id) {
        return equipeRepo.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Equipe não encontrada: " + id));
    }

    /**
     * Atualiza nome e descrição de uma equipe.
     *
     * @param id        UUID da equipe
     * @param novoNome  novo nome (null = mantém atual)
     * @param descricao nova descrição (null = mantém atual)
     * @return equipe atualizada
     */
    public Equipe atualizarEquipe(UUID id, String novoNome, String descricao) {
        Equipe equipe = buscarPorId(id);

        if (novoNome != null && !novoNome.isBlank()) {
            equipe.setNome(novoNome.trim());
        }
        if (descricao != null) {
            equipe.setDescricao(descricao);
        }

        return equipeRepo.atualizar(equipe);
    }

    /**
     * Remove uma equipe pelo id.
     *
     * <p>Regra: equipes alocadas em projetos não podem ser removidas.</p>
     *
     * @param id UUID da equipe
     */
    public void removerEquipe(UUID id) {
        Equipe equipe = buscarPorId(id);

        if (!equipe.getProjetos().isEmpty()) {
            throw new IllegalStateException(
                    "Não é possível remover uma equipe que está alocada em projeto(s)."
            );
        }

        equipeRepo.deletar(id);
    }
}

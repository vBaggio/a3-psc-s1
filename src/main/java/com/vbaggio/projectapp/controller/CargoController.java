package com.vbaggio.projectapp.controller;

import com.vbaggio.projectapp.model.entity.Cargo;
import com.vbaggio.projectapp.repository.CargoRepository;
import com.vbaggio.projectapp.repository.UsuarioRepository;

import java.util.List;
import java.util.UUID;

/**
 * Controller responsável pelas regras de negócio da entidade {@link Cargo}.
 *
 * <p>Valida os dados recebidos da View antes de delegar ao Repository,
 * garantindo integridade sem misturar lógica de persistência com lógica de domínio.</p>
 */
public class CargoController {

    private final CargoRepository    cargoRepo;
    private final UsuarioRepository  usuarioRepo;

    public CargoController() {
        this.cargoRepo   = new CargoRepository();
        this.usuarioRepo = new UsuarioRepository();
    }

    /**
     * Cadastra um novo cargo.
     *
     * <p>Regras:</p>
     * <ul>
     *   <li>Nome não pode ser nulo ou vazio.</li>
     *   <li>Nome não pode ser duplicado (case-insensitive).</li>
     * </ul>
     *
     * @param nome nome do cargo
     * @return entidade Cargo persistida
     * @throws IllegalArgumentException se o nome for inválido ou já existir
     */
    public Cargo cadastrarCargo(String nome) {
        validarNomeNaoVazio(nome);

        if (cargoRepo.buscarPorNome(nome).isPresent()) {
            throw new IllegalArgumentException("Já existe um cargo com o nome '" + nome + "'.");
        }

        Cargo cargo = new Cargo();
        cargo.setNome(nome.trim());
        cargoRepo.salvar(cargo);
        return cargo;
    }

    /**
     * Retorna todos os cargos cadastrados.
     *
     * @return lista de cargos ordenada por nome
     */
    public List<Cargo> listarCargos() {
        return cargoRepo.listarTodos();
    }

    /**
     * Busca um cargo pelo id.
     *
     * @param id UUID do cargo
     * @return entidade Cargo encontrada
     * @throws IllegalArgumentException se o cargo não existir
     */
    public Cargo buscarPorId(UUID id) {
        return cargoRepo.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Cargo não encontrado: " + id));
    }

    /**
     * Atualiza o nome de um cargo existente.
     *
     * <p>Regras:</p>
     * <ul>
     *   <li>Cargo deve existir.</li>
     *   <li>Novo nome não pode ser vazio nem duplicado.</li>
     * </ul>
     *
     * @param id      UUID do cargo a ser atualizado
     * @param novoNome novo nome desejado
     * @return entidade Cargo atualizada
     */
    public Cargo atualizarNome(UUID id, String novoNome) {
        validarNomeNaoVazio(novoNome);

        Cargo cargo = buscarPorId(id);

        cargoRepo.buscarPorNome(novoNome).ifPresent(existente -> {
            if (!existente.getId().equals(id)) {
                throw new IllegalArgumentException("Já existe outro cargo com o nome '" + novoNome + "'.");
            }
        });

        cargo.setNome(novoNome.trim());
        return cargoRepo.atualizar(cargo);
    }

    /**
     * Remove um cargo pelo id.
     *
     * <p>Regra: não é possível remover um cargo que ainda possui usuários vinculados.</p>
     *
     * @param id UUID do cargo a ser removido
     * @throws IllegalStateException se houver usuários vinculados ao cargo
     */
    public void removerCargo(UUID id) {
        buscarPorId(id); // garante que existe

        boolean temUsuarios = usuarioRepo.listarTodos().stream()
                .anyMatch(u -> u.getCargo() != null && u.getCargo().getId().equals(id));

        if (temUsuarios) {
            throw new IllegalStateException(
                    "Não é possível remover o cargo pois existem usuários vinculados a ele."
            );
        }

        cargoRepo.deletar(id);
    }

    // ------------------------------------------------------------------
    // Privados
    // ------------------------------------------------------------------

    private void validarNomeNaoVazio(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do cargo não pode ser vazio.");
        }
    }
}

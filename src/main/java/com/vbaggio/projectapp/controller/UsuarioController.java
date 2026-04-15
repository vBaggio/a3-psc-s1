package com.vbaggio.projectapp.controller;

import com.vbaggio.projectapp.model.entity.Cargo;
import com.vbaggio.projectapp.model.entity.Tarefa;
import com.vbaggio.projectapp.model.entity.Usuario;
import com.vbaggio.projectapp.model.enums.Perfil;
import com.vbaggio.projectapp.model.enums.StatusTarefa;
import com.vbaggio.projectapp.repository.CargoRepository;
import com.vbaggio.projectapp.repository.TarefaRepository;
import com.vbaggio.projectapp.repository.UsuarioRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;
import java.util.UUID;

/**
 * Controller responsável pelas regras de negócio da entidade {@link Usuario}.
 *
 * <p>Centraliza validações de domínio: formato de CPF, unicidade de login/e-mail,
 * regras de perfil de acesso e autenticação.</p>
 */
public class UsuarioController {

    private final UsuarioRepository usuarioRepo;
    private final CargoRepository   cargoRepo;
    private final TarefaRepository  tarefaRepository;

    public UsuarioController() {
        this.usuarioRepo      = new UsuarioRepository();
        this.cargoRepo        = new CargoRepository();
        this.tarefaRepository = new TarefaRepository();
    }

    /**
     * Cadastra um novo usuário no sistema.
     *
     * <p>Regras:</p>
     * <ul>
     *   <li>CPF deve ter exatamente 11 dígitos numéricos.</li>
     *   <li>Login, CPF e e-mail devem ser únicos.</li>
     *   <li>Senha não pode ser vazia.</li>
     *   <li>Perfil é obrigatório.</li>
     *   <li>Cargo é opcional (pode ser null).</li>
     * </ul>
     *
     * @param nome    nome completo
     * @param cpf     CPF sem formatação (11 dígitos)
     * @param email   e-mail
     * @param login   login de acesso
     * @param senha   senha armazenada com hash BCrypt
     * @param perfil  perfil de acesso
     * @param cargoId UUID do cargo (pode ser null)
     * @return entidade Usuario persistida
     */
    public Usuario cadastrarUsuario(String nome, String cpf, String email,
                                    String login, String senha,
                                    Perfil perfil, UUID cargoId) {
        validarCamposObrigatorios(nome, cpf, email, login, senha, perfil);
        validarCpf(cpf);
        validarUnicidade(cpf, email, login, null);

        Usuario usuario = new Usuario();
        usuario.setNome(nome.trim());
        usuario.setCpf(cpf.trim());
        usuario.setEmail(email.trim());
        usuario.setLogin(login.trim());
        usuario.setSenha(BCrypt.hashpw(senha, BCrypt.gensalt()));
        usuario.setPerfil(perfil);

        if (cargoId != null) {
            Cargo cargo = cargoRepo.buscarPorId(cargoId)
                    .orElseThrow(() -> new IllegalArgumentException("Cargo não encontrado: " + cargoId));
            usuario.setCargo(cargo);
        }

        usuarioRepo.salvar(usuario);
        return usuario;
    }

    /**
     * Autentica um usuário pelo login e senha.
     *
     * @param login login
     * @param senha senha
     * @return entidade Usuario autenticada
     * @throws IllegalArgumentException se as credenciais forem inválidas
     */
    public Usuario autenticar(String login, String senha) {
        if (login == null || login.isBlank() || senha == null || senha.isBlank()) {
            throw new IllegalArgumentException("Login e senha são obrigatórios.");
        }

        Usuario usuario = usuarioRepo.buscarPorLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Usuário ou senha inválidos."));

        if (!BCrypt.checkpw(senha, usuario.getSenha())) {
            throw new IllegalArgumentException("Usuário ou senha inválidos.");
        }

        return usuario;
    }

    /**
     * Retorna todos os usuários cadastrados.
     *
     * @return lista de usuários ordenada por nome
     */
    public List<Usuario> listarUsuarios() {
        return usuarioRepo.listarTodos();
    }

    /**
     * Filtra usuários pelo perfil de acesso.
     *
     * @param perfil perfil desejado
     * @return lista de usuários com o perfil informado
     */
    public List<Usuario> listarPorPerfil(Perfil perfil) {
        if (perfil == null) {
            throw new IllegalArgumentException("Perfil não pode ser nulo.");
        }
        return usuarioRepo.listarPorPerfil(perfil);
    }

    /**
     * Busca um usuário pelo id.
     *
     * @param id UUID do usuário
     * @return entidade Usuario encontrada
     * @throws IllegalArgumentException se não encontrado
     */
    public Usuario buscarPorId(UUID id) {
        return usuarioRepo.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + id));
    }

    /**
     * Atualiza os dados de um usuário existente.
     *
     * <p>Login, CPF e e-mail continuam únicos — a validação ignora o próprio registro.</p>
     *
     * @param id      UUID do usuário
     * @param nome    novo nome
     * @param cpf       novo CPF
     * @param email     novo e-mail
     * @param login     novo login
     * @param novaSenha nova senha em texto plano (null ou vazio = não altera)
     * @param perfil    novo perfil
     * @param cargoId   novo cargo (pode ser null)
     * @return entidade Usuario atualizada
     */
    public Usuario atualizarUsuario(UUID id, String nome, String cpf, String email,
                                    String login, String novaSenha, Perfil perfil, UUID cargoId) {
        Usuario usuario = buscarPorId(id);

        if (nome   != null && !nome.isBlank())  usuario.setNome(nome.trim());
        if (email  != null && !email.isBlank())  usuario.setEmail(email.trim());
        if (login  != null && !login.isBlank())  usuario.setLogin(login.trim());
        if (cpf    != null && !cpf.isBlank())  { validarCpf(cpf); usuario.setCpf(cpf.trim()); }
        if (novaSenha != null && !novaSenha.isBlank()) {
            usuario.setSenha(BCrypt.hashpw(novaSenha, BCrypt.gensalt()));
        }
        if (perfil != null)                      usuario.setPerfil(perfil);

        validarUnicidade(usuario.getCpf(), usuario.getEmail(), usuario.getLogin(), id);

        if (cargoId != null) {
            Cargo cargo = cargoRepo.buscarPorId(cargoId)
                    .orElseThrow(() -> new IllegalArgumentException("Cargo não encontrado: " + cargoId));
            usuario.setCargo(cargo);
        }

        return usuarioRepo.atualizar(usuario);
    }

    /**
     * Remove um usuário pelo id.
     *
     * @param id UUID do usuário
     */
    public void removerUsuario(UUID id) {
        buscarPorId(id); // garante que existe

        List<Tarefa> tarefasAtivas = tarefaRepository.listarPorResponsavel(id);
        boolean temTarefaAtiva = tarefasAtivas.stream()
                .anyMatch(t -> t.getStatus() == StatusTarefa.PENDENTE
                            || t.getStatus() == StatusTarefa.EM_ANDAMENTO);
        if (temTarefaAtiva) {
            throw new IllegalStateException(
                    "Usuário possui tarefas ativas (PENDENTE ou EM_ANDAMENTO). Reatribua as tarefas antes de remover o usuário."
            );
        }

        usuarioRepo.deletar(id);
    }

    // ------------------------------------------------------------------
    // Privados — validações de domínio
    // ------------------------------------------------------------------

    private void validarCamposObrigatorios(String nome, String cpf, String email,
                                            String login, String senha, Perfil perfil) {
        if (nome   == null || nome.isBlank())  throw new IllegalArgumentException("Nome é obrigatório.");
        if (cpf    == null || cpf.isBlank())   throw new IllegalArgumentException("CPF é obrigatório.");
        if (email  == null || email.isBlank())  throw new IllegalArgumentException("E-mail é obrigatório.");
        if (login  == null || login.isBlank())  throw new IllegalArgumentException("Login é obrigatório.");
        if (senha  == null || senha.isBlank())  throw new IllegalArgumentException("Senha é obrigatória.");
        if (perfil == null)                     throw new IllegalArgumentException("Perfil é obrigatório.");
    }

    private void validarCpf(String cpf) {
        if (!cpf.matches("\\d{11}")) {
            throw new IllegalArgumentException("CPF inválido. Informe exatamente 11 dígitos numéricos.");
        }
    }

    private void validarUnicidade(String cpf, String email, String login, UUID ignorarId) {
        usuarioRepo.buscarPorCpf(cpf).ifPresent(u -> {
            if (ignorarId == null || !u.getId().equals(ignorarId)) {
                throw new IllegalArgumentException("Já existe um usuário com o CPF informado.");
            }
        });

        usuarioRepo.buscarPorLogin(login).ifPresent(u -> {
            if (ignorarId == null || !u.getId().equals(ignorarId)) {
                throw new IllegalArgumentException("Já existe um usuário com o login '" + login + "'.");
            }
        });

        usuarioRepo.buscarPorEmail(email).ifPresent(u -> {
            if (ignorarId == null || !u.getId().equals(ignorarId)) {
                throw new IllegalArgumentException("E-mail já cadastrado.");
            }
        });
    }
}

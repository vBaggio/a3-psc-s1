package com.vbaggio.projectapp.controller;

import com.vbaggio.projectapp.model.entity.Projeto;
import com.vbaggio.projectapp.model.entity.Tarefa;
import com.vbaggio.projectapp.model.entity.Usuario;
import com.vbaggio.projectapp.model.enums.Perfil;
import com.vbaggio.projectapp.model.enums.StatusProjeto;
import com.vbaggio.projectapp.model.enums.StatusTarefa;
import com.vbaggio.projectapp.repository.ProjetoRepository;
import com.vbaggio.projectapp.repository.TarefaRepository;
import com.vbaggio.projectapp.repository.UsuarioRepository;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller responsável pelas regras de negócio da entidade {@link Tarefa}.
 *
 * <p>Valida os dados recebidos da View antes de delegar ao Repository,
 * garantindo integridade sem misturar lógica de persistência com lógica de domínio.</p>
 */
public class TarefaController {

    private final TarefaRepository   tarefaRepo;
    private final ProjetoRepository  projetoRepo;
    private final UsuarioRepository  usuarioRepo;

    public TarefaController() {
        this.tarefaRepo  = new TarefaRepository();
        this.projetoRepo = new ProjetoRepository();
        this.usuarioRepo = new UsuarioRepository();
    }

    /**
     * Cria uma nova tarefa vinculada a um projeto.
     *
     * <p>Regras:</p>
     * <ul>
     *   <li>Nome não pode ser nulo ou vazio.</li>
     *   <li>projetoId é obrigatório e o projeto deve existir.</li>
     *   <li>Projeto deve estar com status PLANEJADO ou EM_ANDAMENTO.</li>
     *   <li>responsavelId é opcional; se informado, o usuário deve existir.</li>
     *   <li>Status inicial: {@link StatusTarefa#PENDENTE}.</li>
     * </ul>
     *
     * @param nome          nome da tarefa
     * @param descricao     descrição (pode ser null)
     * @param prazo         prazo de entrega (pode ser null)
     * @param projetoId     UUID do projeto
     * @param responsavelId UUID do usuário responsável (pode ser null)
     * @return entidade Tarefa persistida
     * @throws IllegalArgumentException se dados inválidos ou entidades não encontradas
     * @throws IllegalStateException    se o projeto não aceita novas tarefas
     */
    public Tarefa criarTarefa(String nome, String descricao, LocalDate prazo,
                              UUID projetoId, UUID responsavelId, Usuario caller) {
        return criarTarefa(nome, descricao, prazo, projetoId, responsavelId, caller, null);
    }

    public Tarefa criarTarefa(String nome, String descricao, LocalDate prazo,
                              UUID projetoId, UUID responsavelId, Usuario caller, EntityManager em) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome da tarefa é obrigatório.");
        }
        if (projetoId == null) {
            throw new IllegalArgumentException("O identificador do projeto é obrigatório.");
        }

        Projeto projeto = projetoRepo.buscarPorId(projetoId)
                .orElseThrow(() -> new IllegalArgumentException("Projeto não encontrado."));

        if (projeto.getStatus() != StatusProjeto.PLANEJADO
                && projeto.getStatus() != StatusProjeto.EM_ANDAMENTO) {
            throw new IllegalStateException(
                    "Não é possível criar tarefa para projeto com status: " + projeto.getStatus()
            );
        }

        boolean isAdmin = caller.getPerfil() == Perfil.ADMINISTRADOR;
        boolean isGerenteEfetivo = projeto.getGerente() != null
                && projeto.getGerente().getId().equals(caller.getId());

        if (!isAdmin && !isGerenteEfetivo) {
            // COLABORADOR ou GERENTE-membro: self-assignment forçado
            responsavelId = caller.getId();
        } else if (isGerenteEfetivo && responsavelId != null) {
            // GERENTE_EFETIVO: responsável deve ser membro da equipe
            if (projeto.getEquipe() != null) {
                UUID finalResp = responsavelId;
                boolean isMembro = projeto.getEquipe().getMembros().stream()
                        .anyMatch(m -> m.getId().equals(finalResp));
                if (!isMembro)
                    throw new IllegalArgumentException(
                            "O responsável deve ser membro da equipe do projeto.");
            }
        }

        Tarefa tarefa = new Tarefa();
        tarefa.setNome(nome.trim());
        tarefa.setDescricao(descricao == null || descricao.isBlank() ? null : descricao.trim());
        tarefa.setPrazo(prazo);
        tarefa.setProjeto(projeto);
        tarefa.setStatus(StatusTarefa.PENDENTE);

        if (responsavelId != null) {
            Usuario responsavel = usuarioRepo.buscarPorId(responsavelId)
                    .orElseThrow(() -> new IllegalArgumentException("Responsável não encontrado."));
            tarefa.setResponsavel(responsavel);
        }

        tarefaRepo.salvar(tarefa, em);
        return tarefa;
    }

    /**
     * Atualiza nome, descrição e prazo de uma tarefa existente.
     *
     * @param id        UUID da tarefa
     * @param nome      novo nome (obrigatório)
     * @param descricao nova descrição (null ou em branco → armazenado como null)
     * @param prazo     novo prazo (pode ser null)
     * @throws IllegalArgumentException se nome for nulo ou em branco
     * @throws IllegalStateException    se a tarefa estiver CONCLUIDA ou CANCELADA
     */
    public void atualizarTarefa(UUID id, String nome, String descricao, LocalDate prazo) {
        atualizarTarefa(id, nome, descricao, prazo, null);
    }

    public void atualizarTarefa(UUID id, String nome, String descricao, LocalDate prazo, EntityManager em) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome da tarefa é obrigatório.");
        }
        Tarefa tarefa = buscarTarefaOuFalhar(id);
        tarefa.setNome(nome.trim());
        tarefa.setDescricao(descricao == null || descricao.isBlank() ? null : descricao.trim());
        tarefa.setPrazo(prazo);
        tarefaRepo.atualizar(tarefa, em);
    }

    /**
     * Atualiza o status de uma tarefa seguindo a máquina de estados definida.
     *
     * <p>Transições permitidas:</p>
     * <ul>
     *   <li>PENDENTE → EM_ANDAMENTO | CANCELADA</li>
     *   <li>EM_ANDAMENTO → CONCLUIDA | CANCELADA</li>
     *   <li>CONCLUIDA → nenhuma</li>
     *   <li>CANCELADA → nenhuma</li>
     * </ul>
     *
     * @param tarefaId   UUID da tarefa
     * @param novoStatus novo status desejado
     * @return entidade Tarefa atualizada
     * @throws IllegalArgumentException se a tarefa não existir
     * @throws IllegalStateException    se a transição for inválida
     */
    public Tarefa atualizarStatus(UUID tarefaId, StatusTarefa novoStatus) {
        return atualizarStatus(tarefaId, novoStatus, null);
    }

    public Tarefa atualizarStatus(UUID tarefaId, StatusTarefa novoStatus, EntityManager em) {
        Tarefa tarefa = buscarTarefaOuFalhar(tarefaId);
        tarefa.setStatus(novoStatus);
        return tarefaRepo.atualizar(tarefa, em);
    }

    /**
     * Reatribui (ou remove) o responsável de uma tarefa.
     *
     * <p>Regras:</p>
     * <ul>
     *   <li>novoResponsavelId null → desatribui o responsável.</li>
     *   <li>novoResponsavelId não null → usuário deve existir.</li>
     * </ul>
     *
     * @param tarefaId          UUID da tarefa
     * @param novoResponsavelId UUID do novo responsável (null para desatribuir)
     * @return entidade Tarefa atualizada
     */
    public Tarefa reatribuirResponsavel(UUID tarefaId, UUID novoResponsavelId, Usuario caller) {
        return reatribuirResponsavel(tarefaId, novoResponsavelId, caller, null);
    }

    public Tarefa reatribuirResponsavel(UUID tarefaId, UUID novoResponsavelId, Usuario caller, EntityManager em) {
        Tarefa tarefa = buscarTarefaOuFalhar(tarefaId);

        Projeto projeto = projetoRepo.buscarPorId(tarefa.getProjeto().getId())
                .orElseThrow(() -> new IllegalArgumentException("Projeto não encontrado."));

        boolean isAdmin = caller.getPerfil() == Perfil.ADMINISTRADOR;
        boolean isGerenteEfetivo = projeto.getGerente() != null
                && projeto.getGerente().getId().equals(caller.getId());
        if (!isAdmin && !isGerenteEfetivo)
            throw new IllegalStateException(
                    "Apenas ADMINISTRADOR ou o gerente do projeto podem reatribuir tarefas.");

        if (novoResponsavelId != null && projeto.getEquipe() != null) {
            UUID finalResp = novoResponsavelId;
            boolean isMembro = projeto.getEquipe().getMembros().stream()
                    .anyMatch(m -> m.getId().equals(finalResp));
            if (!isMembro)
                throw new IllegalArgumentException(
                        "O novo responsável deve ser membro da equipe do projeto.");
        }

        if (novoResponsavelId == null) {
            tarefa.setResponsavel(null);
        } else {
            Usuario responsavel = usuarioRepo.buscarPorId(novoResponsavelId)
                    .orElseThrow(() -> new IllegalArgumentException("Responsável não encontrado."));
            tarefa.setResponsavel(responsavel);
        }

        return tarefaRepo.atualizar(tarefa, em);
    }

    /**
     * Retorna todas as tarefas de um projeto.
     *
     * @param projetoId UUID do projeto
     * @return lista de tarefas
     */
    public List<Tarefa> listarPorProjeto(UUID projetoId) {
        return tarefaRepo.listarPorProjeto(projetoId);
    }

    /**
     * Retorna todas as tarefas atribuídas a um usuário.
     *
     * @param usuarioId UUID do usuário responsável
     * @return lista de tarefas
     */
    public List<Tarefa> listarPorResponsavel(UUID usuarioId) {
        return tarefaRepo.listarPorResponsavel(usuarioId);
    }

    /**
     * Busca uma tarefa pelo id.
     *
     * @param tarefaId UUID da tarefa
     * @return Optional com a tarefa, ou vazio se não encontrada
     */
    public Optional<Tarefa> buscarPorId(UUID tarefaId) {
        return tarefaRepo.buscarPorId(tarefaId);
    }

    /**
     * Remove uma tarefa pelo id.
     *
     * <p>Regra: apenas tarefas com status PENDENTE ou CANCELADA podem ser removidas.</p>
     *
     * @param tarefaId UUID da tarefa
     * @throws IllegalArgumentException se a tarefa não existir
     * @throws IllegalStateException    se o status não permitir remoção
     */
    public void removerTarefa(UUID tarefaId) { removerTarefa(tarefaId, null); }

    public void removerTarefa(UUID tarefaId, EntityManager em) {
        buscarTarefaOuFalhar(tarefaId);
        tarefaRepo.deletar(tarefaId, em);
    }

    // ------------------------------------------------------------------
    // Privados
    // ------------------------------------------------------------------

    private Tarefa buscarTarefaOuFalhar(UUID tarefaId) {
        return tarefaRepo.buscarPorId(tarefaId)
                .orElseThrow(() -> new IllegalArgumentException("Tarefa não encontrada."));
    }

}

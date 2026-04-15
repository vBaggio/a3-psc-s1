package com.vbaggio.projectapp.controller;

import com.vbaggio.projectapp.model.entity.Projeto;
import com.vbaggio.projectapp.model.entity.Tarefa;
import com.vbaggio.projectapp.model.entity.Usuario;
import com.vbaggio.projectapp.model.enums.StatusProjeto;
import com.vbaggio.projectapp.model.enums.StatusTarefa;
import com.vbaggio.projectapp.repository.ProjetoRepository;
import com.vbaggio.projectapp.repository.TarefaRepository;
import com.vbaggio.projectapp.repository.UsuarioRepository;

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
                              UUID projetoId, UUID responsavelId) {
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

        Tarefa tarefa = new Tarefa();
        tarefa.setNome(nome.trim());
        tarefa.setDescricao(descricao);
        tarefa.setPrazo(prazo);
        tarefa.setProjeto(projeto);
        tarefa.setStatus(StatusTarefa.PENDENTE);

        if (responsavelId != null) {
            Usuario responsavel = usuarioRepo.buscarPorId(responsavelId)
                    .orElseThrow(() -> new IllegalArgumentException("Responsável não encontrado."));
            tarefa.setResponsavel(responsavel);
        }

        tarefaRepo.salvar(tarefa);
        return tarefa;
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
        Tarefa tarefa = buscarTarefaOuFalhar(tarefaId);
        validarTransicaoStatus(tarefa.getStatus(), novoStatus);
        tarefa.setStatus(novoStatus);
        return tarefaRepo.atualizar(tarefa);
    }

    /**
     * Reatribui (ou remove) o responsável de uma tarefa.
     *
     * <p>Regras:</p>
     * <ul>
     *   <li>Tarefa não pode estar CONCLUIDA ou CANCELADA.</li>
     *   <li>novoResponsavelId null → desatribui o responsável.</li>
     *   <li>novoResponsavelId não null → usuário deve existir.</li>
     * </ul>
     *
     * @param tarefaId          UUID da tarefa
     * @param novoResponsavelId UUID do novo responsável (null para desatribuir)
     * @return entidade Tarefa atualizada
     */
    public Tarefa reatribuirResponsavel(UUID tarefaId, UUID novoResponsavelId) {
        Tarefa tarefa = buscarTarefaOuFalhar(tarefaId);

        if (tarefa.getStatus() == StatusTarefa.CONCLUIDA
                || tarefa.getStatus() == StatusTarefa.CANCELADA) {
            throw new IllegalStateException(
                    "Não é possível reatribuir responsável de tarefa finalizada."
            );
        }

        if (novoResponsavelId == null) {
            tarefa.setResponsavel(null);
        } else {
            Usuario responsavel = usuarioRepo.buscarPorId(novoResponsavelId)
                    .orElseThrow(() -> new IllegalArgumentException("Responsável não encontrado."));
            tarefa.setResponsavel(responsavel);
        }

        return tarefaRepo.atualizar(tarefa);
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
    public void removerTarefa(UUID tarefaId) {
        Tarefa tarefa = buscarTarefaOuFalhar(tarefaId);

        if (tarefa.getStatus() == StatusTarefa.EM_ANDAMENTO
                || tarefa.getStatus() == StatusTarefa.CONCLUIDA) {
            throw new IllegalStateException(
                    "Apenas tarefas PENDENTE ou CANCELADA podem ser removidas."
            );
        }

        tarefaRepo.deletar(tarefaId);
    }

    // ------------------------------------------------------------------
    // Privados
    // ------------------------------------------------------------------

    private Tarefa buscarTarefaOuFalhar(UUID tarefaId) {
        return tarefaRepo.buscarPorId(tarefaId)
                .orElseThrow(() -> new IllegalArgumentException("Tarefa não encontrada."));
    }

    private void validarTransicaoStatus(StatusTarefa atual, StatusTarefa novo) {
        boolean valido = switch (atual) {
            case PENDENTE     -> novo == StatusTarefa.EM_ANDAMENTO || novo == StatusTarefa.CANCELADA;
            case EM_ANDAMENTO -> novo == StatusTarefa.CONCLUIDA    || novo == StatusTarefa.CANCELADA;
            case CONCLUIDA    -> throw new IllegalStateException("Tarefa já concluída.");
            case CANCELADA    -> throw new IllegalStateException("Tarefa já cancelada.");
        };
        if (!valido) {
            throw new IllegalStateException(
                    "Transição de status inválida: " + atual + " → " + novo
            );
        }
    }
}

package com.vbaggio.projectapp.controller;

import com.vbaggio.projectapp.model.entity.Projeto;
import com.vbaggio.projectapp.model.entity.Tarefa;
import com.vbaggio.projectapp.model.entity.Usuario;
import com.vbaggio.projectapp.model.enums.Perfil;
import com.vbaggio.projectapp.model.enums.StatusProjeto;
import com.vbaggio.projectapp.model.enums.StatusTarefa;
import com.vbaggio.projectapp.repository.EquipeRepository;
import com.vbaggio.projectapp.repository.ProjetoRepository;
import com.vbaggio.projectapp.repository.TarefaRepository;
import com.vbaggio.projectapp.repository.UsuarioRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Controller responsável pelas regras de negócio da entidade {@link Projeto}.
 *
 * <p>Garante que datas sejam coerentes, que somente GERENTEs possam
 * ser responsáveis, e que transições de status sejam válidas.</p>
 */
public class ProjetoController {

    private final ProjetoRepository  projetoRepo;
    private final UsuarioRepository  usuarioRepo;
    private final EquipeRepository   equipeRepo;
    private final TarefaRepository   tarefaRepo;

    public ProjetoController() {
        this.projetoRepo      = new ProjetoRepository();
        this.usuarioRepo      = new UsuarioRepository();
        this.equipeRepo       = new EquipeRepository();
        this.tarefaRepo = new TarefaRepository();
    }

    /**
     * Cria um novo projeto.
     *
     * <p>Regras:</p>
     * <ul>
     *   <li>Nome obrigatório.</li>
     *   <li>Data de previsão deve ser >= data de início (quando ambas informadas).</li>
     *   <li>O gerente informado deve ter perfil {@link Perfil#GERENTE}.</li>
     *   <li>Status inicial padrão: {@link StatusProjeto#PLANEJADO}.</li>
     * </ul>
     *
     * @param nome        nome do projeto
     * @param descricao   descrição (pode ser null)
     * @param dataInicio  data de início (pode ser null)
     * @param dataPrevisao data de previsão de término (pode ser null)
     * @param gerenteId   UUID do usuário com perfil GERENTE
     * @return entidade Projeto persistida
     */
    public Projeto criarProjeto(String nome, String descricao,
                                LocalDate dataInicio, LocalDate dataPrevisao,
                                UUID gerenteId) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do projeto é obrigatório.");
        }
        validarDatas(dataInicio, dataPrevisao);

        Usuario gerente = resolverGerente(gerenteId);

        Projeto projeto = new Projeto();
        projeto.setNome(nome.trim());
        projeto.setDescricao(descricao);
        projeto.setDataInicio(dataInicio);
        projeto.setDataPrevisao(dataPrevisao);
        projeto.setStatus(StatusProjeto.PLANEJADO);
        projeto.setGerente(gerente);

        projetoRepo.salvar(projeto);
        return projeto;
    }

    /**
     * Atualiza o status de um projeto.
     *
     * <p>Regras de transição permitidas:</p>
     * <ul>
     *   <li>PLANEJADO → EM_ANDAMENTO</li>
     *   <li>EM_ANDAMENTO → CONCLUIDO | CANCELADO</li>
     *   <li>PLANEJADO → CANCELADO</li>
     * </ul>
     *
     * @param projetoId  UUID do projeto
     * @param novoStatus novo status desejado
     * @return projeto atualizado
     */
    public Projeto atualizarStatus(UUID projetoId, StatusProjeto novoStatus) {
        Projeto projeto = buscarPorId(projetoId);
        validarTransicaoStatus(projeto.getStatus(), novoStatus);
        projeto.setStatus(novoStatus);
        Projeto atualizado = projetoRepo.atualizar(projeto);

        if (novoStatus == StatusProjeto.CANCELADO) {
            List<Tarefa> tarefasAtivas = tarefaRepo.listarPorProjeto(projetoId);
            for (Tarefa t : tarefasAtivas) {
                if (t.getStatus() == StatusTarefa.PENDENTE || t.getStatus() == StatusTarefa.EM_ANDAMENTO) {
                    t.setStatus(StatusTarefa.CANCELADA);
                    tarefaRepo.atualizar(t);
                }
            }
        }

        return atualizado;
    }

    /**
     * Encerra formalmente um projeto, registrando a data de conclusão.
     *
     * <p>Regra: apenas projetos em {@link StatusProjeto#EM_ANDAMENTO} podem ser encerrados.</p>
     *
     * @param projetoId UUID do projeto
     * @param dataFim   data de encerramento real
     * @return projeto atualizado com status CONCLUIDO
     */
    public Projeto encerrarProjeto(UUID projetoId, LocalDate dataFim) {
        Projeto projeto = buscarPorId(projetoId);

        if (projeto.getStatus() != StatusProjeto.EM_ANDAMENTO) {
            throw new IllegalStateException(
                    "Apenas projetos EM_ANDAMENTO podem ser encerrados. Status atual: " + projeto.getStatus()
            );
        }
        if (dataFim == null) {
            throw new IllegalArgumentException("A data de encerramento é obrigatória.");
        }

        projeto.setDataFim(dataFim);
        projeto.setStatus(StatusProjeto.CONCLUIDO);
        Projeto concluido = projetoRepo.atualizar(projeto);

        return concluido;
    }

    /**
     * Aloca uma equipe em um projeto.
     *
     * <p>Regra: a equipe deve ter ao menos 1 membro cadastrado.</p>
     *
     * @param projetoId UUID do projeto
     * @param equipeId  UUID da equipe
     */
    public void atribuirEquipe(UUID projetoId, UUID equipeId) {
        buscarPorId(projetoId); // valida existência

        equipeRepo.buscarPorId(equipeId)
                .orElseThrow(() -> new IllegalArgumentException("Equipe não encontrada: " + equipeId));

        List<Usuario> membros = equipeRepo.listarMembrosDaEquipe(equipeId);
        if (membros.isEmpty()) {
            throw new IllegalStateException("A equipe deve ter ao menos 1 membro antes de ser alocada.");
        }

        equipeRepo.adicionarProjeto(equipeId, projetoId);
    }

    /**
     * Retorna todos os projetos cadastrados.
     *
     * @return lista de projetos ordenada por nome
     */
    public List<Projeto> listarProjetos() {
        return projetoRepo.listarTodos();
    }

    /**
     * Filtra projetos por status.
     *
     * @param status status desejado
     * @return lista de projetos com o status informado
     */
    public List<Projeto> listarPorStatus(StatusProjeto status) {
        if (status == null) {
            throw new IllegalArgumentException("Status não pode ser nulo.");
        }
        return projetoRepo.listarPorStatus(status);
    }

    /**
     * Busca um projeto pelo id.
     *
     * @param id UUID do projeto
     * @return entidade Projeto encontrada
     * @throws IllegalArgumentException se não encontrado
     */
    public Projeto buscarPorId(UUID id) {
        return projetoRepo.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Projeto não encontrado: " + id));
    }

    // ------------------------------------------------------------------
    // Privados
    // ------------------------------------------------------------------

    private void validarDatas(LocalDate inicio, LocalDate previsao) {
        if (inicio != null && previsao != null && previsao.isBefore(inicio)) {
            throw new IllegalArgumentException(
                    "A data de previsão não pode ser anterior à data de início."
            );
        }
    }

    private void validarTransicaoStatus(StatusProjeto atual, StatusProjeto novo) {
        boolean valido = switch (atual) {
            case PLANEJADO    -> novo == StatusProjeto.EM_ANDAMENTO || novo == StatusProjeto.CANCELADO;
            case EM_ANDAMENTO -> novo == StatusProjeto.CONCLUIDO    || novo == StatusProjeto.CANCELADO;
            case CONCLUIDO, CANCELADO -> false;
        };
        if (!valido) {
            throw new IllegalStateException(
                    "Transição de status inválida: " + atual + " → " + novo
            );
        }
    }

    private Usuario resolverGerente(UUID gerenteId) {
        if (gerenteId == null) {
            throw new IllegalArgumentException("O gerente do projeto é obrigatório.");
        }
        Usuario gerente = usuarioRepo.buscarPorId(gerenteId)
                .orElseThrow(() -> new IllegalArgumentException("Gerente não encontrado: " + gerenteId));

        if (gerente.getPerfil() != Perfil.GERENTE) {
            throw new IllegalArgumentException(
                    "O usuário '" + gerente.getNome() + "' não possui perfil de GERENTE."
            );
        }
        return gerente;
    }
}

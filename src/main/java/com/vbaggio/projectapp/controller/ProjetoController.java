package com.vbaggio.projectapp.controller;

import com.vbaggio.projectapp.model.entity.Equipe;
import com.vbaggio.projectapp.model.entity.Projeto;
import com.vbaggio.projectapp.model.entity.Usuario;
import com.vbaggio.projectapp.model.enums.Perfil;
import com.vbaggio.projectapp.model.enums.StatusProjeto;
import com.vbaggio.projectapp.repository.EquipeRepository;
import com.vbaggio.projectapp.repository.ProjetoRepository;
import com.vbaggio.projectapp.repository.TarefaRepository;
import com.vbaggio.projectapp.repository.UsuarioRepository;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
        this.tarefaRepo       = new TarefaRepository();
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
                                UUID gerenteId, UUID equipeId) {
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

        if (equipeId != null) {
            Equipe equipe = resolverEquipe(equipeId);
            List<Usuario> membros = equipeRepo.listarMembrosDaEquipe(equipeId);
            if (membros.isEmpty()) {
                throw new IllegalArgumentException("A equipe deve ter ao menos 1 membro para ser vinculada a um projeto.");
            }
            projeto.setEquipe(equipe);
        }

        projetoRepo.salvar(projeto);
        return projeto;
    }

    /**
     * Atualiza os dados de um projeto existente.
     *
     * <p>Regras:</p>
     * <ul>
     *   <li>Nome obrigatório.</li>
     *   <li>Data de previsão deve ser >= data de início (quando ambas informadas).</li>
     *   <li>Projetos CONCLUIDO ou CANCELADO não podem ser editados.</li>
     * </ul>
     *
     * @param id           UUID do projeto a ser atualizado
     * @param nome         novo nome
     * @param descricao    nova descrição (pode ser null)
     * @param dataInicio   nova data de início (pode ser null)
     * @param dataPrevisao nova data de previsão (pode ser null)
     * @param gerenteId    UUID do novo gerente
     */
    public void atualizarProjeto(UUID id, String nome, String descricao,
                                  LocalDate dataInicio, LocalDate dataPrevisao,
                                  UUID gerenteId, UUID equipeId, Usuario caller) {
        atualizarProjeto(id, nome, descricao, dataInicio, dataPrevisao, gerenteId, equipeId, caller, null);
    }

    public void atualizarProjeto(UUID id, String nome, String descricao,
                                  LocalDate dataInicio, LocalDate dataPrevisao,
                                  UUID gerenteId, UUID equipeId, Usuario caller, EntityManager em) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do projeto é obrigatório.");
        }
        validarDatas(dataInicio, dataPrevisao);
        Projeto projeto = buscarPorId(id);
        if (caller.getPerfil() != Perfil.ADMINISTRADOR && !isGerenteEfetivo(projeto, caller))
            throw new IllegalStateException("Apenas ADMINISTRADOR ou o gerente do projeto podem editar.");
        if (gerenteId != null
                && projeto.getGerente() != null
                && !gerenteId.equals(projeto.getGerente().getId())
                && caller.getPerfil() != Perfil.ADMINISTRADOR)
            throw new IllegalStateException("Apenas ADMINISTRADOR pode trocar o gerente do projeto.");
        if (projeto.getStatus() == StatusProjeto.CONCLUIDO
                || projeto.getStatus() == StatusProjeto.CANCELADO) {
            throw new IllegalStateException(
                    "Não é possível editar um projeto com status " + projeto.getStatus() + ".");
        }
        projeto.setNome(nome.trim());
        projeto.setDescricao(descricao == null || descricao.isBlank() ? null : descricao.trim());
        projeto.setDataInicio(dataInicio);
        projeto.setDataPrevisao(dataPrevisao);
        projeto.setGerente(resolverGerente(gerenteId));

        if (equipeId != null) {
            Equipe equipe = resolverEquipe(equipeId);
            List<Usuario> novosMembros = equipeRepo.listarMembrosDaEquipe(equipeId);
            if (novosMembros.isEmpty()) {
                throw new IllegalArgumentException("A equipe deve ter ao menos 1 membro para ser vinculada a um projeto.");
            }

            // Validação de troca de equipe
            boolean trocandoEquipe = projeto.getEquipe() == null
                    || !projeto.getEquipe().getId().equals(equipeId);
            if (trocandoEquipe) {
                Set<UUID> membroIds = novosMembros.stream()
                        .map(Usuario::getId)
                        .collect(Collectors.toSet());
                List<String> conflitos = tarefaRepo.listarPorProjeto(id).stream()
                        .filter(t -> t.getResponsavel() != null
                                && !membroIds.contains(t.getResponsavel().getId()))
                        .map(t -> t.getNome() + " → " + t.getResponsavel().getNome())
                        .toList();
                if (!conflitos.isEmpty()) {
                    throw new IllegalStateException(
                            "Não é possível trocar a equipe pois as seguintes tarefas possuem "
                            + "responsáveis fora da nova equipe:\n" + String.join("\n", conflitos));
                }
            }

            projeto.setEquipe(equipe);
        }

        projetoRepo.atualizar(projeto, em);
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
    public Projeto atualizarStatus(UUID projetoId, StatusProjeto novoStatus, Usuario caller) {
        return atualizarStatus(projetoId, novoStatus, caller, null);
    }

    public Projeto atualizarStatus(UUID projetoId, StatusProjeto novoStatus, Usuario caller, EntityManager em) {
        Projeto projeto = buscarPorId(projetoId);
        if (caller.getPerfil() != Perfil.ADMINISTRADOR && !isGerenteEfetivo(projeto, caller))
            throw new IllegalStateException("Apenas ADMINISTRADOR ou o gerente do projeto podem alterar o status.");
        validarTransicaoStatus(projeto.getStatus(), novoStatus);
        projeto.setStatus(novoStatus);
        Projeto atualizado = projetoRepo.atualizar(projeto, em);

        if (novoStatus == StatusProjeto.CANCELADO) {
            tarefaRepo.cancelarPorProjeto(projetoId, em);
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
    public Projeto encerrarProjeto(UUID projetoId, LocalDate dataFim, Usuario caller) {
        return encerrarProjeto(projetoId, dataFim, caller, null);
    }

    public Projeto encerrarProjeto(UUID projetoId, LocalDate dataFim, Usuario caller, EntityManager em) {
        Projeto projeto = buscarPorId(projetoId);
        if (caller.getPerfil() != Perfil.ADMINISTRADOR && !isGerenteEfetivo(projeto, caller))
            throw new IllegalStateException("Apenas ADMINISTRADOR ou o gerente do projeto podem encerrar o projeto.");

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
        return projetoRepo.atualizar(projeto, em);
    }

    /**
     * Remove um projeto e todas as suas tarefas (cascade).
     *
     * @param id     UUID do projeto a ser excluído
     * @param caller usuário que está realizando a operação
     */
    public void removerProjeto(UUID id, Usuario caller) {
        if (caller.getPerfil() != Perfil.ADMINISTRADOR)
            throw new IllegalStateException("Apenas ADMINISTRADOR pode remover projetos.");
        buscarPorId(id);
        projetoRepo.deletar(id);
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
     * Retorna os projetos visíveis para o usuário informado.
     * ADMINISTRADORs veem todos; demais veem apenas os que são membro ou gerente.
     *
     * @param usuario usuário autenticado
     * @return lista de projetos visíveis
     */
    public List<Projeto> listarProjetosVisiveis(Usuario usuario) {
        if (usuario.getPerfil() == Perfil.ADMINISTRADOR) {
            return projetoRepo.listarTodos();
        }
        return projetoRepo.listarPorMembroOuGerente(usuario.getId());
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

    private boolean isGerenteEfetivo(Projeto projeto, Usuario caller) {
        return projeto.getGerente() != null
            && projeto.getGerente().getId().equals(caller.getId());
    }

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

    private Equipe resolverEquipe(UUID equipeId) {
        if (equipeId == null) throw new IllegalArgumentException("A equipe do projeto é obrigatória.");
        return equipeRepo.buscarPorId(equipeId)
                .orElseThrow(() -> new IllegalArgumentException("Equipe não encontrada: " + equipeId));
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

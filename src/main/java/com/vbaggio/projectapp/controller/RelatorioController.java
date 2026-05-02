package com.vbaggio.projectapp.controller;

import com.vbaggio.projectapp.dto.CargaUsuario;
import com.vbaggio.projectapp.dto.ProjetoOpcao;
import com.vbaggio.projectapp.dto.ResumoProjeto;
import com.vbaggio.projectapp.model.entity.Projeto;
import com.vbaggio.projectapp.model.entity.Tarefa;
import com.vbaggio.projectapp.model.entity.Usuario;
import com.vbaggio.projectapp.model.enums.StatusProjeto;
import com.vbaggio.projectapp.model.enums.StatusTarefa;
import com.vbaggio.projectapp.repository.ProjetoRepository;
import com.vbaggio.projectapp.repository.TarefaRepository;
import com.vbaggio.projectapp.repository.UsuarioRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller de leitura para geração de métricas e relatórios de desempenho.
 * Não realiza persistência — apenas agrega dados dos repositories existentes.
 */
public class RelatorioController {

    private final ProjetoRepository  projetoRepo;
    private final TarefaRepository   tarefaRepo;
    private final UsuarioRepository  usuarioRepo;

    public RelatorioController() {
        this.projetoRepo = new ProjetoRepository();
        this.tarefaRepo  = new TarefaRepository();
        this.usuarioRepo = new UsuarioRepository();
    }

    /**
     * Retorna a contagem de projetos agrupada por status.
     */
    public Map<StatusProjeto, Long> resumoGlobal() {
        List<Projeto> projetos = projetoRepo.listarTodos();

        Map<StatusProjeto, Long> resultado = new EnumMap<>(StatusProjeto.class);
        for (StatusProjeto s : StatusProjeto.values()) {
            resultado.put(s, 0L);
        }
        for (Projeto p : projetos) {
            resultado.merge(p.getStatus(), 1L, Long::sum);
        }
        return resultado;
    }

    /**
     * Calcula métricas detalhadas de tarefas e prazo para um projeto específico.
     */
    public ResumoProjeto desempenhoPorProjeto(UUID projetoId) {
        Projeto projeto = projetoRepo.buscarPorId(projetoId)
                .orElseThrow(() -> new IllegalArgumentException("Projeto não encontrado: " + projetoId));

        List<Tarefa> tarefas = tarefaRepo.listarPorProjeto(projetoId);

        int total       = tarefas.size();
        int concluidas  = 0;
        int emAndamento = 0;
        int pendentes   = 0;
        int canceladas  = 0;
        int vencidas    = 0;

        LocalDate hoje = LocalDate.now();
        for (Tarefa t : tarefas) {
            switch (t.getStatus()) {
                case CONCLUIDA  -> concluidas++;
                case EM_ANDAMENTO -> emAndamento++;
                case PENDENTE   -> pendentes++;
                case CANCELADA  -> canceladas++;
            }
            if (t.getPrazo() != null
                    && t.getPrazo().isBefore(hoje)
                    && t.getStatus() != StatusTarefa.CONCLUIDA
                    && t.getStatus() != StatusTarefa.CANCELADA) {
                vencidas++;
            }
        }

        return new ResumoProjeto(
                projeto.getNome(),
                projeto.getStatus(),
                projeto.getDataPrevisao(),
                projeto.getDataFim(),
                total, concluidas, emAndamento, pendentes, canceladas, vencidas
        );
    }

    /**
     * Retorna a carga de trabalho de todos os usuários que possuem tarefas atribuídas.
     */
    public List<CargaUsuario> cargaDeTrabalho() {
        List<Usuario> usuarios = usuarioRepo.listarTodos();
        LocalDate hoje = LocalDate.now();

        List<CargaUsuario> resultado = new ArrayList<>();
        for (Usuario u : usuarios) {
            List<Tarefa> tarefas = tarefaRepo.listarPorResponsavel(u.getId());
            if (tarefas.isEmpty()) continue;

            int pendentes   = 0;
            int emAndamento = 0;
            int concluidas  = 0;
            int canceladas  = 0;
            int vencidas    = 0;

            for (Tarefa t : tarefas) {
                switch (t.getStatus()) {
                    case PENDENTE     -> pendentes++;
                    case EM_ANDAMENTO -> emAndamento++;
                    case CONCLUIDA    -> concluidas++;
                    case CANCELADA    -> canceladas++;
                }
                if (t.getPrazo() != null
                        && t.getPrazo().isBefore(hoje)
                        && t.getStatus() != StatusTarefa.CONCLUIDA
                        && t.getStatus() != StatusTarefa.CANCELADA) {
                    vencidas++;
                }
            }

            resultado.add(new CargaUsuario(
                    u.getNome(),
                    u.getPerfil().toString(),
                    pendentes, emAndamento, concluidas, canceladas, vencidas
            ));
        }
        return resultado;
    }

    /** Lista projetos como DTOs leves para popular combos na view. */
    public List<ProjetoOpcao> listarProjetosParaCombo() {
        return projetoRepo.listarTodos().stream()
                .map(p -> new ProjetoOpcao(p.getId(), p.getNome()))
                .toList();
    }
}

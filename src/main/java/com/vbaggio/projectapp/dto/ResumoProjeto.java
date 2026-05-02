package com.vbaggio.projectapp.dto;

import com.vbaggio.projectapp.model.enums.StatusProjeto;

import java.time.LocalDate;

public class ResumoProjeto {

    private final String nome;
    private final StatusProjeto status;
    private final LocalDate dataPrevisao;
    private final LocalDate dataFim;

    private final int totalTarefas;
    private final int tarefasConcluidas;
    private final int tarefasEmAndamento;
    private final int tarefasPendentes;
    private final int tarefasCanceladas;
    private final int tarefasVencidas;

    public ResumoProjeto(String nome, StatusProjeto status,
                         LocalDate dataPrevisao, LocalDate dataFim,
                         int totalTarefas, int tarefasConcluidas,
                         int tarefasEmAndamento, int tarefasPendentes,
                         int tarefasCanceladas, int tarefasVencidas) {
        this.nome              = nome;
        this.status            = status;
        this.dataPrevisao      = dataPrevisao;
        this.dataFim           = dataFim;
        this.totalTarefas      = totalTarefas;
        this.tarefasConcluidas = tarefasConcluidas;
        this.tarefasEmAndamento = tarefasEmAndamento;
        this.tarefasPendentes  = tarefasPendentes;
        this.tarefasCanceladas = tarefasCanceladas;
        this.tarefasVencidas   = tarefasVencidas;
    }

    public String getNome()               { return nome; }
    public StatusProjeto getStatus()      { return status; }
    public LocalDate getDataPrevisao()    { return dataPrevisao; }
    public LocalDate getDataFim()         { return dataFim; }
    public int getTotalTarefas()          { return totalTarefas; }
    public int getTarefasConcluidas()     { return tarefasConcluidas; }
    public int getTarefasEmAndamento()    { return tarefasEmAndamento; }
    public int getTarefasPendentes()      { return tarefasPendentes; }
    public int getTarefasCanceladas()     { return tarefasCanceladas; }
    public int getTarefasVencidas()       { return tarefasVencidas; }

    /** Percentual de conclusão (0–100) ignorando tarefas canceladas. */
    public int getPercentualConclusao() {
        int base = totalTarefas - tarefasCanceladas;
        if (base == 0) return 0;
        return (int) Math.round((tarefasConcluidas * 100.0) / base);
    }

    /** true se o projeto encerrou ou vai encerrar após a data prevista (exceto cancelados). */
    public boolean isAtrasado() {
        if (dataPrevisao == null || status == StatusProjeto.CANCELADO) return false;
        LocalDate referencia = (dataFim != null) ? dataFim : LocalDate.now();
        return referencia.isAfter(dataPrevisao);
    }
}

package com.vbaggio.projectapp.dto;

import com.vbaggio.projectapp.model.enums.StatusProjeto;

import java.time.LocalDate;

public record ResumoProjeto(
        String nome,
        StatusProjeto status,
        LocalDate dataPrevisao,
        LocalDate dataFim,
        int totalTarefas,
        int tarefasConcluidas,
        int tarefasEmAndamento,
        int tarefasPendentes,
        int tarefasCanceladas,
        int tarefasVencidas
) {
    /** Percentual de conclusão (0–100) ignorando tarefas canceladas. */
    public int percentualConclusao() {
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

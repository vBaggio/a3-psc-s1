package com.vbaggio.projectapp.dto;

public record CargaUsuario(
        String nome,
        String perfil,
        int tarefasPendentes,
        int tarefasEmAndamento,
        int tarefasConcluidas,
        int tarefasCanceladas,
        int tarefasVencidas
) {
    public int getTotalAtivas() {
        return tarefasPendentes + tarefasEmAndamento;
    }
}

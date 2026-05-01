package com.vbaggio.projectapp.dto;

public class CargaUsuario {

    private final String nome;
    private final String perfil;
    private final int tarefasPendentes;
    private final int tarefasEmAndamento;
    private final int tarefasConcluidas;
    private final int tarefasVencidas;

    public CargaUsuario(String nome, String perfil,
                        int tarefasPendentes, int tarefasEmAndamento,
                        int tarefasConcluidas, int tarefasVencidas) {
        this.nome               = nome;
        this.perfil             = perfil;
        this.tarefasPendentes   = tarefasPendentes;
        this.tarefasEmAndamento = tarefasEmAndamento;
        this.tarefasConcluidas  = tarefasConcluidas;
        this.tarefasVencidas    = tarefasVencidas;
    }

    public String getNome()             { return nome; }
    public String getPerfil()           { return perfil; }
    public int getTarefasPendentes()    { return tarefasPendentes; }
    public int getTarefasEmAndamento()  { return tarefasEmAndamento; }
    public int getTarefasConcluidas()   { return tarefasConcluidas; }
    public int getTarefasVencidas()     { return tarefasVencidas; }

    public int getTotalAtivas() {
        return tarefasPendentes + tarefasEmAndamento;
    }
}

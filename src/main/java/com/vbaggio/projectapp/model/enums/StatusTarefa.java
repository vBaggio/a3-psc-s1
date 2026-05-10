package com.vbaggio.projectapp.model.enums;

public enum StatusTarefa {
    PENDENTE,
    EM_ANDAMENTO,
    CONCLUIDA,
    CANCELADA;

    @Override
    public String toString() {
        return switch (this) {
            case PENDENTE     -> "Pendente";
            case EM_ANDAMENTO -> "Em Andamento";
            case CONCLUIDA    -> "Concluída";
            case CANCELADA    -> "Cancelada";
        };
    }
}

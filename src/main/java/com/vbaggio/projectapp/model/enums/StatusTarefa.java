package com.vbaggio.projectapp.model.enums;

public enum StatusTarefa {
    PENDENTE,
    EM_ANDAMENTO,
    CONCLUIDA,
    CANCELADA;

    public StatusTarefa[] proximosStatus() {
        return switch (this) {
            case PENDENTE     -> new StatusTarefa[]{EM_ANDAMENTO, CANCELADA};
            case EM_ANDAMENTO -> new StatusTarefa[]{CONCLUIDA, CANCELADA};
            case CONCLUIDA, CANCELADA -> new StatusTarefa[]{};
        };
    }

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

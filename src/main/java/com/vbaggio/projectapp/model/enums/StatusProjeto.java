package com.vbaggio.projectapp.model.enums;

public enum StatusProjeto {
    PLANEJADO,
    EM_ANDAMENTO,
    CONCLUIDO,
    CANCELADO;

    public StatusProjeto[] proximosStatus() {
        return switch (this) {
            case PLANEJADO    -> new StatusProjeto[]{EM_ANDAMENTO, CANCELADO};
            case EM_ANDAMENTO -> new StatusProjeto[]{CONCLUIDO, CANCELADO};
            case CONCLUIDO, CANCELADO -> new StatusProjeto[]{};
        };
    }

    @Override
    public String toString() {
        return switch (this) {
            case PLANEJADO    -> "Planejado";
            case EM_ANDAMENTO -> "Em Andamento";
            case CONCLUIDO    -> "Concluído";
            case CANCELADO    -> "Cancelado";
        };
    }
}

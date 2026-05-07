package com.vbaggio.projectapp.model.enums;

public enum Perfil {
    ADMINISTRADOR,
    GERENTE,
    COLABORADOR;

    @Override
    public String toString() {
        return switch (this) {
            case ADMINISTRADOR -> "Administrador";
            case GERENTE       -> "Gerente";
            case COLABORADOR   -> "Colaborador";
        };
    }
}

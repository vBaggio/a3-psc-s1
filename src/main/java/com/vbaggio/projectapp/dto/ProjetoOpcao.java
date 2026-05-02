package com.vbaggio.projectapp.dto;

import java.util.UUID;

public record ProjetoOpcao(UUID id, String nome) {
    @Override
    public String toString() { return nome; }
}

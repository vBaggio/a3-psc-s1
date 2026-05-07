package com.vbaggio.projectapp.view;

import com.vbaggio.projectapp.model.enums.StatusTarefa;
import java.time.LocalDate;
import java.util.UUID;

record DadosTarefa(String nome, String descricao, LocalDate prazo,
                   UUID responsavelId, StatusTarefa status) {}

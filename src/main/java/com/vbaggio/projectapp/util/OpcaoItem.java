package com.vbaggio.projectapp.util;

import java.util.UUID;

public record OpcaoItem(UUID id, String label) {
    @Override
    public String toString() { return label; }
}

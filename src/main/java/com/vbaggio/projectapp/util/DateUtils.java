package com.vbaggio.projectapp.util;

import javax.swing.*;
import javax.swing.text.MaskFormatter;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateUtils {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static LocalDate parse(String texto) {
        if (texto == null) return null;
        String digits = texto.replace("_", "").replace("/", "").trim();
        if (digits.isEmpty()) return null;
        try {
            return LocalDate.parse(texto.trim(), FMT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data inválida '" + texto + "'. Use dd/MM/yyyy.");
        }
    }

    public static String format(LocalDate data) {
        return data == null ? "" : data.format(FMT);
    }

    public static JFormattedTextField campData() {
        try {
            MaskFormatter mask = new MaskFormatter("##/##/####");
            mask.setPlaceholderCharacter('_');
            mask.setValidCharacters("0123456789");
            JFormattedTextField f = new JFormattedTextField(mask);
            f.setColumns(10);
            return f;
        } catch (ParseException e) {
            throw new AssertionError("máscara fixa nunca lança ParseException", e);
        }
    }
}

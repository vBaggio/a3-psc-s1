package com.vbaggio.projectapp.view;

import com.formdev.flatlaf.FlatDarkLaf;
import com.vbaggio.projectapp.model.entity.Usuario;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class MainFrame extends JFrame {

    private static final String CARD_LOGIN = "login";
    private static final String CARD_HOME  = "home";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel     cardPanel  = new JPanel(cardLayout);
    private final Map<String, JFrame> janelasAbertas = new HashMap<>();

    public MainFrame() {
        super("Sistema de Gerenciamento de Projetos e Equipes");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(960, 600);
        setMinimumSize(new Dimension(720, 500));
        setLocationRelativeTo(null);

        cardPanel.add(new LoginPanel(this), CARD_LOGIN);
        add(cardPanel);
    }

    public void mostrarHome(Usuario usuario) {
        cardPanel.add(new HomePanel(this, usuario), CARD_HOME);
        cardLayout.show(cardPanel, CARD_HOME);
        revalidate();
    }

    public void mostrarLogin() {
        new HashMap<>(janelasAbertas).forEach((k, v) -> v.dispose());
        janelasAbertas.clear();
        setJMenuBar(null);
        cardPanel.removeAll();
        cardPanel.add(new LoginPanel(this), CARD_LOGIN);
        cardLayout.show(cardPanel, CARD_LOGIN);
        revalidate();
        repaint();
    }

    public void abrirTela(String chave, String titulo, Supplier<JPanel> fabrica, Dimension tamanho) {
        JFrame janela = janelasAbertas.get(chave);
        if (janela != null && janela.isDisplayable()) {
            janela.toFront();
            janela.requestFocus();
            return;
        }
        janela = new JFrame(titulo);
        janela.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        janela.setSize(tamanho);
        janela.setMinimumSize(new Dimension(400, 320));
        janela.setLocationRelativeTo(this);
        janela.add(fabrica.get());
        janela.setVisible(true);

        final JFrame ref = janela;
        janela.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { janelasAbertas.remove(chave); }
        });
        janelasAbertas.put(chave, janela);
    }

    public static void iniciar() {
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}

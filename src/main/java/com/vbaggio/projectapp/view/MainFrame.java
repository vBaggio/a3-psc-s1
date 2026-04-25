package com.vbaggio.projectapp.view;

import com.formdev.flatlaf.FlatDarkLaf;
import com.vbaggio.projectapp.model.entity.Usuario;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private static final String CARD_LOGIN = "login";
    private static final String CARD_APP   = "app";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel     cardPanel  = new JPanel(cardLayout);
    private DashboardPanel   dashboard;

    public MainFrame() {
        super("Sistema de Gerenciamento de Projetos e Equipes");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 680);
        setLocationRelativeTo(null);

        LoginPanel loginPanel = new LoginPanel(this);
        cardPanel.add(loginPanel, CARD_LOGIN);

        add(cardPanel);
    }

    public void mostrarDashboard(Usuario usuarioLogado) {
        if (dashboard == null) {
            dashboard = new DashboardPanel(this, usuarioLogado);
            cardPanel.add(dashboard, CARD_APP);
        }
        cardLayout.show(cardPanel, CARD_APP);
    }

    public void mostrarLogin() {
        dashboard = null;
        cardPanel.removeAll();
        LoginPanel loginPanel = new LoginPanel(this);
        cardPanel.add(loginPanel, CARD_LOGIN);
        cardLayout.show(cardPanel, CARD_LOGIN);
        revalidate();
        repaint();
    }

    public static void iniciar() {
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}

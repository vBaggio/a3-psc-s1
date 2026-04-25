package com.vbaggio.projectapp.view;

import com.vbaggio.projectapp.model.entity.Usuario;

import javax.swing.*;
import java.awt.*;

public class DashboardPanel extends JPanel {

    private final MainFrame   frame;
    private final Usuario     usuarioLogado;
    private final JTabbedPane abas = new JTabbedPane();

    public DashboardPanel(MainFrame frame, Usuario usuarioLogado) {
        this.frame         = frame;
        this.usuarioLogado = usuarioLogado;
        setLayout(new BorderLayout());

        add(criarHeader(), BorderLayout.NORTH);
        add(abas,          BorderLayout.CENTER);

        abas.addTab("Cargos",   new CargoPanel());
        abas.addTab("Usuários", new UsuarioPanel());
        abas.addTab("Projetos", new ProjetoPanel());
        abas.addTab("Equipes",  new EquipePanel());
        abas.addTab("Tarefas",  new TarefaPanel());
    }

    private JPanel criarHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        header.add(new JLabel("Usuário: " + usuarioLogado.getNome()
                + " [" + usuarioLogado.getPerfil() + "]"), BorderLayout.WEST);

        JButton btnSair = new JButton("Sair");
        btnSair.addActionListener(e -> frame.mostrarLogin());
        header.add(btnSair, BorderLayout.EAST);
        return header;
    }
}

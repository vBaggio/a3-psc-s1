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
        setJMenuBar(criarMenuBar(usuario));
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

    private JMenuBar criarMenuBar(Usuario usuario) {
        JMenuBar bar = new JMenuBar();

        JMenu mCadastros = new JMenu("Cadastros");
        item(mCadastros, "Cargos",   () -> abrirTela("cargos",   "Cargos",   CargoPanel::new,   new Dimension(480, 420)));
        item(mCadastros, "Usuários", () -> abrirTela("usuarios", "Usuários", UsuarioPanel::new, new Dimension(820, 520)));
        item(mCadastros, "Equipes",  () -> abrirTela("equipes",  "Equipes",  EquipePanel::new,  new Dimension(780, 520)));
        bar.add(mCadastros);

        JMenu mOperacoes = new JMenu("Operações");
        item(mOperacoes, "Projetos", () -> abrirTela("projetos", "Projetos", ProjetoPanel::new, new Dimension(820, 520)));
        item(mOperacoes, "Tarefas",  () -> abrirTela("tarefas",  "Tarefas",  TarefaPanel::new,  new Dimension(820, 520)));
        bar.add(mOperacoes);

        bar.add(Box.createHorizontalGlue());

        JMenu mUser = new JMenu(usuario.getNome());
        JMenuItem iPerfil = new JMenuItem(usuario.getPerfil().toString());
        iPerfil.setEnabled(false);
        mUser.add(iPerfil);
        mUser.addSeparator();
        JMenuItem iSair = new JMenuItem("Sair");
        iSair.addActionListener(e -> mostrarLogin());
        mUser.add(iSair);
        bar.add(mUser);

        return bar;
    }

    private void item(JMenu menu, String label, Runnable acao) {
        JMenuItem it = new JMenuItem(label);
        it.addActionListener(e -> acao.run());
        menu.add(it);
    }

    public static void iniciar() {
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}

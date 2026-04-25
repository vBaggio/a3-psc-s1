package com.vbaggio.projectapp.view;

import com.vbaggio.projectapp.model.entity.Usuario;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Supplier;

public class HomePanel extends JPanel {

    private final MainFrame frame;
    private final Usuario   usuario;

    private static final Color[] CORES_MODULO = {
        new Color(66,  133, 244),   // Cargos   — azul
        new Color(52,  168,  83),   // Usuários — verde
        new Color(154,  95, 229),   // Equipes  — roxo
        new Color(251, 140,   0),   // Projetos — laranja
        new Color(234,  67,  53),   // Tarefas  — vermelho
    };

    public HomePanel(MainFrame frame, Usuario usuario) {
        this.frame   = frame;
        this.usuario = usuario;
        setLayout(new BorderLayout());
        add(criarCabecalho(),  BorderLayout.NORTH);
        add(criarGrade(),      BorderLayout.CENTER);
        add(criarRodape(),     BorderLayout.SOUTH);
    }

    // ------------------------------------------------------------------
    // Cabeçalho
    // ------------------------------------------------------------------

    private JPanel criarCabecalho() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(44, 0, 28, 0));

        JLabel titulo = new JLabel("Sistema de Gerenciamento de Projetos e Equipes");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 22f));
        titulo.setAlignmentX(CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Bem-vindo, " + usuario.getNome() + "  ·  " + usuario.getPerfil());
        sub.setFont(sub.getFont().deriveFont(13f));
        sub.setForeground(cor("Label.disabledForeground", new Color(128, 128, 128)));
        sub.setAlignmentX(CENTER_ALIGNMENT);

        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(200, 1));
        sep.setAlignmentX(CENTER_ALIGNMENT);

        p.add(titulo);
        p.add(Box.createVerticalStrut(10));
        p.add(sub);
        p.add(Box.createVerticalStrut(20));
        p.add(sep);
        return p;
    }

    // ------------------------------------------------------------------
    // Grade de módulos
    // ------------------------------------------------------------------

    private JPanel criarGrade() {
        JPanel outer = new JPanel(new GridBagLayout()); // centraliza vertical e horizontal

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        JPanel row1 = fileira();
        row1.add(card(0, "C", "Cargos",   "Funções e papéis",        "cargos",   "Cargos",   CargoPanel::new,   new Dimension(480, 420)));
        row1.add(card(1, "U", "Usuários", "Contas e permissões",     "usuarios", "Usuários", UsuarioPanel::new, new Dimension(820, 520)));
        row1.add(card(2, "E", "Equipes",  "Times e alocações",       "equipes",  "Equipes",  EquipePanel::new,  new Dimension(780, 520)));

        JPanel row2 = fileira();
        row2.add(card(3, "P", "Projetos", "Ciclo de vida e status",  "projetos", "Projetos", ProjetoPanel::new, new Dimension(820, 520)));
        row2.add(card(4, "T", "Tarefas",  "Acompanhamento e prazos", "tarefas",  "Tarefas",  TarefaPanel::new,  new Dimension(820, 520)));

        inner.add(row1);
        inner.add(Box.createVerticalStrut(16));
        inner.add(row2);

        outer.add(inner);
        return outer;
    }

    private JPanel fileira() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        p.setOpaque(false);
        return p;
    }

    // ------------------------------------------------------------------
    // Card individual
    // ------------------------------------------------------------------

    private JPanel card(int idx, String letra, String titulo, String descricao,
                        String chave, String tituloJanela,
                        Supplier<JPanel> fabrica, Dimension tamanho) {

        final Color acento = CORES_MODULO[idx];
        final boolean[] hover = {false};

        JPanel card = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color base = cor("Panel.background", new Color(43, 45, 48));
                Color fundo = hover[0] ? misturar(base, Color.WHITE, 0.07) : misturar(base, Color.WHITE, 0.03);
                g2.setColor(fundo);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

                Color borda = hover[0] ? acento.darker() : cor("Separator.foreground", new Color(70, 73, 75));
                g2.setColor(borda);
                g2.setStroke(new BasicStroke(hover[0] ? 1.6f : 1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(186, 156));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Badge circular com a letra do módulo
        JPanel badge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(acento);
                g2.fillOval(0, 0, 46, 46);
                g2.setColor(Color.WHITE);
                g2.setFont(getFont().deriveFont(Font.BOLD, 19f));
                FontMetrics fm = g2.getFontMetrics();
                int x = (46 - fm.stringWidth(letra)) / 2;
                int y = (46 + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(letra, x, y);
                g2.dispose();
            }
        };
        badge.setOpaque(false);
        badge.setPreferredSize(new Dimension(46, 46));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 14f));

        JLabel lblDesc = new JLabel(descricao);
        lblDesc.setFont(lblDesc.getFont().deriveFont(11f));
        lblDesc.setForeground(cor("Label.disabledForeground", new Color(128, 128, 128)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 10, 0);
        card.add(badge, gbc);
        gbc.gridy = 1; gbc.insets = new Insets(0, 0, 4, 0);
        card.add(lblTitulo, gbc);
        gbc.gridy = 2; gbc.insets = new Insets(0, 0, 0, 0);
        card.add(lblDesc, gbc);

        MouseAdapter ma = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover[0] = true;  card.repaint(); }
            @Override public void mouseExited(MouseEvent e)  {
                Point p = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(), card);
                if (!card.contains(p)) { hover[0] = false; card.repaint(); }
            }
            @Override public void mouseClicked(MouseEvent e) { frame.abrirTela(chave, tituloJanela, fabrica, tamanho); }
        };
        card.addMouseListener(ma);
        badge.addMouseListener(ma);
        lblTitulo.addMouseListener(ma);
        lblDesc.addMouseListener(ma);

        return card;
    }

    // ------------------------------------------------------------------
    // Rodapé
    // ------------------------------------------------------------------

    private JPanel criarRodape() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0,
                cor("Separator.foreground", new Color(70, 73, 75))),
            BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));

        JLabel esquerda = new JLabel("●  Conectado  ·  PostgreSQL 16");
        esquerda.setFont(esquerda.getFont().deriveFont(11f));
        esquerda.setForeground(cor("Label.disabledForeground", new Color(128, 128, 128)));

        JLabel direita = new JLabel("v1.0-SNAPSHOT");
        direita.setFont(direita.getFont().deriveFont(11f));
        direita.setForeground(cor("Label.disabledForeground", new Color(128, 128, 128)));

        footer.add(esquerda, BorderLayout.WEST);
        footer.add(direita,  BorderLayout.EAST);
        return footer;
    }

    // ------------------------------------------------------------------
    // Utilitários
    // ------------------------------------------------------------------

    private static Color cor(String key, Color fallback) {
        Color c = UIManager.getColor(key);
        return c != null ? c : fallback;
    }

    private static Color misturar(Color base, Color overlay, double alpha) {
        int r = clamp((int) (base.getRed()   + (overlay.getRed()   - base.getRed())   * alpha));
        int g = clamp((int) (base.getGreen() + (overlay.getGreen() - base.getGreen()) * alpha));
        int b = clamp((int) (base.getBlue()  + (overlay.getBlue()  - base.getBlue())  * alpha));
        return new Color(r, g, b);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}

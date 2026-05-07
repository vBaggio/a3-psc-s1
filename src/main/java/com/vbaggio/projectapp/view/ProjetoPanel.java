package com.vbaggio.projectapp.view;

import com.vbaggio.projectapp.controller.ProjetoController;
import com.vbaggio.projectapp.controller.UsuarioController;
import com.vbaggio.projectapp.model.entity.Projeto;
import com.vbaggio.projectapp.model.entity.Usuario;
import com.vbaggio.projectapp.model.enums.Perfil;
import com.vbaggio.projectapp.util.DateUtils;
import com.vbaggio.projectapp.util.OpcaoItem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.vbaggio.projectapp.view.TableUtils.tabelaComMensagem;

public class ProjetoPanel extends JPanel {

    private final ProjetoController ctrl        = new ProjetoController();
    private final UsuarioController usuarioCtrl = new UsuarioController();

    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"ID", "Nome", "Status", "Início", "Previsão", "Gerente"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabela = tabelaComMensagem(modelo, "Nenhum projeto cadastrado. Clique em 'Novo' para começar.");
    private boolean scrollParaFim = false;
    private final Map<UUID, JFrame> janelasGestao = new HashMap<>();

    public ProjetoPanel() {
        setLayout(new BorderLayout(0, 4));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(criarToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        ocultarColuna(0);
        tabela.setAutoCreateRowSorter(true);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(200);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(100);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(80);
        tabela.getColumnModel().getColumn(4).setPreferredWidth(80);
        tabela.getColumnModel().getColumn(5).setPreferredWidth(140);
        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabela.getSelectedRow() >= 0) abrirGestao();
            }
        });
        tabela.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "excluir");
        tabela.getActionMap().put("excluir", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (tabela.getSelectedRow() >= 0) excluir();
            }
        });
        carregar();
    }

    private JPanel criarToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton btnNovo      = new JButton("Novo");
        JButton btnGerenciar = new JButton("Gerenciar");
        JButton btnExcluir   = new JButton("Excluir");

        btnGerenciar.setEnabled(false);
        btnExcluir.setEnabled(false);

        bar.add(btnNovo); bar.add(btnGerenciar); bar.add(btnExcluir);

        btnNovo.setToolTipText("Criar novo projeto");
        btnGerenciar.setToolTipText("Gerenciar projeto, status e tarefas");
        btnExcluir.setToolTipText("Excluir projeto e suas tarefas");

        btnNovo.addActionListener(e -> abrirFormulario());
        btnGerenciar.addActionListener(e -> abrirGestao());
        btnExcluir.addActionListener(e -> excluir());

        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            boolean sel = tabela.getSelectedRow() >= 0;
            btnGerenciar.setEnabled(sel);
            btnExcluir.setEnabled(sel);
        });

        return bar;
    }

    private void abrirFormulario() {
        List<Usuario> gerentes = usuarioCtrl.listarPorPerfil(Perfil.GERENTE);
        if (gerentes.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Não há usuários com perfil GERENTE cadastrados.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField campNome = new JTextField(24);
        JTextArea campDesc  = new JTextArea(3, 24);
        campDesc.setLineWrap(true); campDesc.setWrapStyleWord(true);
        JFormattedTextField campInicio   = DateUtils.campData();
        campInicio.setText(DateUtils.format(LocalDate.now()));
        JFormattedTextField campPrevisao = DateUtils.campData();
        OpcaoItem[] opcoesGerente = gerentes.stream()
                .map(u -> new OpcaoItem(u.getId(), u.getNome()))
                .toArray(OpcaoItem[]::new);
        JComboBox<OpcaoItem> comboGerente = new JComboBox<>(opcoesGerente);

        JPanel form = montarForm(
                "Nome:", campNome,
                "Descrição:", new JScrollPane(campDesc),
                "Início (dd/MM/yyyy):", campInicio,
                "Previsão (dd/MM/yyyy):", campPrevisao,
                "Gerente:", comboGerente);

        while (true) {
            int op = JOptionPane.showConfirmDialog(this, form, "Novo Projeto",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (op != JOptionPane.OK_OPTION) return;
            try {
                UUID gerenteId = ((OpcaoItem) comboGerente.getSelectedItem()).id();
                ctrl.criarProjeto(campNome.getText().trim(), campDesc.getText().trim(),
                        DateUtils.parse(campInicio.getText()),
                        DateUtils.parse(campPrevisao.getText()),
                        gerenteId);
                scrollParaFim = true;
                carregar();
                return;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void abrirGestao() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) return;
        UUID id     = UUID.fromString(modelo.getValueAt(linha, 0).toString());
        String nome = modelo.getValueAt(linha, 1).toString();

        JFrame janela = janelasGestao.get(id);
        if (janela != null && janela.isDisplayable()) {
            janela.toFront(); janela.requestFocus(); return;
        }

        GestaoProjetoPanel gestaoPanel = new GestaoProjetoPanel(id, this::carregar);
        janela = new JFrame("Gerenciar Projeto — " + nome);
        janela.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        janela.setSize(820, 580);
        janela.setMinimumSize(new Dimension(600, 420));
        janela.setLocationRelativeTo(this);
        janela.add(gestaoPanel);

        final UUID   fId         = id;
        final JFrame janelaFinal = janela;
        janela.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (gestaoPanel.temAlteracoesPendentes()) {
                    int op = JOptionPane.showConfirmDialog(janelaFinal,
                            "Há alterações não salvas. Deseja sair sem salvar?",
                            "Alterações não salvas",
                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (op != JOptionPane.YES_OPTION) return;
                }
                janelaFinal.dispose();
            }
            @Override public void windowClosed(WindowEvent e) {
                janelasGestao.remove(fId);
                carregar();
            }
        });

        janelasGestao.put(id, janela);
        janela.setVisible(true);
    }

    private void excluir() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) return;
        UUID id = UUID.fromString(modelo.getValueAt(linha, 0).toString());
        String nome = modelo.getValueAt(linha, 1).toString();

        JFrame janelaAberta = janelasGestao.get(id);
        if (janelaAberta != null && janelaAberta.isDisplayable()) {
            JOptionPane.showMessageDialog(this,
                    "Feche a janela de gerenciamento deste projeto antes de excluí-lo.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int conf = JOptionPane.showConfirmDialog(this,
                "Excluir o projeto '" + nome + "' e todas as suas tarefas?\nEsta ação não pode ser desfeita.",
                "Confirmar Exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conf != JOptionPane.YES_OPTION) return;
        try {
            ctrl.removerProjeto(id);
            carregar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void carregar() {
        new SwingWorker<List<Projeto>, Void>() {
            @Override protected List<Projeto> doInBackground() { return ctrl.listarProjetos(); }
            @Override protected void done() {
                try {
                    modelo.setRowCount(0);
                    for (Projeto p : get()) {
                        modelo.addRow(new Object[]{
                                p.getId().toString(), p.getNome(), p.getStatus(),
                                DateUtils.format(p.getDataInicio()),
                                DateUtils.format(p.getDataPrevisao()),
                                p.getGerente() != null ? p.getGerente().getNome() : ""
                        });
                    }
                    if (scrollParaFim && modelo.getRowCount() > 0) {
                        int last = modelo.getRowCount() - 1;
                        tabela.setRowSelectionInterval(last, last);
                        tabela.scrollRectToVisible(tabela.getCellRect(last, 0, true));
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ProjetoPanel.this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                } finally {
                    scrollParaFim = false;
                }
            }
        }.execute();
    }

    private static JPanel montarForm(Object... labelsECampos) {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints lbl = new GridBagConstraints();
        lbl.anchor = GridBagConstraints.NORTHWEST;
        lbl.insets = new Insets(5, 0, 2, 8); lbl.gridx = 0;
        GridBagConstraints fld = new GridBagConstraints();
        fld.weightx = 1.0; fld.gridx = 1; fld.insets = new Insets(3, 0, 2, 0);
        for (int i = 0, row = 0; i < labelsECampos.length; i += 2, row++) {
            lbl.gridy = row; fld.gridy = row;
            boolean isArea = labelsECampos[i + 1] instanceof JScrollPane;
            fld.fill    = isArea ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
            fld.weighty = isArea ? 0.5 : 0;
            p.add(new JLabel((String) labelsECampos[i]), lbl);
            p.add((Component) labelsECampos[i + 1], fld);
        }
        return p;
    }

    private void ocultarColuna(int col) {
        tabela.getColumnModel().getColumn(col).setMinWidth(0);
        tabela.getColumnModel().getColumn(col).setMaxWidth(0);
        tabela.getColumnModel().getColumn(col).setWidth(0);
    }
}

package com.vbaggio.projectapp.view;

import com.vbaggio.projectapp.controller.ProjetoController;
import com.vbaggio.projectapp.controller.TarefaController;
import com.vbaggio.projectapp.controller.UsuarioController;
import com.vbaggio.projectapp.model.entity.Projeto;
import com.vbaggio.projectapp.model.entity.Tarefa;
import com.vbaggio.projectapp.model.entity.Usuario;
import com.vbaggio.projectapp.model.enums.Perfil;
import com.vbaggio.projectapp.model.enums.StatusProjeto;
import com.vbaggio.projectapp.model.enums.StatusTarefa;
import com.vbaggio.projectapp.util.DateUtils;
import com.vbaggio.projectapp.util.OpcaoItem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GestaoProjetoPanel extends JPanel {

    private final ProjetoController projetoCtrl = new ProjetoController();
    private final TarefaController  tarefaCtrl  = new TarefaController();
    private final UsuarioController usuarioCtrl = new UsuarioController();

    private final UUID projetoId;
    private final Runnable onSalvar;

    // Project form fields
    private final JTextField               campNome     = new JTextField(28);
    private final JTextArea                campDesc     = new JTextArea(3, 28);
    private final JComboBox<StatusProjeto> comboStatus  = new JComboBox<>();
    private final JComboBox<OpcaoItem>     comboGerente = new JComboBox<>();
    private final JFormattedTextField      campInicio   = DateUtils.campData();
    private final JFormattedTextField      campPrevisao = DateUtils.campData();

    // Task table
    private final DefaultTableModel modeloTarefas = new DefaultTableModel(
            new String[]{"ID", "Nome", "Status", "Prazo", "Responsável"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return c == 2; }
    };
    private final JTable tabelaTarefas = TableUtils.tabelaComMensagem(
            modeloTarefas, "Nenhuma tarefa. Clique em 'Nova Tarefa' para adicionar.");
    private final JLabel lblContagem = new JLabel("Tarefas");

    // Staging — changes not yet persisted
    private final Map<UUID, DadosTarefa> tarefasNovas     = new LinkedHashMap<>();
    private final Map<UUID, DadosTarefa> tarefasEditadas  = new LinkedHashMap<>();
    private final Set<UUID>              tarefasExcluidas  = new LinkedHashSet<>();

    public boolean temAlteracoesPendentes() {
        return !tarefasNovas.isEmpty() || !tarefasEditadas.isEmpty() || !tarefasExcluidas.isEmpty();
    }

    public GestaoProjetoPanel(UUID projetoId, Runnable onSalvar) {
        this.projetoId = projetoId;
        this.onSalvar  = onSalvar;
        setLayout(new BorderLayout(0, 0));
        add(criarBlocoProjet(),  BorderLayout.NORTH);
        add(criarBlocoTarefas(), BorderLayout.CENTER);
        configurarTabela();
        carregarProjeto();
        carregarTarefas();
    }

    // ------------------------------------------------------------------
    // Top block — project form
    // ------------------------------------------------------------------

    private JPanel criarBlocoProjet() {
        campDesc.setLineWrap(true); campDesc.setWrapStyleWord(true);

        JButton btnSalvar = new JButton("Salvar Projeto");
        btnSalvar.addActionListener(e -> salvarProjeto());

        JPanel form = montarForm(
                "Nome:",                  campNome,
                "Descrição:",             new JScrollPane(campDesc),
                "Status:",                comboStatus,
                "Gerente:",               comboGerente,
                "Início (dd/MM/yyyy):",   campInicio,
                "Previsão (dd/MM/yyyy):", campPrevisao);

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rodape.add(btnSalvar);

        JPanel bloco = new JPanel(new BorderLayout(0, 4));
        bloco.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        bloco.add(form,   BorderLayout.CENTER);
        bloco.add(rodape, BorderLayout.SOUTH);
        return bloco;
    }

    // ------------------------------------------------------------------
    // Bottom block — tasks table
    // ------------------------------------------------------------------

    private JPanel criarBlocoTarefas() {
        JPanel bloco = new JPanel(new BorderLayout(0, 4));
        bloco.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0,
                        UIManager.getColor("Separator.foreground")),
                BorderFactory.createEmptyBorder(4, 8, 8, 8)));

        // header label
        lblContagem.setFont(lblContagem.getFont().deriveFont(Font.BOLD, 12f));

        // toolbar buttons
        JButton btnNova    = new JButton("Nova Tarefa");
        JButton btnEditar  = new JButton("Editar");
        JButton btnExcluir = new JButton("Excluir");
        btnEditar.setEnabled(false);
        btnExcluir.setEnabled(false);

        btnNova.setToolTipText("Nova tarefa para este projeto");
        btnEditar.setToolTipText("Editar tarefa selecionada");
        btnExcluir.setToolTipText("Excluir tarefa selecionada");

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        toolbar.add(btnNova); toolbar.add(btnEditar); toolbar.add(btnExcluir);

        btnNova.addActionListener(e -> novaTarefa());
        btnEditar.addActionListener(e -> editarTarefa());
        btnExcluir.addActionListener(e -> excluirTarefa());

        tabelaTarefas.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            boolean sel = tabelaTarefas.getSelectedRow() >= 0;
            btnEditar.setEnabled(sel);
            btnExcluir.setEnabled(sel);
        });

        // double-click opens edit dialog (but NOT on status column which has inline editor)
        tabelaTarefas.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabelaTarefas.getSelectedRow() >= 0
                        && tabelaTarefas.columnAtPoint(e.getPoint()) != 2) editarTarefa();
            }
        });

        // Delete key removes task
        tabelaTarefas.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "excluirTarefa");
        tabelaTarefas.getActionMap().put("excluirTarefa", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (tabelaTarefas.getSelectedRow() >= 0) excluirTarefa();
            }
        });

        // north area: label + toolbar stacked
        JPanel norte = new JPanel(new BorderLayout(0, 2));
        norte.add(lblContagem, BorderLayout.NORTH);
        norte.add(toolbar,     BorderLayout.SOUTH);

        bloco.add(norte,                          BorderLayout.NORTH);
        bloco.add(new JScrollPane(tabelaTarefas), BorderLayout.CENTER);
        return bloco;
    }

    // ------------------------------------------------------------------
    // Table configuration — inline status editor
    // ------------------------------------------------------------------

    private void configurarTabela() {
        // hide ID column
        tabelaTarefas.getColumnModel().getColumn(0).setMinWidth(0);
        tabelaTarefas.getColumnModel().getColumn(0).setMaxWidth(0);
        tabelaTarefas.getColumnModel().getColumn(0).setWidth(0);

        tabelaTarefas.setAutoCreateRowSorter(true);
        tabelaTarefas.getColumnModel().getColumn(1).setPreferredWidth(200);
        tabelaTarefas.getColumnModel().getColumn(2).setPreferredWidth(120);
        tabelaTarefas.getColumnModel().getColumn(3).setPreferredWidth(80);
        tabelaTarefas.getColumnModel().getColumn(4).setPreferredWidth(140);

        // inline status cell editor
        tabelaTarefas.getColumnModel().getColumn(2).setCellEditor(
                new DefaultCellEditor(new JComboBox<StatusTarefa>()) {
            @Override
            public Component getTableCellEditorComponent(JTable table, Object value,
                    boolean isSelected, int row, int col) {
                StatusTarefa atual = (StatusTarefa) value;
                JComboBox<StatusTarefa> combo = new JComboBox<>();
                combo.addItem(atual);
                for (StatusTarefa prox : atual.proximosStatus()) combo.addItem(prox);
                combo.setSelectedItem(atual);
                editorComponent = combo;
                return combo;
            }

            @Override
            public Object getCellEditorValue() {
                return ((JComboBox<?>) editorComponent).getSelectedItem();
            }

            @Override
            public boolean stopCellEditing() {
                StatusTarefa novo = (StatusTarefa) getCellEditorValue();
                int row = tabelaTarefas.getEditingRow();
                if (row >= 0) {
                    UUID id = UUID.fromString((String) modeloTarefas.getValueAt(row, 0));
                    StatusTarefa atual = (StatusTarefa) modeloTarefas.getValueAt(row, 2);
                    if (novo != atual) {
                        super.stopCellEditing();
                        new SwingWorker<Void, Void>() {
                            @Override protected Void doInBackground() throws Exception {
                                tarefaCtrl.atualizarStatus(id, novo);
                                return null;
                            }
                            @Override protected void done() {
                                try {
                                    get();
                                } catch (Exception ex) {
                                    JOptionPane.showMessageDialog(GestaoProjetoPanel.this,
                                            ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                                }
                                carregarTarefas();
                            }
                        }.execute();
                        return true;
                    }
                }
                return super.stopCellEditing();
            }
        });
    }

    // ------------------------------------------------------------------
    // Load / save project
    // ------------------------------------------------------------------

    private void carregarProjeto() {
        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() {
                Projeto p = projetoCtrl.buscarPorId(projetoId);
                List<Usuario> gerentes = usuarioCtrl.listarPorPerfil(Perfil.GERENTE);
                return new Object[]{p, gerentes};
            }
            @Override @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] resultado = get();
                    Projeto p = (Projeto) resultado[0];
                    List<Usuario> gerentes = (List<Usuario>) resultado[1];
                    campNome.setText(p.getNome());
                    campDesc.setText(p.getDescricao() != null ? p.getDescricao() : "");
                    if (p.getDataInicio()   != null) campInicio.setText(DateUtils.format(p.getDataInicio()));
                    if (p.getDataPrevisao() != null) campPrevisao.setText(DateUtils.format(p.getDataPrevisao()));

                    comboStatus.removeAllItems();
                    comboStatus.addItem(p.getStatus());
                    for (StatusProjeto prox : p.getStatus().proximosStatus()) comboStatus.addItem(prox);
                    comboStatus.setSelectedItem(p.getStatus());

                    comboGerente.removeAllItems();
                    for (Usuario u : gerentes)
                        comboGerente.addItem(new OpcaoItem(u.getId(), u.getNome()));
                    if (p.getGerente() != null) {
                        for (int i = 0; i < comboGerente.getItemCount(); i++) {
                            if (comboGerente.getItemAt(i).id().equals(p.getGerente().getId())) {
                                comboGerente.setSelectedIndex(i); break;
                            }
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(GestaoProjetoPanel.this,
                            ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void salvarProjeto() {
        StatusProjeto novoStatus = (StatusProjeto) comboStatus.getSelectedItem();
        OpcaoItem gerenteItem    = (OpcaoItem) comboGerente.getSelectedItem();
        if (gerenteItem == null) {
            JOptionPane.showMessageDialog(this, "Selecione um gerente.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        UUID gerenteId   = gerenteItem.id();
        String nome      = campNome.getText().trim();
        String desc      = campDesc.getText().trim();
        LocalDate inicio  = DateUtils.parse(campInicio.getText());
        LocalDate previsao = DateUtils.parse(campPrevisao.getText());

        // For CONCLUIDO transition: show date dialog on EDT before spawning the worker
        final LocalDate[] dataFimHolder = {null};
        if (novoStatus == StatusProjeto.CONCLUIDO) {
            JFormattedTextField campFim = DateUtils.campData();
            campFim.setText(DateUtils.format(LocalDate.now()));
            while (true) {
                int op = JOptionPane.showConfirmDialog(this, campFim,
                        "Data de Encerramento (dd/MM/yyyy)",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (op != JOptionPane.OK_OPTION) return;
                LocalDate dataFim = DateUtils.parse(campFim.getText());
                if (dataFim == null) {
                    JOptionPane.showMessageDialog(this, "Informe a data de encerramento.",
                            "Erro", JOptionPane.ERROR_MESSAGE);
                    continue;
                }
                dataFimHolder[0] = dataFim;
                break;
            }
        }

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                Projeto atual = projetoCtrl.buscarPorId(projetoId);
                projetoCtrl.atualizarProjeto(projetoId, nome, desc, inicio, previsao, gerenteId);
                if (novoStatus == StatusProjeto.CONCLUIDO && atual.getStatus() != StatusProjeto.CONCLUIDO) {
                    projetoCtrl.encerrarProjeto(projetoId, dataFimHolder[0]);
                } else if (novoStatus != atual.getStatus()) {
                    projetoCtrl.atualizarStatus(projetoId, novoStatus);
                }
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    if (onSalvar != null) onSalvar.run();
                    carregarProjeto();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(GestaoProjetoPanel.this,
                            ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ------------------------------------------------------------------
    // Load tasks
    // ------------------------------------------------------------------

    private void carregarTarefas() {
        new SwingWorker<List<Tarefa>, Void>() {
            @Override protected List<Tarefa> doInBackground() {
                return tarefaCtrl.listarPorProjeto(projetoId);
            }
            @Override protected void done() {
                try {
                    modeloTarefas.setRowCount(0);
                    List<Tarefa> tarefas = get();
                    long concluidas = tarefas.stream()
                            .filter(t -> t.getStatus() == StatusTarefa.CONCLUIDA).count();
                    for (Tarefa t : tarefas) {
                        modeloTarefas.addRow(new Object[]{
                                t.getId().toString(), t.getNome(), t.getStatus(),
                                DateUtils.format(t.getPrazo()),
                                t.getResponsavel() != null ? t.getResponsavel().getNome() : ""
                        });
                    }
                    lblContagem.setText("Tarefas (" + tarefas.size() + " total · "
                            + concluidas + " concluída" + (concluidas != 1 ? "s" : "") + ")");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(GestaoProjetoPanel.this,
                            ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ------------------------------------------------------------------
    // Task CRUD
    // ------------------------------------------------------------------

    private void novaTarefa() {
        JTextField campNome     = new JTextField(24);
        JTextArea  campDesc     = new JTextArea(3, 24);
        campDesc.setLineWrap(true); campDesc.setWrapStyleWord(true);
        JFormattedTextField campPrazo = DateUtils.campData();
        JComboBox<OpcaoItem> comboResp = montarComboResponsavel();

        JPanel form = montarForm(
                "Nome:", campNome,
                "Descrição:", new JScrollPane(campDesc),
                "Prazo (dd/MM/yyyy):", campPrazo,
                "Responsável:", comboResp);

        while (true) {
            int op = JOptionPane.showConfirmDialog(this, form, "Nova Tarefa",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (op != JOptionPane.OK_OPTION) return;
            try {
                tarefaCtrl.criarTarefa(campNome.getText().trim(), campDesc.getText().trim(),
                        DateUtils.parse(campPrazo.getText()),
                        projetoId, ((OpcaoItem) comboResp.getSelectedItem()).id());
                carregarTarefas();
                return;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editarTarefa() {
        int linha = tabelaTarefas.getSelectedRow();
        if (linha < 0) return;
        UUID id = UUID.fromString((String) modeloTarefas.getValueAt(linha, 0));
        Tarefa t = tarefaCtrl.buscarPorId(id).orElseThrow();

        JTextField campNome     = new JTextField(t.getNome(), 24);
        JTextArea  campDesc     = new JTextArea(t.getDescricao() != null ? t.getDescricao() : "", 3, 24);
        campDesc.setLineWrap(true); campDesc.setWrapStyleWord(true);
        JFormattedTextField campPrazo = DateUtils.campData();
        if (t.getPrazo() != null) campPrazo.setText(DateUtils.format(t.getPrazo()));
        JComboBox<OpcaoItem> comboResp = montarComboResponsavel();
        if (t.getResponsavel() != null) {
            for (int i = 0; i < comboResp.getItemCount(); i++) {
                if (comboResp.getItemAt(i).id() != null
                        && comboResp.getItemAt(i).id().equals(t.getResponsavel().getId())) {
                    comboResp.setSelectedIndex(i); break;
                }
            }
        }

        JPanel form = montarForm(
                "Nome:", campNome,
                "Descrição:", new JScrollPane(campDesc),
                "Prazo (dd/MM/yyyy):", campPrazo,
                "Responsável:", comboResp);

        while (true) {
            int op = JOptionPane.showConfirmDialog(this, form, "Editar Tarefa",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (op != JOptionPane.OK_OPTION) return;
            try {
                tarefaCtrl.atualizarTarefa(id, campNome.getText().trim(),
                        campDesc.getText().trim(), DateUtils.parse(campPrazo.getText()));
                tarefaCtrl.reatribuirResponsavel(id, ((OpcaoItem) comboResp.getSelectedItem()).id());
                carregarTarefas();
                return;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void excluirTarefa() {
        int linha = tabelaTarefas.getSelectedRow();
        if (linha < 0) return;
        String nome = modeloTarefas.getValueAt(linha, 1).toString();
        int conf = JOptionPane.showConfirmDialog(this,
                "Excluir a tarefa '" + nome + "'?",
                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conf != JOptionPane.YES_OPTION) return;
        UUID id = UUID.fromString(modeloTarefas.getValueAt(linha, 0).toString());
        try {
            tarefaCtrl.removerTarefa(id);
            carregarTarefas();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private JComboBox<OpcaoItem> montarComboResponsavel() {
        List<Usuario> usuarios = usuarioCtrl.listarUsuarios();
        OpcaoItem[] opcoes = new OpcaoItem[usuarios.size() + 1];
        opcoes[0] = new OpcaoItem(null, "(sem responsável)");
        for (int i = 0; i < usuarios.size(); i++) {
            opcoes[i + 1] = new OpcaoItem(usuarios.get(i).getId(),
                    usuarios.get(i).getNome() + " [" + usuarios.get(i).getLogin() + "]");
        }
        return new JComboBox<>(opcoes);
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
}

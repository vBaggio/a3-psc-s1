package com.vbaggio.projectapp.view;

import com.vbaggio.projectapp.controller.EquipeController;
import com.vbaggio.projectapp.controller.ProjetoController;
import com.vbaggio.projectapp.controller.TarefaController;
import com.vbaggio.projectapp.controller.UsuarioController;
import com.vbaggio.projectapp.model.entity.Equipe;
import com.vbaggio.projectapp.model.entity.Projeto;
import com.vbaggio.projectapp.model.entity.Tarefa;
import com.vbaggio.projectapp.model.entity.Usuario;
import com.vbaggio.projectapp.model.enums.Perfil;
import com.vbaggio.projectapp.model.enums.RoleNoProjeto;
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
    private final EquipeController  equipeCtrl  = new EquipeController();

    private final UUID projetoId;
    private final Usuario usuario;
    private final Runnable onSalvar;
    private RoleNoProjeto roleNoProjeto; // calculado no done() de carregarProjeto

    // Project form fields
    private final JTextField               campNome     = new JTextField(28);
    private final JTextArea                campDesc     = new JTextArea(3, 28);
    private final JComboBox<StatusProjeto> comboStatus  = new JComboBox<>();
    private final JComboBox<OpcaoItem>     comboGerente = new JComboBox<>();
    private final JComboBox<OpcaoItem>     comboEquipe  = new JComboBox<>();
    private final JFormattedTextField      campInicio   = DateUtils.campData();
    private final JFormattedTextField      campPrevisao = DateUtils.campData();

    // Task table
    private final DefaultTableModel modeloTarefas = new DefaultTableModel(
            new String[]{"ID", "Nome", "Status", "Prazo", "Responsável", "RespID", "Desc"}, 0) {
        @Override public boolean isCellEditable(int r, int c) {
            if (c != 2) return false;
            if (roleNoProjeto == null) return false;
            if (roleNoProjeto == RoleNoProjeto.ADMIN
                    || roleNoProjeto == RoleNoProjeto.GERENTE_EFETIVO) return true;
            // COLABORADOR: only own tasks
            String respId = (String) getValueAt(r, 5);
            return usuario != null && usuario.getId().toString().equals(respId);
        }
    };
    private final JTable tabelaTarefas = TableUtils.tabelaComMensagem(
            modeloTarefas, "Nenhuma tarefa. Clique em 'Nova Tarefa' para adicionar.");
    private final JLabel lblContagem = new JLabel("Tarefas");

    // Staging — changes not yet persisted
    private final Map<UUID, DadosTarefa> tarefasNovas     = new LinkedHashMap<>();
    private final Map<UUID, DadosTarefa> tarefasEditadas  = new LinkedHashMap<>();
    private final Set<UUID>              tarefasExcluidas = new LinkedHashSet<>();

    public boolean temAlteracoesPendentes() {
        return !tarefasNovas.isEmpty() || !tarefasEditadas.isEmpty() || !tarefasExcluidas.isEmpty();
    }

    public GestaoProjetoPanel(UUID projetoId, Usuario usuario, Runnable onSalvar) {
        this.projetoId = projetoId;
        this.usuario   = usuario;
        this.onSalvar  = onSalvar;
        setLayout(new BorderLayout(0, 0));
        add(criarBlocoProjet(),  BorderLayout.NORTH);
        add(criarBlocoTarefas(), BorderLayout.CENTER);
        add(criarRodape(),       BorderLayout.SOUTH);
        configurarTabela();
        carregarProjeto();
        carregarTarefas();
    }

    // ------------------------------------------------------------------
    // Top block — project form
    // ------------------------------------------------------------------

    private JPanel criarBlocoProjet() {
        campDesc.setLineWrap(true); campDesc.setWrapStyleWord(true);

        JPanel form = montarForm(
                "Nome:",                  campNome,
                "Descrição:",             new JScrollPane(campDesc),
                "Status:",                comboStatus,
                "Gerente:",               comboGerente,
                "Equipe:",                comboEquipe,
                "Início (dd/MM/yyyy):",   campInicio,
                "Previsão (dd/MM/yyyy):", campPrevisao);

        JPanel bloco = new JPanel(new BorderLayout(0, 4));
        bloco.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        bloco.add(form, BorderLayout.CENTER);
        return bloco;
    }

    // ------------------------------------------------------------------
    // Bottom block — tasks table
    // ------------------------------------------------------------------

    private JPanel criarBlocoTarefas() {
        JPanel bloco = new JPanel(new BorderLayout(0, 4));
        bloco.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0,
                        java.util.Objects.requireNonNullElse(
                                UIManager.getColor("Separator.foreground"), java.awt.Color.GRAY)),
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
            int viewRow = tabelaTarefas.getSelectedRow();
            boolean temSelecao = viewRow >= 0;
            boolean podeEditarQualquer = roleNoProjeto == RoleNoProjeto.ADMIN
                                      || roleNoProjeto == RoleNoProjeto.GERENTE_EFETIVO;
            boolean ehMinhaTarefa = temSelecao && usuario != null && podeEditarTarefa(viewRow);
            btnEditar.setEnabled(temSelecao && (podeEditarQualquer || ehMinhaTarefa));
            btnExcluir.setEnabled(temSelecao && podeEditarQualquer);
        });

        // double-click opens edit dialog (but NOT on status column which has inline editor)
        tabelaTarefas.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int viewRow = tabelaTarefas.getSelectedRow();
                if (e.getClickCount() == 2 && viewRow >= 0
                        && tabelaTarefas.columnAtPoint(e.getPoint()) != 2
                        && podeEditarTarefa(viewRow)) editarTarefa();
            }
        });

        // Delete key removes task
        tabelaTarefas.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "excluirTarefa");
        tabelaTarefas.getActionMap().put("excluirTarefa", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                boolean podeExcluir = roleNoProjeto == RoleNoProjeto.ADMIN
                        || roleNoProjeto == RoleNoProjeto.GERENTE_EFETIVO;
                if (tabelaTarefas.getSelectedRow() >= 0 && podeExcluir) excluirTarefa();
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

    private JPanel criarRodape() {
        JButton btnCancelar = new JButton("Cancelar");
        JButton btnSalvar   = new JButton("Salvar");
        btnCancelar.addActionListener(e -> cancelar());
        btnSalvar.addActionListener(e -> salvarTudo());

        JPanel direita = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        direita.add(btnCancelar);
        direita.add(btnSalvar);

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0,
                        java.util.Objects.requireNonNullElse(
                                UIManager.getColor("Separator.foreground"), java.awt.Color.GRAY)),
                BorderFactory.createEmptyBorder(6, 8, 8, 8)));
        rodape.add(direita, BorderLayout.EAST);
        return rodape;
    }

    // ------------------------------------------------------------------
    // Table configuration — inline status editor
    // ------------------------------------------------------------------

    private void configurarTabela() {
        // hide ID column
        tabelaTarefas.getColumnModel().getColumn(0).setMinWidth(0);
        tabelaTarefas.getColumnModel().getColumn(0).setMaxWidth(0);
        tabelaTarefas.getColumnModel().getColumn(0).setWidth(0);
        tabelaTarefas.getColumnModel().getColumn(5).setMinWidth(0);
        tabelaTarefas.getColumnModel().getColumn(5).setMaxWidth(0);
        tabelaTarefas.getColumnModel().getColumn(5).setWidth(0);
        tabelaTarefas.getColumnModel().getColumn(6).setMinWidth(0);
        tabelaTarefas.getColumnModel().getColumn(6).setMaxWidth(0);
        tabelaTarefas.getColumnModel().getColumn(6).setWidth(0);

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
                JComboBox<StatusTarefa> combo = new JComboBox<>(StatusTarefa.values());
                combo.setSelectedItem(value);
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
                int rowView = tabelaTarefas.getEditingRow();
                if (rowView >= 0) {
                    int row = tabelaTarefas.convertRowIndexToModel(rowView);
                    UUID uuid = UUID.fromString((String) modeloTarefas.getValueAt(row, 0));
                    StatusTarefa atual = (StatusTarefa) modeloTarefas.getValueAt(row, 2);
                    if (novo != atual) {
                        super.stopCellEditing();
                        DadosTarefa base = dadosParaStaging(uuid, row);
                        DadosTarefa atualizado = new DadosTarefa(
                                base.nome(), base.descricao(), base.prazo(),
                                base.responsavelId(), novo);
                        if (tarefasNovas.containsKey(uuid)) {
                            tarefasNovas.put(uuid, atualizado);
                        } else {
                            tarefasEditadas.put(uuid, atualizado);
                        }
                        atualizarContagem();
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
                List<Equipe>  equipes  = equipeCtrl.listarEquipes().stream()
                        .filter(e -> !equipeCtrl.listarMembros(e.getId()).isEmpty())
                        .toList();
                // Load members inside the JPA session to avoid LazyInitializationException
                List<Usuario> membrosEquipe = (p.getEquipe() != null)
                        ? equipeCtrl.listarMembros(p.getEquipe().getId())
                        : List.of();
                return new Object[]{p, gerentes, equipes, membrosEquipe};
            }
            @Override @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] resultado = get();
                    Projeto       p             = (Projeto)       resultado[0];
                    List<Usuario> gerentes      = (List<Usuario>) resultado[1];
                    List<Equipe>  equipes       = (List<Equipe>)  resultado[2];
                    List<Usuario> membrosEquipe = (List<Usuario>) resultado[3];

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

                    comboEquipe.removeAllItems();
                    for (Equipe e : equipes)
                        comboEquipe.addItem(new OpcaoItem(e.getId(), e.getNome()));
                    if (p.getEquipe() != null) {
                        for (int i = 0; i < comboEquipe.getItemCount(); i++) {
                            if (comboEquipe.getItemAt(i).id().equals(p.getEquipe().getId())) {
                                comboEquipe.setSelectedIndex(i); break;
                            }
                        }
                    }

                    roleNoProjeto = calcularRole(p, membrosEquipe);
                    aplicarConstraintesDeRole();
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
                    for (Tarefa t : tarefas) {
                        modeloTarefas.addRow(new Object[]{
                            t.getId().toString(),
                            t.getNome(),
                            t.getStatus(),
                            DateUtils.format(t.getPrazo()),
                            t.getResponsavel() != null ? t.getResponsavel().getNome() : "",
                            t.getResponsavel() != null ? t.getResponsavel().getId().toString() : "",
                            t.getDescricao() != null ? t.getDescricao() : ""
                        });
                    }
                    atualizarContagem();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(GestaoProjetoPanel.this,
                            ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void atualizarContagem() {
        int total = modeloTarefas.getRowCount();
        long concluidas = 0;
        for (int i = 0; i < total; i++) {
            Object v = modeloTarefas.getValueAt(i, 2);
            if (v instanceof StatusTarefa s && s == StatusTarefa.CONCLUIDA) concluidas++;
        }
        lblContagem.setText("Tarefas (" + total + " total · "
                + concluidas + " concluída" + (concluidas != 1 ? "s" : "") + ")");
    }

    private void salvarTudo() {
        if (tabelaTarefas.isEditing()) tabelaTarefas.getCellEditor().stopCellEditing();
        StatusProjeto novoStatus = (StatusProjeto) comboStatus.getSelectedItem();
        OpcaoItem     gerenteItem = (OpcaoItem) comboGerente.getSelectedItem();
        if (gerenteItem == null) {
            JOptionPane.showMessageDialog(this, "Selecione um gerente.", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        OpcaoItem equipeItem = (OpcaoItem) comboEquipe.getSelectedItem();
        if (equipeItem == null) {
            JOptionPane.showMessageDialog(this, "Selecione uma equipe.", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        UUID      equipeId  = equipeItem.id();
        UUID      gerenteId = gerenteItem.id();
        String    nome      = campNome.getText().trim();
        String    desc      = campDesc.getText().trim();
        LocalDate inicio    = DateUtils.parse(campInicio.getText());
        LocalDate previsao  = DateUtils.parse(campPrevisao.getText());

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

        Map<UUID, DadosTarefa> novas    = new LinkedHashMap<>(tarefasNovas);
        Map<UUID, DadosTarefa> editadas = new LinkedHashMap<>(tarefasEditadas);
        Set<UUID>              excluidas = new LinkedHashSet<>(tarefasExcluidas);

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                // Note: These operations run without a DB transaction. A partial failure
                // leaves earlier steps persisted. Retry may create duplicate tasks.
                // Acceptable limitation for this scope — staging is preserved on error.
                // 1. Projeto — somente ADMIN e GERENTE_EFETIVO podem alterar dados do projeto
                boolean podeEditarProjeto = roleNoProjeto == RoleNoProjeto.ADMIN
                        || roleNoProjeto == RoleNoProjeto.GERENTE_EFETIVO;
                if (podeEditarProjeto) {
                    Projeto atual = projetoCtrl.buscarPorId(projetoId);
                    projetoCtrl.atualizarProjeto(projetoId, nome, desc, inicio, previsao, gerenteId, equipeId, usuario);
                    if (novoStatus == StatusProjeto.CONCLUIDO
                            && atual.getStatus() != StatusProjeto.CONCLUIDO) {
                        projetoCtrl.encerrarProjeto(projetoId, dataFimHolder[0], usuario);
                    } else if (novoStatus != atual.getStatus()) {
                        projetoCtrl.atualizarStatus(projetoId, novoStatus, usuario);
                    }
                }
                // 2. Novas tarefas
                for (DadosTarefa d : novas.values()) {
                    Tarefa t = tarefaCtrl.criarTarefa(d.nome(), d.descricao(),
                            d.prazo(), projetoId, d.responsavelId(), usuario);
                    if (d.status() != StatusTarefa.PENDENTE) {
                        tarefaCtrl.atualizarStatus(t.getId(), d.status());
                    }
                }
                // 3. Tarefas editadas
                for (Map.Entry<UUID, DadosTarefa> e : editadas.entrySet()) {
                    UUID id = e.getKey(); DadosTarefa d = e.getValue();
                    tarefaCtrl.atualizarTarefa(id, d.nome(), d.descricao(), d.prazo());
                    tarefaCtrl.reatribuirResponsavel(id, d.responsavelId(), usuario);
                    Tarefa t = tarefaCtrl.buscarPorId(id).orElseThrow();
                    if (t.getStatus() != d.status()) {
                        tarefaCtrl.atualizarStatus(id, d.status());
                    }
                }
                // 4. Tarefas excluídas
                for (UUID id : excluidas) {
                    tarefaCtrl.removerTarefa(id);
                }
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    tarefasNovas.clear();
                    tarefasEditadas.clear();
                    tarefasExcluidas.clear();
                    if (onSalvar != null) onSalvar.run();
                    carregarProjeto();
                    carregarTarefas();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(GestaoProjetoPanel.this,
                            ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void cancelar() {
        tarefasNovas.clear();
        tarefasEditadas.clear();
        tarefasExcluidas.clear();
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w != null) w.dispose();
    }

    private DadosTarefa dadosParaStaging(UUID uuid, int row) {
        if (tarefasNovas.containsKey(uuid))    return tarefasNovas.get(uuid);
        if (tarefasEditadas.containsKey(uuid)) return tarefasEditadas.get(uuid);
        Object respIdObj = modeloTarefas.getValueAt(row, 5);
        UUID   respId    = (respIdObj == null || respIdObj.toString().isEmpty())
                           ? null : UUID.fromString(respIdObj.toString());
        Object descObj   = modeloTarefas.getValueAt(row, 6);
        String desc      = descObj != null ? descObj.toString() : null;
        return new DadosTarefa(
                modeloTarefas.getValueAt(row, 1).toString(),
                desc,
                DateUtils.parse(modeloTarefas.getValueAt(row, 3).toString()),
                respId,
                (StatusTarefa) modeloTarefas.getValueAt(row, 2));
    }

    // ------------------------------------------------------------------
    // Task CRUD
    // ------------------------------------------------------------------

    private void novaTarefa() {
        JTextField          campNomeTarefa = new JTextField(24);
        JTextArea           campDescTarefa = new JTextArea(3, 24);
        campDescTarefa.setLineWrap(true); campDescTarefa.setWrapStyleWord(true);
        JFormattedTextField campPrazo      = DateUtils.campData();
        JComboBox<OpcaoItem>    comboResp  = montarComboResponsavel();
        JComboBox<StatusTarefa> comboSt = new JComboBox<>(StatusTarefa.values());
        comboSt.setSelectedItem(StatusTarefa.PENDENTE);

        JPanel form = montarForm(
                "Nome:",              campNomeTarefa,
                "Descrição:",         new JScrollPane(campDescTarefa),
                "Prazo (dd/MM/yyyy):", campPrazo,
                "Responsável:",       comboResp,
                "Status:",            comboSt);

        while (true) {
            int op = JOptionPane.showConfirmDialog(this, form, "Nova Tarefa",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (op != JOptionPane.OK_OPTION) return;
            String nome = campNomeTarefa.getText().trim();
            if (nome.isBlank()) {
                JOptionPane.showMessageDialog(this, "Nome da tarefa é obrigatório.",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                continue;
            }
            UUID      tempId = UUID.randomUUID();
            OpcaoItem resp   = (OpcaoItem) comboResp.getSelectedItem();
            DadosTarefa dados = new DadosTarefa(
                    nome,
                    campDescTarefa.getText().trim(),
                    DateUtils.parse(campPrazo.getText()),
                    resp != null ? resp.id() : null,
                    (StatusTarefa) comboSt.getSelectedItem());
            tarefasNovas.put(tempId, dados);
            modeloTarefas.addRow(new Object[]{
                    tempId.toString(),
                    dados.nome(),
                    dados.status(),
                    DateUtils.format(dados.prazo()),
                    resp != null && resp.id() != null ? resp.label() : "",
                    resp != null && resp.id() != null ? resp.id().toString() : "",
                    dados.descricao() != null ? dados.descricao() : ""
            });
            atualizarContagem();
            return;
        }
    }

    private void editarTarefa() {
        int linhaView = tabelaTarefas.getSelectedRow();
        if (linhaView < 0) return;
        int linha = tabelaTarefas.convertRowIndexToModel(linhaView);
        UUID uuid = UUID.fromString((String) modeloTarefas.getValueAt(linha, 0));

        DadosTarefa base = dadosParaStaging(uuid, linha);

        JTextField          campNomeTarefa = new JTextField(base.nome(), 24);
        JTextArea           campDescTarefa = new JTextArea(
                base.descricao() != null ? base.descricao() : "", 3, 24);
        campDescTarefa.setLineWrap(true); campDescTarefa.setWrapStyleWord(true);
        JFormattedTextField campPrazo = DateUtils.campData();
        if (base.prazo() != null) campPrazo.setText(DateUtils.format(base.prazo()));

        JComboBox<OpcaoItem> comboResp = montarComboResponsavel();
        if (base.responsavelId() != null) {
            for (int i = 0; i < comboResp.getItemCount(); i++) {
                if (comboResp.getItemAt(i).id() != null
                        && comboResp.getItemAt(i).id().equals(base.responsavelId())) {
                    comboResp.setSelectedIndex(i); break;
                }
            }
        }

        JComboBox<StatusTarefa> comboSt = new JComboBox<>(StatusTarefa.values());
        comboSt.setSelectedItem(base.status());

        JPanel form = montarForm(
                "Nome:",              campNomeTarefa,
                "Descrição:",         new JScrollPane(campDescTarefa),
                "Prazo (dd/MM/yyyy):", campPrazo,
                "Responsável:",       comboResp,
                "Status:",            comboSt);

        while (true) {
            int op = JOptionPane.showConfirmDialog(this, form, "Editar Tarefa",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (op != JOptionPane.OK_OPTION) return;
            String nome = campNomeTarefa.getText().trim();
            if (nome.isBlank()) {
                JOptionPane.showMessageDialog(this, "Nome da tarefa é obrigatório.",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                continue;
            }
            OpcaoItem resp = (OpcaoItem) comboResp.getSelectedItem();
            DadosTarefa dados = new DadosTarefa(
                    nome,
                    campDescTarefa.getText().trim(),
                    DateUtils.parse(campPrazo.getText()),
                    resp != null ? resp.id() : null,
                    (StatusTarefa) comboSt.getSelectedItem());

            if (tarefasNovas.containsKey(uuid)) {
                tarefasNovas.put(uuid, dados);
            } else {
                tarefasEditadas.put(uuid, dados);
            }

            modeloTarefas.setValueAt(dados.nome(),   linha, 1);
            modeloTarefas.setValueAt(dados.status(), linha, 2);
            modeloTarefas.setValueAt(DateUtils.format(dados.prazo()), linha, 3);
            modeloTarefas.setValueAt(resp != null && resp.id() != null ? resp.label() : "", linha, 4);
            modeloTarefas.setValueAt(resp != null && resp.id() != null ? resp.id().toString() : "", linha, 5);
            modeloTarefas.setValueAt(dados.descricao() != null ? dados.descricao() : "", linha, 6);
            atualizarContagem();
            return;
        }
    }

    private void excluirTarefa() {
        int linhaView = tabelaTarefas.getSelectedRow();
        if (linhaView < 0) return;
        int linha = tabelaTarefas.convertRowIndexToModel(linhaView);
        UUID uuid = UUID.fromString(modeloTarefas.getValueAt(linha, 0).toString());
        String nome = modeloTarefas.getValueAt(linha, 1).toString();

        int conf = JOptionPane.showConfirmDialog(this,
                "Excluir a tarefa '" + nome + "'?",
                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conf != JOptionPane.YES_OPTION) return;

        if (tarefasNovas.containsKey(uuid)) {
            tarefasNovas.remove(uuid);
        } else {
            tarefasEditadas.remove(uuid);
            tarefasExcluidas.add(uuid);
        }

        modeloTarefas.removeRow(linha);
        atualizarContagem();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private boolean podeEditarTarefa(int viewRow) {
        if (roleNoProjeto == RoleNoProjeto.ADMIN || roleNoProjeto == RoleNoProjeto.GERENTE_EFETIVO)
            return true;
        int modelRow = tabelaTarefas.convertRowIndexToModel(viewRow);
        String respId = (String) modeloTarefas.getValueAt(modelRow, 5);
        return usuario != null && usuario.getId().toString().equals(respId);
    }

    private RoleNoProjeto calcularRole(Projeto projeto, List<Usuario> membrosEquipe) {
        if (usuario.getPerfil() == Perfil.ADMINISTRADOR) return RoleNoProjeto.ADMIN;
        boolean isGerente = projeto.getGerente() != null
            && projeto.getGerente().getId().equals(usuario.getId());
        if (isGerente) return RoleNoProjeto.GERENTE_EFETIVO;
        boolean isMembro = membrosEquipe.stream()
                .anyMatch(m -> m.getId().equals(usuario.getId()));
        return isMembro ? RoleNoProjeto.COLABORADOR : RoleNoProjeto.SEM_ACESSO;
    }

    private void aplicarConstraintesDeRole() {
        boolean podeEditar = roleNoProjeto == RoleNoProjeto.ADMIN
                          || roleNoProjeto == RoleNoProjeto.GERENTE_EFETIVO;
        campNome.setEnabled(podeEditar);
        campDesc.setEnabled(podeEditar);
        campInicio.setEnabled(podeEditar);
        campPrevisao.setEnabled(podeEditar);
        comboStatus.setEnabled(podeEditar);
        comboEquipe.setEnabled(roleNoProjeto == RoleNoProjeto.ADMIN);
        comboGerente.setEnabled(roleNoProjeto == RoleNoProjeto.ADMIN);
    }

    private JComboBox<OpcaoItem> montarComboResponsavel() {
        boolean podeAtribuirOutros = roleNoProjeto == RoleNoProjeto.ADMIN
                                  || roleNoProjeto == RoleNoProjeto.GERENTE_EFETIVO;
        if (!podeAtribuirOutros) {
            JComboBox<OpcaoItem> combo = new JComboBox<>();
            combo.addItem(new OpcaoItem(usuario.getId(),
                    usuario.getNome() + " [" + usuario.getLogin() + "]"));
            combo.setEnabled(false);
            return combo;
        }
        OpcaoItem equipeItem = (OpcaoItem) comboEquipe.getSelectedItem();
        List<Usuario> usuarios;
        if (equipeItem != null && equipeItem.id() != null) {
            usuarios = equipeCtrl.listarMembros(equipeItem.id());
        } else {
            usuarios = usuarioCtrl.listarUsuarios();
        }
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

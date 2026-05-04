package com.vbaggio.projectapp.view;

import com.vbaggio.projectapp.controller.ProjetoController;
import com.vbaggio.projectapp.controller.TarefaController;
import com.vbaggio.projectapp.controller.UsuarioController;
import com.vbaggio.projectapp.model.entity.Projeto;
import com.vbaggio.projectapp.model.entity.Tarefa;
import com.vbaggio.projectapp.model.entity.Usuario;
import com.vbaggio.projectapp.model.enums.StatusTarefa;
import com.vbaggio.projectapp.util.DateUtils;
import com.vbaggio.projectapp.util.OpcaoItem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.UUID;

public class TarefaPanel extends JPanel {

    private final TarefaController  ctrl        = new TarefaController();
    private final ProjetoController projetoCtrl = new ProjetoController();
    private final UsuarioController usuarioCtrl = new UsuarioController();

    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"ID", "Nome", "Status", "Prazo", "Responsável"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabela = new JTable(modelo);

    private final JComboBox<OpcaoItem> comboProjeto = new JComboBox<>();

    public TarefaPanel() {
        setLayout(new BorderLayout(0, 4));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(criarFiltro(),   BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        add(criarToolbar(),  BorderLayout.SOUTH);
        ocultarColuna(0);
        carregarComboProjeto();
        comboProjeto.addActionListener(e -> carregarTarefas());
    }

    private JPanel criarFiltro() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton btnRefresh = new JButton("↻");
        bar.add(new JLabel("Projeto:")); bar.add(comboProjeto); bar.add(btnRefresh);
        btnRefresh.addActionListener(e -> { carregarComboProjeto(); carregarTarefas(); });
        return bar;
    }

    private JPanel criarToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton btnNova    = new JButton("Nova Tarefa");
        JButton btnEditar  = new JButton("Editar");
        JButton btnStatus  = new JButton("Alterar Status");
        JButton btnRespons = new JButton("Reatribuir");
        JButton btnExcluir = new JButton("Excluir");
        btnEditar.setEnabled(false);
        bar.add(btnNova); bar.add(btnEditar); bar.add(btnStatus); bar.add(btnRespons); bar.add(btnExcluir);

        btnNova.addActionListener(e -> abrirFormulario());
        btnEditar.addActionListener(e -> abrirEdicao());
        btnStatus.addActionListener(e -> alterarStatus());
        btnRespons.addActionListener(e -> reatribuir());
        btnExcluir.addActionListener(e -> excluir());

        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = tabela.getSelectedRow();
            if (row < 0) { btnEditar.setEnabled(false); return; }
            Object s = modelo.getValueAt(row, 2);
            btnEditar.setEnabled(s != StatusTarefa.CONCLUIDA && s != StatusTarefa.CANCELADA);
        });

        return bar;
    }

    private void abrirFormulario() {
        OpcaoItem projetoSel = (OpcaoItem) comboProjeto.getSelectedItem();
        if (projetoSel == null) {
            JOptionPane.showMessageDialog(this, "Selecione um projeto primeiro.");
            return;
        }
        UUID projetoId = projetoSel.id();

        JTextField campNome = new JTextField(24);
        JTextArea  campDesc = new JTextArea(3, 24);
        campDesc.setLineWrap(true); campDesc.setWrapStyleWord(true);
        JFormattedTextField campPrazo = DateUtils.campData();

        List<Usuario> usuarios = usuarioCtrl.listarUsuarios();
        OpcaoItem[] opcoesResp = new OpcaoItem[usuarios.size() + 1];
        opcoesResp[0] = new OpcaoItem(null, "(sem responsável)");
        for (int i = 0; i < usuarios.size(); i++) {
            opcoesResp[i + 1] = new OpcaoItem(
                    usuarios.get(i).getId(),
                    usuarios.get(i).getNome() + " [" + usuarios.get(i).getLogin() + "]");
        }
        JComboBox<OpcaoItem> comboResp = new JComboBox<>(opcoesResp);

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
                ctrl.criarTarefa(campNome.getText().trim(), campDesc.getText().trim(),
                        DateUtils.parse(campPrazo.getText()),
                        projetoId, ((OpcaoItem) comboResp.getSelectedItem()).id());
                carregarTarefas();
                return;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void abrirEdicao() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) { JOptionPane.showMessageDialog(this, "Selecione uma tarefa."); return; }
        UUID id = UUID.fromString((String) modelo.getValueAt(linha, 0));
        Tarefa t = ctrl.buscarPorId(id)
                .orElseThrow(() -> new IllegalStateException("Tarefa não encontrada."));

        JTextField campNome = new JTextField(t.getNome(), 24);
        JTextArea  campDesc = new JTextArea(t.getDescricao() != null ? t.getDescricao() : "", 3, 24);
        campDesc.setLineWrap(true); campDesc.setWrapStyleWord(true);
        JFormattedTextField campPrazo = DateUtils.campData();
        if (t.getPrazo() != null) campPrazo.setText(DateUtils.format(t.getPrazo()));

        JPanel form = montarForm(
                "Nome:", campNome,
                "Descrição:", new JScrollPane(campDesc),
                "Prazo (dd/MM/yyyy):", campPrazo);

        while (true) {
            int op = JOptionPane.showConfirmDialog(this, form, "Editar Tarefa",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (op != JOptionPane.OK_OPTION) return;
            try {
                ctrl.atualizarTarefa(id, campNome.getText().trim(), campDesc.getText().trim(),
                        DateUtils.parse(campPrazo.getText()));
                carregarTarefas();
                return;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void alterarStatus() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) { JOptionPane.showMessageDialog(this, "Selecione uma tarefa."); return; }
        UUID id = UUID.fromString((String) modelo.getValueAt(linha, 0));
        StatusTarefa[] opcoes = StatusTarefa.values();
        StatusTarefa escolha = (StatusTarefa) JOptionPane.showInputDialog(
                this, "Novo status:", "Alterar Status",
                JOptionPane.PLAIN_MESSAGE, null, opcoes, opcoes[0]);
        if (escolha == null) return;
        try {
            ctrl.atualizarStatus(id, escolha);
            carregarTarefas();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void reatribuir() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) { JOptionPane.showMessageDialog(this, "Selecione uma tarefa."); return; }
        UUID id = UUID.fromString((String) modelo.getValueAt(linha, 0));

        List<Usuario> usuarios = usuarioCtrl.listarUsuarios();
        OpcaoItem[] opcoesResp = new OpcaoItem[usuarios.size() + 1];
        opcoesResp[0] = new OpcaoItem(null, "(remover responsável)");
        for (int i = 0; i < usuarios.size(); i++) {
            opcoesResp[i + 1] = new OpcaoItem(
                    usuarios.get(i).getId(),
                    usuarios.get(i).getNome() + " [" + usuarios.get(i).getLogin() + "]");
        }
        OpcaoItem escolha = (OpcaoItem) JOptionPane.showInputDialog(
                this, "Responsável:", "Reatribuir",
                JOptionPane.PLAIN_MESSAGE, null, opcoesResp, opcoesResp[0]);
        if (escolha == null) return;
        try {
            ctrl.reatribuirResponsavel(id, escolha.id());
            carregarTarefas();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void excluir() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) { JOptionPane.showMessageDialog(this, "Selecione uma tarefa."); return; }
        String nome = modelo.getValueAt(linha, 1).toString();
        int conf = JOptionPane.showConfirmDialog(this, "Excluir tarefa '" + nome + "'?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;
        UUID id = UUID.fromString(modelo.getValueAt(linha, 0).toString());
        try {
            ctrl.removerTarefa(id);
            carregarTarefas();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void carregarComboProjeto() {
        OpcaoItem selAnterior = (OpcaoItem) comboProjeto.getSelectedItem();
        UUID selId = selAnterior != null ? selAnterior.id() : null;
        comboProjeto.removeAllItems();
        for (Projeto p : projetoCtrl.listarProjetos()) {
            comboProjeto.addItem(new OpcaoItem(p.getId(), p.getNome() + " [" + p.getStatus() + "]"));
        }
        if (selId != null) {
            for (int i = 0; i < comboProjeto.getItemCount(); i++) {
                if (comboProjeto.getItemAt(i).id().equals(selId)) {
                    comboProjeto.setSelectedIndex(i); break;
                }
            }
        }
    }

    private void carregarTarefas() {
        OpcaoItem projetoSel = (OpcaoItem) comboProjeto.getSelectedItem();
        if (projetoSel == null) return;
        UUID projetoId = projetoSel.id();
        modelo.setRowCount(0);
        for (Tarefa t : ctrl.listarPorProjeto(projetoId)) {
            modelo.addRow(new Object[]{
                    t.getId().toString(), t.getNome(), t.getStatus(),
                    DateUtils.format(t.getPrazo()),
                    t.getResponsavel() != null ? t.getResponsavel().getNome() : ""
            });
        }
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

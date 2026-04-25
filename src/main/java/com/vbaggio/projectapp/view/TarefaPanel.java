package com.vbaggio.projectapp.view;

import com.vbaggio.projectapp.controller.ProjetoController;
import com.vbaggio.projectapp.controller.TarefaController;
import com.vbaggio.projectapp.controller.UsuarioController;
import com.vbaggio.projectapp.model.entity.Projeto;
import com.vbaggio.projectapp.model.entity.Tarefa;
import com.vbaggio.projectapp.model.entity.Usuario;
import com.vbaggio.projectapp.model.enums.StatusTarefa;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class TarefaPanel extends JPanel {

    private final TarefaController  ctrl        = new TarefaController();
    private final ProjetoController projetoCtrl = new ProjetoController();
    private final UsuarioController usuarioCtrl = new UsuarioController();

    private final JComboBox<String> comboProjeto = new JComboBox<>();
    private List<Projeto>           listaProjetos;

    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"ID", "Nome", "Status", "Prazo", "Responsável"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabela = new JTable(modelo);

    public TarefaPanel() {
        setLayout(new BorderLayout(0, 4));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(criarFiltro(),           BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        add(criarToolbar(),          BorderLayout.SOUTH);

        ocultarColuna(0);

        carregarComboProjeto();
        comboProjeto.addActionListener(e -> carregarTarefas());
    }

    private JPanel criarFiltro() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        p.add(new JLabel("Projeto:"));
        p.add(comboProjeto);
        JButton btnAtualizar = new JButton("↻");
        btnAtualizar.addActionListener(e -> { carregarComboProjeto(); carregarTarefas(); });
        p.add(btnAtualizar);
        return p;
    }

    private JPanel criarToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton btnNova    = new JButton("Nova Tarefa");
        JButton btnStatus  = new JButton("Alterar Status");
        JButton btnRespons = new JButton("Reatribuir");
        JButton btnExcluir = new JButton("Excluir");
        bar.add(btnNova); bar.add(btnStatus); bar.add(btnRespons); bar.add(btnExcluir);

        btnNova.addActionListener(e -> abrirFormulario());
        btnStatus.addActionListener(e -> alterarStatus());
        btnRespons.addActionListener(e -> reatribuir());
        btnExcluir.addActionListener(e -> excluir());
        return bar;
    }

    private void abrirFormulario() {
        if (comboProjeto.getSelectedIndex() < 0 || listaProjetos == null || listaProjetos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecione um projeto primeiro.");
            return;
        }
        UUID projetoId = listaProjetos.get(comboProjeto.getSelectedIndex()).getId();

        JTextField campNome  = new JTextField(24);
        JTextArea  campDesc  = new JTextArea(3, 24);
        JTextField campPrazo = new JTextField("yyyy-MM-dd", 10);

        List<Usuario> usuarios = usuarioCtrl.listarUsuarios();
        String[] nomesUsuarios = new String[usuarios.size() + 1];
        UUID[]   idsUsuarios   = new UUID[usuarios.size() + 1];
        nomesUsuarios[0] = "(sem responsável)"; idsUsuarios[0] = null;
        for (int i = 0; i < usuarios.size(); i++) {
            nomesUsuarios[i + 1] = usuarios.get(i).getNome() + " [" + usuarios.get(i).getLogin() + "]";
            idsUsuarios[i + 1]   = usuarios.get(i).getId();
        }
        JComboBox<String> comboResp = new JComboBox<>(nomesUsuarios);

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 4));
        form.add(new JLabel("Nome:"));               form.add(campNome);
        form.add(new JLabel("Descrição:"));          form.add(new JScrollPane(campDesc));
        form.add(new JLabel("Prazo (yyyy-MM-dd):")); form.add(campPrazo);
        form.add(new JLabel("Responsável:"));        form.add(comboResp);

        int op = JOptionPane.showConfirmDialog(this, form, "Nova Tarefa",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return;

        try {
            LocalDate prazo = parsarData(campPrazo.getText().trim());
            UUID respId = idsUsuarios[comboResp.getSelectedIndex()];
            ctrl.criarTarefa(campNome.getText().trim(), campDesc.getText().trim(),
                    prazo, projetoId, respId);
            carregarTarefas();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void alterarStatus() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) { JOptionPane.showMessageDialog(this, "Selecione uma tarefa."); return; }
        UUID id = UUID.fromString(modelo.getValueAt(linha, 0).toString());

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
        UUID id = UUID.fromString(modelo.getValueAt(linha, 0).toString());

        List<Usuario> usuarios = usuarioCtrl.listarUsuarios();
        String[] nomes = new String[usuarios.size() + 1];
        UUID[]   ids   = new UUID[usuarios.size() + 1];
        nomes[0] = "(remover responsável)"; ids[0] = null;
        for (int i = 0; i < usuarios.size(); i++) {
            nomes[i + 1] = usuarios.get(i).getNome() + " [" + usuarios.get(i).getLogin() + "]";
            ids[i + 1]   = usuarios.get(i).getId();
        }
        String escolha = (String) JOptionPane.showInputDialog(this,
                "Novo responsável:", "Reatribuir",
                JOptionPane.PLAIN_MESSAGE, null, nomes, nomes[0]);
        if (escolha == null) return;
        int idx = Arrays.asList(nomes).indexOf(escolha);

        try {
            ctrl.reatribuirResponsavel(id, ids[idx]);
            carregarTarefas();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void excluir() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) { JOptionPane.showMessageDialog(this, "Selecione uma tarefa."); return; }
        int conf = JOptionPane.showConfirmDialog(this, "Confirmar exclusão?",
                "Excluir", JOptionPane.YES_NO_OPTION);
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
        int selAnterior = comboProjeto.getSelectedIndex();
        comboProjeto.removeAllItems();
        listaProjetos = projetoCtrl.listarProjetos();
        for (Projeto p : listaProjetos) {
            comboProjeto.addItem(p.getNome() + " [" + p.getStatus() + "]");
        }
        if (selAnterior >= 0 && selAnterior < comboProjeto.getItemCount()) {
            comboProjeto.setSelectedIndex(selAnterior);
        }
    }

    private void carregarTarefas() {
        modelo.setRowCount(0);
        if (comboProjeto.getSelectedIndex() < 0 || listaProjetos == null || listaProjetos.isEmpty()) return;
        UUID projetoId = listaProjetos.get(comboProjeto.getSelectedIndex()).getId();
        for (Tarefa t : ctrl.listarPorProjeto(projetoId)) {
            modelo.addRow(new Object[]{
                    t.getId().toString(),
                    t.getNome(),
                    t.getStatus(),
                    t.getPrazo()       != null ? t.getPrazo().toString()                : "",
                    t.getResponsavel() != null ? t.getResponsavel().getNome()           : ""
            });
        }
    }

    private LocalDate parsarData(String texto) {
        if (texto == null || texto.isBlank() || texto.equals("yyyy-MM-dd")) return null;
        try { return LocalDate.parse(texto); }
        catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Data inválida '" + texto + "'. Use o formato yyyy-MM-dd.");
        }
    }

    private void ocultarColuna(int col) {
        tabela.getColumnModel().getColumn(col).setMinWidth(0);
        tabela.getColumnModel().getColumn(col).setMaxWidth(0);
        tabela.getColumnModel().getColumn(col).setWidth(0);
    }
}

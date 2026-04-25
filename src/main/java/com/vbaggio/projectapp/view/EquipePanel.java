package com.vbaggio.projectapp.view;

import com.vbaggio.projectapp.controller.EquipeController;
import com.vbaggio.projectapp.controller.UsuarioController;
import com.vbaggio.projectapp.model.entity.Equipe;
import com.vbaggio.projectapp.model.entity.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class EquipePanel extends JPanel {

    private final EquipeController  ctrl        = new EquipeController();
    private final UsuarioController usuarioCtrl = new UsuarioController();

    private final DefaultTableModel modeloEquipes = new DefaultTableModel(
            new String[]{"ID", "Nome", "Descrição"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabelaEquipes = new JTable(modeloEquipes);

    private final DefaultTableModel modeloMembros = new DefaultTableModel(
            new String[]{"ID", "Nome", "Login"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabelaMembros = new JTable(modeloMembros);

    public EquipePanel() {
        setLayout(new BorderLayout(0, 4));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        ocultarColuna(tabelaEquipes, 0);
        ocultarColuna(tabelaMembros, 0);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                criarPainelEquipes(), criarPainelMembros());
        split.setResizeWeight(0.5);
        add(split, BorderLayout.CENTER);

        tabelaEquipes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) carregarMembros();
        });

        carregarEquipes();
    }

    private JPanel criarPainelEquipes() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBorder(BorderFactory.createTitledBorder("Equipes"));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton btnNova    = new JButton("Nova");
        JButton btnEditar  = new JButton("Editar");
        JButton btnExcluir = new JButton("Excluir");
        bar.add(btnNova); bar.add(btnEditar); bar.add(btnExcluir);

        btnNova.addActionListener(e -> abrirFormularioEquipe(null));
        btnEditar.addActionListener(e -> {
            int linha = tabelaEquipes.getSelectedRow();
            if (linha < 0) { JOptionPane.showMessageDialog(this, "Selecione uma equipe."); return; }
            UUID id = UUID.fromString(modeloEquipes.getValueAt(linha, 0).toString());
            abrirFormularioEquipe(ctrl.buscarPorId(id));
        });
        btnExcluir.addActionListener(e -> excluirEquipe());

        p.add(bar, BorderLayout.NORTH);
        p.add(new JScrollPane(tabelaEquipes), BorderLayout.CENTER);
        return p;
    }

    private JPanel criarPainelMembros() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBorder(BorderFactory.createTitledBorder("Membros da Equipe Selecionada"));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton btnAdicionar = new JButton("Adicionar");
        JButton btnRemover   = new JButton("Remover");
        bar.add(btnAdicionar); bar.add(btnRemover);

        btnAdicionar.addActionListener(e -> adicionarMembro());
        btnRemover.addActionListener(e -> removerMembro());

        p.add(bar, BorderLayout.NORTH);
        p.add(new JScrollPane(tabelaMembros), BorderLayout.CENTER);
        return p;
    }

    private void abrirFormularioEquipe(Equipe equipe) {
        boolean edicao = equipe != null;
        JTextField campNome = new JTextField(edicao ? equipe.getNome() : "", 20);
        JTextArea  campDesc = new JTextArea(
                edicao && equipe.getDescricao() != null ? equipe.getDescricao() : "", 3, 20);

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 4));
        form.add(new JLabel("Nome:"));      form.add(campNome);
        form.add(new JLabel("Descrição:")); form.add(new JScrollPane(campDesc));

        int op = JOptionPane.showConfirmDialog(this, form,
                edicao ? "Editar Equipe" : "Nova Equipe",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return;

        try {
            if (!edicao) {
                ctrl.criarEquipe(campNome.getText().trim(), campDesc.getText().trim());
            } else {
                ctrl.atualizarEquipe(equipe.getId(), campNome.getText().trim(), campDesc.getText().trim());
            }
            carregarEquipes();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void excluirEquipe() {
        int linha = tabelaEquipes.getSelectedRow();
        if (linha < 0) { JOptionPane.showMessageDialog(this, "Selecione uma equipe."); return; }
        int conf = JOptionPane.showConfirmDialog(this, "Confirmar exclusão?",
                "Excluir", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;
        UUID id = UUID.fromString(modeloEquipes.getValueAt(linha, 0).toString());
        try {
            ctrl.removerEquipe(id);
            carregarEquipes();
            modeloMembros.setRowCount(0);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void adicionarMembro() {
        int linha = tabelaEquipes.getSelectedRow();
        if (linha < 0) { JOptionPane.showMessageDialog(this, "Selecione uma equipe."); return; }
        UUID equipeId = UUID.fromString(modeloEquipes.getValueAt(linha, 0).toString());

        List<Usuario> todos    = usuarioCtrl.listarUsuarios();
        List<Usuario> membros  = ctrl.listarMembros(equipeId);
        List<Usuario> naoMembros = todos.stream()
                .filter(u -> membros.stream().noneMatch(m -> m.getId().equals(u.getId())))
                .toList();

        if (naoMembros.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Todos os usuários já são membros desta equipe.");
            return;
        }

        String[] nomes = naoMembros.stream()
                .map(u -> u.getNome() + " [" + u.getLogin() + "]")
                .toArray(String[]::new);
        String escolha = (String) JOptionPane.showInputDialog(this,
                "Selecionar usuário:", "Adicionar Membro",
                JOptionPane.PLAIN_MESSAGE, null, nomes, nomes[0]);
        if (escolha == null) return;

        int idx = Arrays.asList(nomes).indexOf(escolha);
        try {
            ctrl.adicionarMembro(equipeId, naoMembros.get(idx).getId());
            carregarMembros();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removerMembro() {
        int linhaEquipe = tabelaEquipes.getSelectedRow();
        int linhaMembro = tabelaMembros.getSelectedRow();
        if (linhaEquipe < 0 || linhaMembro < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma equipe e um membro.");
            return;
        }
        UUID equipeId  = UUID.fromString(modeloEquipes.getValueAt(linhaEquipe, 0).toString());
        UUID usuarioId = UUID.fromString(modeloMembros.getValueAt(linhaMembro, 0).toString());
        try {
            ctrl.removerMembro(equipeId, usuarioId);
            carregarMembros();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void carregarEquipes() {
        modeloEquipes.setRowCount(0);
        for (Equipe e : ctrl.listarEquipes()) {
            modeloEquipes.addRow(new Object[]{
                    e.getId().toString(), e.getNome(),
                    e.getDescricao() != null ? e.getDescricao() : ""
            });
        }
        modeloMembros.setRowCount(0);
    }

    private void carregarMembros() {
        modeloMembros.setRowCount(0);
        int linha = tabelaEquipes.getSelectedRow();
        if (linha < 0) return;
        UUID equipeId = UUID.fromString(modeloEquipes.getValueAt(linha, 0).toString());
        for (Usuario u : ctrl.listarMembros(equipeId)) {
            modeloMembros.addRow(new Object[]{u.getId().toString(), u.getNome(), u.getLogin()});
        }
    }

    private void ocultarColuna(JTable tabela, int col) {
        tabela.getColumnModel().getColumn(col).setMinWidth(0);
        tabela.getColumnModel().getColumn(col).setMaxWidth(0);
        tabela.getColumnModel().getColumn(col).setWidth(0);
    }
}

package com.vbaggio.projectapp.view;

import com.vbaggio.projectapp.controller.CargoController;
import com.vbaggio.projectapp.model.entity.Cargo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.UUID;

public class CargoPanel extends JPanel {

    private final CargoController   ctrl  = new CargoController();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"ID", "Nome"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabela = new JTable(modelo);

    public CargoPanel() {
        setLayout(new BorderLayout(0, 4));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(criarToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        ocultarColuna(0);
        carregar();
    }

    private JPanel criarToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton btnNovo    = new JButton("Novo");
        JButton btnEditar  = new JButton("Editar");
        JButton btnExcluir = new JButton("Excluir");
        bar.add(btnNovo); bar.add(btnEditar); bar.add(btnExcluir);

        btnNovo.addActionListener(e -> abrirFormulario(null));
        btnEditar.addActionListener(e -> {
            int linha = tabela.getSelectedRow();
            if (linha < 0) { JOptionPane.showMessageDialog(this, "Selecione um cargo."); return; }
            UUID id = UUID.fromString(modelo.getValueAt(linha, 0).toString());
            abrirFormulario(ctrl.buscarPorId(id));
        });
        btnExcluir.addActionListener(e -> excluir());
        return bar;
    }

    private void abrirFormulario(Cargo cargo) {
        JTextField campNome = new JTextField(cargo != null ? cargo.getNome() : "", 24);
        JPanel form = new JPanel(new GridLayout(1, 2, 6, 0));
        form.add(new JLabel("Nome:")); form.add(campNome);

        int op = JOptionPane.showConfirmDialog(this, form,
                cargo == null ? "Novo Cargo" : "Editar Cargo",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return;

        try {
            if (cargo == null) {
                ctrl.cadastrarCargo(campNome.getText().trim());
            } else {
                ctrl.atualizarNome(cargo.getId(), campNome.getText().trim());
            }
            carregar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void excluir() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) { JOptionPane.showMessageDialog(this, "Selecione um cargo."); return; }
        int conf = JOptionPane.showConfirmDialog(this, "Confirmar exclusão?",
                "Excluir", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;
        UUID id = UUID.fromString(modelo.getValueAt(linha, 0).toString());
        try {
            ctrl.removerCargo(id);
            carregar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void carregar() {
        modelo.setRowCount(0);
        List<Cargo> cargos = ctrl.listarCargos();
        for (Cargo c : cargos) {
            modelo.addRow(new Object[]{c.getId().toString(), c.getNome()});
        }
    }

    private void ocultarColuna(int col) {
        tabela.getColumnModel().getColumn(col).setMinWidth(0);
        tabela.getColumnModel().getColumn(col).setMaxWidth(0);
        tabela.getColumnModel().getColumn(col).setWidth(0);
    }
}

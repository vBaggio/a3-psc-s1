package com.vbaggio.projectapp.view;

import com.vbaggio.projectapp.controller.CargoController;
import com.vbaggio.projectapp.model.entity.Cargo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
        tabela.setAutoCreateRowSorter(true);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(320);
        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabela.getSelectedRow() >= 0) {
                    int linha = tabela.getSelectedRow();
                    UUID id = UUID.fromString(modelo.getValueAt(linha, 0).toString());
                    abrirFormulario(ctrl.buscarPorId(id));
                }
            }
        });
        carregar();
    }

    private JPanel criarToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton btnNovo    = new JButton("Novo");
        JButton btnEditar  = new JButton("Editar");
        JButton btnExcluir = new JButton("Excluir");
        bar.add(btnNovo); bar.add(btnEditar); bar.add(btnExcluir);

        btnEditar.setEnabled(false);
        btnExcluir.setEnabled(false);

        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            boolean sel = tabela.getSelectedRow() >= 0;
            btnEditar.setEnabled(sel);
            btnExcluir.setEnabled(sel);
        });

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
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 4));
        form.add(new JLabel("Nome:")); form.add(campNome);

        String titulo = cargo == null ? "Novo Cargo" : "Editar Cargo";
        while (true) {
            int op = JOptionPane.showConfirmDialog(this, form, titulo,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (op != JOptionPane.OK_OPTION) return;
            try {
                if (cargo == null) {
                    ctrl.cadastrarCargo(campNome.getText().trim());
                } else {
                    ctrl.atualizarNome(cargo.getId(), campNome.getText().trim());
                }
                carregar();
                return;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void excluir() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) { JOptionPane.showMessageDialog(this, "Selecione um cargo."); return; }
        String nome = modelo.getValueAt(linha, 1).toString();
        int conf = JOptionPane.showConfirmDialog(this,
                "Excluir cargo '" + nome + "'?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
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
        new SwingWorker<List<Cargo>, Void>() {
            @Override protected List<Cargo> doInBackground() { return ctrl.listarCargos(); }
            @Override protected void done() {
                try {
                    modelo.setRowCount(0);
                    for (Cargo c : get()) modelo.addRow(new Object[]{c.getId().toString(), c.getNome()});
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CargoPanel.this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void ocultarColuna(int col) {
        tabela.getColumnModel().getColumn(col).setMinWidth(0);
        tabela.getColumnModel().getColumn(col).setMaxWidth(0);
        tabela.getColumnModel().getColumn(col).setWidth(0);
    }
}

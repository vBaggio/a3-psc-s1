package com.vbaggio.projectapp.view;

import com.vbaggio.projectapp.controller.CargoController;
import com.vbaggio.projectapp.model.entity.Cargo;
import com.vbaggio.projectapp.model.entity.Usuario;
import com.vbaggio.projectapp.model.enums.Perfil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.UUID;

import static com.vbaggio.projectapp.view.TableUtils.tabelaComMensagem;


public class CargoPanel extends JPanel {

    private final Usuario usuario;
    private final CargoController   ctrl  = new CargoController();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"ID", "Nome"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabela = tabelaComMensagem(modelo, "Nenhum cargo cadastrado.");
    private boolean scrollParaFim = false;

    private JButton btnNovo;
    private JButton btnEditar;
    private JButton btnExcluir;

    public CargoPanel(Usuario usuario) {
        this.usuario = usuario;
        setLayout(new BorderLayout(0, 4));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(criarToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        if (usuario.getPerfil() != Perfil.ADMINISTRADOR) {
            // GERENTE é somente leitura neste painel
            btnNovo.setEnabled(false);
            btnEditar.setEnabled(false);
            btnExcluir.setEnabled(false);
        }

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
        btnNovo    = new JButton("Novo");
        btnEditar  = new JButton("Editar");
        btnExcluir = new JButton("Excluir");
        bar.add(btnNovo); bar.add(btnEditar); bar.add(btnExcluir);

        btnEditar.setEnabled(false);
        btnExcluir.setEnabled(false);

        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            boolean sel = tabela.getSelectedRow() >= 0;
            btnEditar.setEnabled(sel);
            btnExcluir.setEnabled(sel);
        });

        btnNovo.setToolTipText("Criar novo cargo");
        btnEditar.setToolTipText("Editar cargo selecionado");
        btnExcluir.setToolTipText("Excluir cargo selecionado");

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
                    scrollParaFim = true;
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
                    if (scrollParaFim && modelo.getRowCount() > 0) {
                        int last = modelo.getRowCount() - 1;
                        tabela.setRowSelectionInterval(last, last);
                        tabela.scrollRectToVisible(tabela.getCellRect(last, 0, true));
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CargoPanel.this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                } finally {
                    scrollParaFim = false;
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
